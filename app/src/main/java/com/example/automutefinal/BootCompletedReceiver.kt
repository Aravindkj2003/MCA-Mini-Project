package com.example.automutefinal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Check if the system has finished booting
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {

            // Restart location monitoring service if there are saved locations
            val savedLocations = loadSavedLocations(context)
            if (savedLocations.isNotEmpty()) {
                val serviceIntent = Intent(context, LocationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }

            // Reschedule all daily timers after reboot
            val timers = loadDailyTimers(context)
            if (timers.isNotEmpty()) {
                val scheduler = TimerScheduler(context)
                timers.forEach { scheduler.schedule(it) }
            }
        }
    }

    // Loads saved locations from SharedPreferences
    private fun loadSavedLocations(context: Context): List<SavedLocation> {
        val sharedPreferences = context.getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("locations", null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<SavedLocation>>() {}.type
            gson.fromJson(json, type)
        } else emptyList()
    }

    // Loads daily timers from SharedPreferences
    private fun loadDailyTimers(context: Context): List<DailyTimer> {
        val sharedPreferences = context.getSharedPreferences("DailyTimerPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("daily_timers", null)
        return if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<DailyTimer>>() {}.type
            gson.fromJson(json, type)
        } else emptyList()
    }
}
