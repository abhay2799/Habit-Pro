package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * BootReceiver — reschedules all habit alarms after device reboot or app update.
 * Reads all stored alarm times from SharedPrefs and re-registers them with AlarmManager.
 * This ensures alarms survive reboot (AlarmManager is wiped on reboot without this).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val action = intent?.action ?: return

        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.d("BootReceiver", "Boot/Update detected — rescheduling all habit alarms")

        val prefs = context.getSharedPreferences("zenter_tracker_shared_prefs", Context.MODE_PRIVATE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Scan all prefs for keys matching "habit_alarm_<id>"
        val allPrefs = prefs.all
        for ((key, value) in allPrefs) {
            if (!key.startsWith("habit_alarm_")) continue
            val habitIdStr = key.removePrefix("habit_alarm_")
            val habitId = habitIdStr.toLongOrNull() ?: continue
            val timeStr = value as? String ?: continue
            if (timeStr.isBlank()) continue

            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: continue
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: continue

            // Read stored habit name for this alarm
            val habitName = prefs.getString("habit_name_$habitId", "Your Habit") ?: "Your Habit"
            val alertType = prefs.getString("habit_alert_type_$habitId", "Notification") ?: "Notification"

            rescheduleAlarm(context, alarmManager, habitId, habitName, alertType, hour, minute)
            Log.d("BootReceiver", "Rescheduled alarm for habit $habitId ($habitName) at $hour:$minute [$alertType]")
        }
    }

    private fun rescheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        habitId: Long,
        habitName: String,
        alertType: String,
        hour: Int,
        minute: Int
    ) {
        val alarmIntent = Intent(context, HabitAlarmReceiver::class.java).apply {
            putExtra("habit_id", habitId)
            putExtra("habit_name", habitName)
            putExtra("alert_type", alertType)
            putExtra("alarm_title", "⏰ Time for $habitName!")
            putExtra("alarm_message", "Don't break your streak! Log $habitName now.")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.toInt() + 1000,
            alarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("BootReceiver", "Failed to reschedule alarm for habit $habitId", e)
        }
    }
}
