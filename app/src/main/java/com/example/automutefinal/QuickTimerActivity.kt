package com.example.automutefinal

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class QuickTimerActivity : AppCompatActivity() {

    private lateinit var minutesInput: EditText
    private lateinit var startTimerButton: Button
    private lateinit var countdownTextView: TextView
    private lateinit var cancelTimerButton: Button
    private lateinit var inputLayout: LinearLayout
    private lateinit var timerLayout: LinearLayout

    private var uiCountdownTimer: CountDownTimer? = null
    private lateinit var alarmManager: AlarmManager

    private val sharedPrefsName = "AppSettings"
    private val quickTimerFlag = "isQuickTimerActive"
    private val timerEndTimeKey = "quickTimerEndTime"

    /**
     * Initializes the activity:
     * - Sets the layout
     * - Binds UI elements
     * - Initializes AlarmManager
     * - Sets up button click listeners for starting and cancelling the quick timer
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_timer)

        minutesInput = findViewById(R.id.minutesInput)
        startTimerButton = findViewById(R.id.startTimerButton)
        countdownTextView = findViewById(R.id.countdownTextView)
        cancelTimerButton = findViewById(R.id.cancelTimerButton)
        inputLayout = findViewById(R.id.inputLayout)
        timerLayout = findViewById(R.id.timerLayout)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        startTimerButton.setOnClickListener { startQuickTimer() }
        cancelTimerButton.setOnClickListener { cancelQuickTimer() }
    }

    /**
     * Called when activity comes to foreground.
     * Checks if a quick timer is active and updates the UI accordingly.
     */
    override fun onResume() {
        super.onResume()
        checkTimerStateAndUpdateUi()
    }

    /**
     * Called when activity goes to background.
     * Cancels the UI countdown to prevent memory leaks.
     */
    override fun onPause() {
        super.onPause()
        uiCountdownTimer?.cancel()
    }

    /**
     * Starts a quick timer based on user input.
     * - Sets an exact alarm using AlarmManager
     * - Activates Do Not Disturb (DND) if permission is granted
     * - Updates shared preferences to store timer state and end time
     * - Updates the UI to show countdown
     */
    private fun startQuickTimer() {
        val minutes = minutesInput.text.toString().toLongOrNull()

        if (minutes == null || minutes <= 0) {
            Toast.makeText(this, "Enter a valid number greater than 0", Toast.LENGTH_SHORT).show()
            return
        }

        val triggerAtMillis = System.currentTimeMillis() + (minutes * 60 * 1000)
        val intent = Intent(this, QuickTimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                Toast.makeText(this, "Permission needed to set exact alarms", Toast.LENGTH_LONG).show()
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                    startActivity(it)
                }
                return
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
        Log.d("AutoMuteApp", "Quick Timer alarm has been set successfully.")

        val prefs = getSharedPreferences(sharedPrefsName, MODE_PRIVATE)
        prefs.edit()
            .putBoolean(quickTimerFlag, true)
            .putLong(timerEndTimeKey, triggerAtMillis)
            .apply()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            Toast.makeText(this, "Timer started. Phone is now on Do Not Disturb.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Timer started, but DND permission is needed.", Toast.LENGTH_LONG).show()
        }

        checkTimerStateAndUpdateUi()
    }

    /**
     * Cancels the active quick timer.
     * - Cancels the AlarmManager alarm
     * - Restores the phone’s ringer mode and DND settings
     * - Updates the UI
     */
    private fun cancelQuickTimer() {
        val intent = Intent(this, QuickTimerReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        restoreRinger()
        Toast.makeText(this, "Timer cancelled.", Toast.LENGTH_SHORT).show()
    }

    /**
     * Restores the phone’s ringer mode and DND settings to normal.
     * - Marks the quick timer as inactive in shared preferences
     * - Updates the UI accordingly
     */
    private fun restoreRinger() {
        val prefs = getSharedPreferences(sharedPrefsName, MODE_PRIVATE)
        prefs.edit()
            .putBoolean(quickTimerFlag, false)
            .remove(timerEndTimeKey)
            .apply()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            Log.d("AutoMuteApp", "DND filter set to ALL by restoreRinger.")
        }

        checkTimerStateAndUpdateUi()
    }

    /**
     * Checks the state of the quick timer from shared preferences.
     * - Updates UI to show input layout if timer is inactive
     * - Shows countdown layout if timer is active
     * - Starts the UI countdown if needed
     */
    private fun checkTimerStateAndUpdateUi() {
        val prefs = getSharedPreferences(sharedPrefsName, MODE_PRIVATE)
        val isTimerActive = prefs.getBoolean(quickTimerFlag, false)

        if (isTimerActive) {
            val endTime = prefs.getLong(timerEndTimeKey, 0)
            val remainingMillis = endTime - System.currentTimeMillis()

            if (remainingMillis > 0) {
                inputLayout.visibility = View.GONE
                timerLayout.visibility = View.VISIBLE
                startUiCountdown(remainingMillis)
            } else {
                restoreRinger()
            }
        } else {
            inputLayout.visibility = View.VISIBLE
            timerLayout.visibility = View.GONE
            minutesInput.text.clear()
            uiCountdownTimer?.cancel()
        }
    }

    /**
     * Starts a countdown timer in the UI for the remaining duration.
     * Updates the TextView every second.
     * Calls checkTimerStateAndUpdateUi() when finished.
     */
    private fun startUiCountdown(milliseconds: Long) {
        uiCountdownTimer?.cancel()
        uiCountdownTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                countdownTextView.text = String.format("%02d:%02d", minutes, seconds)
            }
            override fun onFinish() {
                checkTimerStateAndUpdateUi()
            }
        }.start()
    }
}
