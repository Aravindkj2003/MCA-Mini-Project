package com.example.automutefinal

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity for creating a Daily Timer.
 *
 * Allows the user to select a start and end time, choose the days of the week
 * the timer should repeat, and save the timer.
 */
class DailyTimerActivity : AppCompatActivity() {

    private lateinit var startTimePicker: TimePicker
    private lateinit var endTimePicker: TimePicker
    private lateinit var saveButton: Button
    private lateinit var checkboxes: Map<Int, CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_timer)

        startTimePicker = findViewById(R.id.startTimePicker)
        endTimePicker = findViewById(R.id.endTimePicker)
        saveButton = findViewById(R.id.saveDailyTimerButton)

        // Set TimePickers to 24-hour view
        startTimePicker.setIs24HourView(true)
        endTimePicker.setIs24HourView(true)

        // Map each day of the week to its checkbox
        checkboxes = mapOf(
            1 to findViewById(R.id.sundayCheckbox),
            2 to findViewById(R.id.mondayCheckbox),
            3 to findViewById(R.id.tuesdayCheckbox),
            4 to findViewById(R.id.wednesdayCheckbox),
            5 to findViewById(R.id.thursdayCheckbox),
            6 to findViewById(R.id.fridayCheckbox),
            7 to findViewById(R.id.saturdayCheckbox)
        )

        // Handle save button click
        saveButton.setOnClickListener {
            if (checkDnDPermission()) {
                saveTimerAndReturn()
            }
        }
    }

    /**
     * Saves the timer details (start/end time and selected days)
     * and returns the data to the calling activity.
     */
    private fun saveTimerAndReturn() {
        val startHour = startTimePicker.hour
        val startMinute = startTimePicker.minute
        val endHour = endTimePicker.hour
        val endMinute = endTimePicker.minute

        val selectedDays = ArrayList<Int>()
        for ((dayOfWeek, checkbox) in checkboxes) {
            if (checkbox.isChecked) {
                selectedDays.add(dayOfWeek)
            }
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day to repeat.", Toast.LENGTH_SHORT).show()
            return
        }

        val resultIntent = Intent().apply {
            putExtra("EXTRA_START_HOUR", startHour)
            putExtra("EXTRA_START_MINUTE", startMinute)
            putExtra("EXTRA_END_HOUR", endHour)
            putExtra("EXTRA_END_MINUTE", endMinute)
            putIntegerArrayListExtra("EXTRA_SELECTED_DAYS", selectedDays)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Checks if Do Not Disturb (DND) permission is granted.
     * Opens settings if permission is not granted.
     *
     * return true if permission is granted, false otherwise
     */
    private fun checkDnDPermission(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (!notificationManager.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Please grant DND access for the timer to work", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
            false
        } else true
    }
}
