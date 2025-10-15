package com.example.automutefinal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SetLocationActivity : AppCompatActivity() {

    private lateinit var saveLocationButton: Button
    private lateinit var locationNameEditText: EditText
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val REQUEST_CODE_LOCATION_PERMISSION = 101
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_location)

        // Initialize UI components and location client
        saveLocationButton = findViewById(R.id.saveLocationButton)
        locationNameEditText = findViewById(R.id.locationNameEditText)
        latitudeTextView = findViewById(R.id.latText)
        longitudeTextView = findViewById(R.id.longText)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check if location permissions are granted; request if not
        if (!hasLocationPermission()) {
            requestLocationPermission()
        } else {
            // Fetch current location and start background location monitoring
            fetchCurrentLocation()
            startLocationServiceIfNeeded()
        }

        // Handle save button click to save location
        saveLocationButton.setOnClickListener {
            checkPermissionsAndSave()
        }
    }

    /**
     * Checks if both fine and background location permissions are granted.
     * Returns true if all required permissions are available.
     */
    private fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return fineLocation && backgroundLocation
    }

    /**
     * Requests fine and background location permissions from the user if not granted.
     */
    private fun requestLocationPermission() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                REQUEST_CODE_LOCATION_PERMISSION
            )
        }
    }

    /**
     * Handles the result of the permission request.
     * If granted, fetches the current location and starts the location monitoring service.
     * If denied, shows a toast message.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                fetchCurrentLocation()
                startLocationServiceIfNeeded()
            } else {
                Toast.makeText(this, "Location permissions are required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Fetches the device's current location.
     * If last known location is not available, requests a high-accuracy single location update.
     */
    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                updateLocationUI(location)
            } else {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                    .setMaxUpdates(1)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { updateLocationUI(it) }
                    }
                }

                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            }
        }
    }

    /**
     * Updates the latitude and longitude text fields in the UI with the given location values.
     */
    private fun updateLocationUI(location: Location) {
        latitudeTextView.text = String.format("%.6f", location.latitude)
        longitudeTextView.text = String.format("%.6f", location.longitude)
    }

    /**
     * Checks if location permissions are granted; if yes, saves the location, otherwise requests permissions.
     */
    private fun checkPermissionsAndSave() {
        if (hasLocationPermission()) {
            saveCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    /**
     * Saves the current location with user-provided name, sets ringer to silent, and starts monitoring.
     */
    private fun saveCurrentLocation() {
        val name = locationNameEditText.text.toString()
        val latText = latitudeTextView.text.toString()
        val lonText = longitudeTextView.text.toString()

        if (name.isEmpty() || latText.isEmpty() || lonText.isEmpty() || latText == "0.000000") {
            Toast.makeText(this, "Please wait for location and enter a name.", Toast.LENGTH_SHORT).show()
            return
        }

        val ringerMode = AudioManager.RINGER_MODE_SILENT
        val radius = 150f
        val newLocation = SavedLocation(name, latText.toDouble(), lonText.toDouble(), ringerMode, radius)
        addLocationToSavedList(newLocation)

        startLocationServiceIfNeeded()
        Toast.makeText(this, "Location saved and monitoring started!", Toast.LENGTH_LONG).show()
        finish()
    }

    /**
     * Adds the newly saved location to shared preferences for persistent storage.
     */
    private fun addLocationToSavedList(newLocation: SavedLocation) {
        val sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = sharedPreferences.getString("locations", null)
        val type = object : TypeToken<MutableList<SavedLocation>>() {}.type

        val locations: MutableList<SavedLocation> = if (json != null) {
            gson.fromJson(json, type)
        } else mutableListOf()

        locations.add(newLocation)
        editor.putString("locations", gson.toJson(locations)).apply()
    }

    /**
     * Starts the location monitoring service if any saved locations exist.
     * Uses foreground service for Android O and above.
     */
    private fun startLocationServiceIfNeeded() {
        val sharedPreferences = getSharedPreferences("LocationPrefs", MODE_PRIVATE)
        val json = sharedPreferences.getString("locations", null)
        if (!json.isNullOrEmpty()) {
            val serviceIntent = Intent(this, LocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }
}
