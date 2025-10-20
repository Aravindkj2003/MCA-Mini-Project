package com.example.automutefinal

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.media.AudioManager

/**
 * Displays the list of daily timers.
 * Allows users to add new timers, delete existing timers,
 * and schedules alarms for each timer using TimerScheduler.
 */
class DailyTimerListActivity : AppCompatActivity() {

    private lateinit var timerContainer: LinearLayout
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var addNewTimer: Button
    private lateinit var backButton: Button

    private lateinit var timerScheduler: TimerScheduler
    private val gson = Gson()
    private val ADD_TIMER_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_timer_list)

        timerScheduler = TimerScheduler(this)

        timerContainer = findViewById(R.id.timerContainer)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        addNewTimer = findViewById(R.id.btnAddTimer)
        backButton = findViewById(R.id.btnBack)

        // Add a new timer
        addNewTimer.setOnClickListener {
            val intent = Intent(this, DailyTimerActivity::class.java)
            startActivityForResult(intent, ADD_TIMER_REQUEST_CODE)
        }

        backButton.setOnClickListener { finish() }

        loadAndDisplayTimers()
    }

    /**
     * Handles the result from DailyTimerActivity when a new timer is created.
     * Schedules alarms and saves the timer.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_TIMER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {
                val startHour = it.getIntExtra("EXTRA_START_HOUR", -1)
                val startMinute = it.getIntExtra("EXTRA_START_MINUTE", -1)
                val endHour = it.getIntExtra("EXTRA_END_HOUR", -1)
                val endMinute = it.getIntExtra("EXTRA_END_MINUTE", -1)
                val selectedDays = it.getIntegerArrayListExtra("EXTRA_SELECTED_DAYS")

                if (startHour != -1 && selectedDays != null) {
                    val newTimer = DailyTimer(
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        daysOfWeek = selectedDays
                    )

                    // Schedule the alarms for the new timer
                    timerScheduler.schedule(newTimer)

                    saveNewTimer(newTimer)
                    loadAndDisplayTimers()
                }
            }
        }
    }


    /**
     * Loads all timers from SharedPreferences and displays them as buttons.
     * Long-clicking a timer deletes it.
     */
    private fun loadAndDisplayTimers() {
        timerContainer.removeAllViews()
        val timers = loadTimers()

        if (timers.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            timerContainer.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            timerContainer.visibility = View.VISIBLE
        }

        timers.forEach { timer ->
            val timerButton = Button(this).apply {
                val startTime = formatTime(timer.startHour, timer.startMinute)
                val endTime = formatTime(timer.endHour, timer.endMinute)
                val days = formatDays(timer.daysOfWeek)

                text = "$startTime - $endTime\nRepeats on: $days"
                isAllCaps = false
                setBackgroundColor(Color.parseColor("#333333"))
                setTextColor(Color.WHITE)
                setPadding(24, 24, 24, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }

                setOnLongClickListener {
                    deleteTimer(timer)
                    loadAndDisplayTimers()
                    Toast.makeText(
                        this@DailyTimerListActivity,
                        "Timer deleted.",
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
            }
            timerContainer.addView(timerButton)
        }
    }

    /** Formats hour and minute into a readable string (e.g., 3:30 PM) */
    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    /** Converts a list of day numbers to a string of day abbreviations */
    private fun formatDays(days: List<Int>): String {
        val dayMap = mapOf(
            1 to "Sun",
            2 to "Mon",
            3 to "Tue",
            4 to "Wed",
            5 to "Thu",
            6 to "Fri",
            7 to "Sat"
        )
        return days.sorted().joinToString(", ") { dayMap[it] ?: "" }
    }

    /** Loads timers from SharedPreferences */
    private fun loadTimers(): MutableList<DailyTimer> {
        val sharedPreferences = getSharedPreferences("DailyTimerPrefs", MODE_PRIVATE)
        val json = sharedPreferences.getString("daily_timers", null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<DailyTimer>>() {}.type
            gson.fromJson(json, type)
        } else mutableListOf()
    }

    /** Saves all timers to SharedPreferences */
    private fun saveTimers(timers: List<DailyTimer>) {
        val sharedPreferences = getSharedPreferences("DailyTimerPrefs", MODE_PRIVATE)
        val json = gson.toJson(timers)
        sharedPreferences.edit().putString("daily_timers", json).apply()
    }

    /** Adds a new timer to the list and saves it */
    private fun saveNewTimer(newTimer: DailyTimer) {
        val timers = loadTimers()
        timers.add(newTimer)
        saveTimers(timers)
    }

    /**
     * Deletes a timer, cancels its future alarms, and instantly unmutes
     * the phone if the deleted timer was currently active.
     */
    private fun deleteTimer(timerToDelete: DailyTimer) {
        // --- NEW LOGIC TO INSTANTLY UNMUTE ---
        // First, check if the timer being deleted is active right now.
        if (isTimerCurrentlyActive(timerToDelete)) {
            // It is active, so we need to unmute the phone.
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
        // --- END OF NEW LOGIC ---

        // Cancel all future alarms for this timer
        timerScheduler.cancel(timerToDelete)

        // Remove the timer from our saved list
        val timers = loadTimers()
        timers.removeAll { it.id == timerToDelete.id }
        saveTimers(timers)
    }


    /**
     * Checks if a specific daily timer is active at the current moment.
     * @param timer The DailyTimer to check.
     * @return True if the timer is currently active, false otherwise.
     */
    private fun isTimerCurrentlyActive(timer: DailyTimer): Boolean {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK)
        val currentTimeInMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        if (timer.daysOfWeek.contains(currentDay)) {
            val startTime = timer.startHour * 60 + timer.startMinute
            val endTime = timer.endHour * 60 + timer.endMinute
            if (currentTimeInMinutes in startTime until endTime) {
                return true
            }
        }
        return false
    }

}