package com.example.automutefinal

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast

class TimerAlarmReceiver : BroadcastReceiver() {

    companion object {
        // Action constants to identify whether the alarm is to set phone silent or normal
        const val ACTION_SET_SILENT = "com.example.automutefinal.SET_SILENT"
        const val ACTION_SET_NORMAL = "com.example.automutefinal.SET_NORMAL"
    }

    /**
     * Called when a Daily Timer alarm is triggered.
     * Checks DND permission and sets phone ringer mode according to the alarm action.
     * Also reschedules the alarm for the next occurrence if needed.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Check if DND permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !notificationManager.isNotificationPolicyAccessGranted
        ) {
            Log.d("TimerAlarmReceiver", "DND permission not granted")
            Toast.makeText(context, "Please grant Do Not Disturb access for Daily Timer to work", Toast.LENGTH_LONG).show()
            return
        }

        // Retrieve timer details from intent
        val timerId = intent.getStringExtra("TIMER_ID") ?: return
        val dayOfWeek = intent.getIntExtra("DAY_OF_WEEK", -1)
        val dailyTimer = DailyTimerRepository.getTimerById(context, timerId) ?: return

        // Set ringer mode based on action
        when (intent.action) {
            ACTION_SET_SILENT -> {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                Log.d("TimerAlarmReceiver", "Daily Timer started: DND ON")
            }
            ACTION_SET_NORMAL -> {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                Log.d("TimerAlarmReceiver", "Daily Timer ended: DND OFF")
            }
        }

        // Reschedule the alarm for the next occurrence if dayOfWeek is valid
        if (dayOfWeek != -1) {
            val scheduler = TimerScheduler(context)
            scheduler.scheduleAlarm(dailyTimer, dayOfWeek, dailyTimer.startHour, dailyTimer.startMinute, ACTION_SET_SILENT)
            scheduler.scheduleAlarm(dailyTimer, dayOfWeek, dailyTimer.endHour, dailyTimer.endMinute, ACTION_SET_NORMAL)
        }
    }
}
