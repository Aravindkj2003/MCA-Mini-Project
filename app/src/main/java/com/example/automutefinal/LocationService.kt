package com.example.automutefinal

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var audioManager: AudioManager
    private val CHANNEL_ID = "AutoMuteLocationServiceChannel"
    private val gson = Gson()

    /**
     * Initializes service, notification channel, foreground notification,
     * location client, audio manager, and location callback.
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }

    /**
     * Starts a foreground notification to keep service alive.
     */
    private fun startForegroundServiceNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoMute is Active")
            .setContentText("Monitoring your location and timers...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    /**
     * Handles location updates.
     * Checks if quick timer or daily timer is active; otherwise,
     * adjusts ringer mode based on proximity to saved locations.
     */
    private fun handleLocationUpdate(location: Location) {
        val appSettings = getSharedPreferences("AppSettings", MODE_PRIVATE)

        if (appSettings.getBoolean("isQuickTimerActive", false)) {
            Log.d("LocationService", "Quick Timer active, skipping location checks")
            return
        }

        if (isDailyTimerActive()) {
            Log.d("LocationService", "Daily Timer active, skipping location-based mute")
            return
        }

        val savedLocations = loadSavedLocations()
        if (savedLocations.isEmpty()) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            return
        }

        var insideZone = false
        for (savedLocation in savedLocations) {
            val distance = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                savedLocation.latitude, savedLocation.longitude,
                distance
            )
            if (distance[0] < savedLocation.radius) {
                audioManager.ringerMode = savedLocation.ringerMode
                insideZone = true
                break
            }
        }

        if (!insideZone) {
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
    }

    /**
     * Checks if any daily timer is currently active based on the day and time.
     */
    private fun isDailyTimerActive(): Boolean {
        val dailyTimers = loadDailyTimers()
        if (dailyTimers.isEmpty()) return false

        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (timer in dailyTimers) {
            if (timer.daysOfWeek.contains(currentDay)) {
                val startTime = timer.startHour * 60 + timer.startMinute
                val endTime = timer.endHour * 60 + timer.endMinute
                if (currentTimeInMinutes in startTime until endTime) return true
            }
        }
        return false
    }

    /**
     * Loads all saved daily timers from shared preferences.
     */
    private fun loadDailyTimers(): List<DailyTimer> {
        val sharedPreferences = getSharedPreferences("DailyTimerPrefs", MODE_PRIVATE)
        val json = sharedPreferences.getString("daily_timers", null)
        return if (json != null) {
            val type = object : TypeToken<List<DailyTimer>>() {}.type
            gson.fromJson(json, type)
        } else emptyList()
    }

    /**
     * Loads all saved locations from shared preferences.
     */
    private fun loadSavedLocations(): List<SavedLocation> {
        val sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE)
        val json = sharedPreferences.getString("locations", null)
        return if (json != null) {
            val type = object : TypeToken<List<SavedLocation>>() {}.type
            gson.fromJson(json, type)
        } else emptyList()
    }

    /**
     * Creates a notification channel for Android O+ to show foreground notification.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AutoMute Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Starts location updates when service is started.
     * Stops service if location permission is not granted.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            15000 // 15 seconds
        ).setMinUpdateIntervalMillis(10000).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        return START_STICKY
    }

    /**
     * Restarts service if it is killed by the system.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartServiceIntent = Intent(applicationContext, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(restartServiceIntent)
        } else {
            applicationContext.startService(restartServiceIntent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Cleans up by removing location updates when service is destroyed.
     */
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
