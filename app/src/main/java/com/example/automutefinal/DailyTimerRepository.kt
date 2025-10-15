package com.example.automutefinal

import android.content.Context
import com.google.gson.Gson

/**
 * A simple repository object for storing and retrieving individual DailyTimer objects.
 * This allows you to persist timers using SharedPreferences with JSON serialization.
 */
object DailyTimerRepository {

    // Name of the SharedPreferences file
    private const val PREFS_NAME = "daily_timers"

    /**
     * Saves a single DailyTimer in SharedPreferences.
     */
    fun saveTimer(context: Context, timer: DailyTimer) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Convert the timer object to JSON and store it with the timer ID as the key
        prefs.edit().putString(timer.id, Gson().toJson(timer)).apply()
    }

    /**
     * Retrieves a DailyTimer from SharedPreferences by its ID.
     */
    fun getTimerById(context: Context, timerId: String): DailyTimer? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(timerId, null) ?: return null
        // Convert the JSON string back to a DailyTimer object
        return Gson().fromJson(json, DailyTimer::class.java)
    }
}
