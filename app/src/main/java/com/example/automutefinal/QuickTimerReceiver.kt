package com.example.automutefinal

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log

class QuickTimerReceiver : BroadcastReceiver() {

    /**
     * Called when the alarm set by QuickTimerActivity goes off.
     * Responsibilities:
     * 1. Logs that the alarm has been received.
     * 2. Restores the phone's ringer and Do Not Disturb (DND) settings to normal.
     * 3. Marks the quick timer as inactive in shared preferences.
     *
     * This ensures that after the quick timer duration, the phone returns to its normal sound settings.
     */
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("QuickTimerReceiver", "Alarm received, restoring ringer.")
        Log.d("AutoMuteApp", "QuickTimerReceiver was triggered by the alarm!")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Check for DND permission before changing the filter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        val sharedPrefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("isQuickTimerActive", false).apply()
    }
}
