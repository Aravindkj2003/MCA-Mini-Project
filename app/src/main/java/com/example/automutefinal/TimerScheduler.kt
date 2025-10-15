package com.example.automutefinal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

class TimerScheduler(private val context: Context) {

    // System alarm manager to schedule and cancel alarms
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules all alarms for a DailyTimer based on its daysOfWeek.
     * Saves the timer to local storage and sets alarms for start and end times.
     */
    fun schedule(timer: DailyTimer) {
        DailyTimerRepository.saveTimer(context, timer)

        timer.daysOfWeek.forEach { dayOfWeek ->
            scheduleAlarm(timer, dayOfWeek, timer.startHour, timer.startMinute, TimerAlarmReceiver.ACTION_SET_SILENT)
            scheduleAlarm(timer, dayOfWeek, timer.endHour, timer.endMinute, TimerAlarmReceiver.ACTION_SET_NORMAL)
        }
    }

    /**
     * Cancels all alarms associated with a DailyTimer.
     * Iterates through all scheduled days and cancels both start (silent) and end (normal) alarms.
     */
    fun cancel(timer: DailyTimer) {
        timer.daysOfWeek.forEach { dayOfWeek ->
            cancelAlarm(timer, dayOfWeek, TimerAlarmReceiver.ACTION_SET_SILENT)
            cancelAlarm(timer, dayOfWeek, TimerAlarmReceiver.ACTION_SET_NORMAL)
        }
    }

    /**
     * Schedules a single alarm for a specific day, hour, minute, and action (silent or normal).
     * Uses setExactAndAllowWhileIdle for precise alarm triggering.
     */
    fun scheduleAlarm(timer: DailyTimer, dayOfWeek: Int, hour: Int, minute: Int, action: String) {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            this.action = action
            putExtra("TIMER_ID", timer.id)
            putExtra("DAY_OF_WEEK", dayOfWeek)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            generateRequestCode(timer.id, dayOfWeek, action),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the alarm time and adjust for past times to schedule next week
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    /**
     * Cancels a specific alarm for a given DailyTimer, day, and action.
     * Uses the same request code as the scheduled alarm to identify it.
     */
    private fun cancelAlarm(timer: DailyTimer, dayOfWeek: Int, action: String) {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            this.action = action
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            generateRequestCode(timer.id, dayOfWeek, action),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    /**
     * Generates a unique integer request code for each alarm.
     * Combines timerId, dayOfWeek, and action hash codes to avoid conflicts.
     */
    private fun generateRequestCode(timerId: String, dayOfWeek: Int, action: String): Int {
        return (timerId.hashCode() + dayOfWeek + action.hashCode())
    }
}
