package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

/**
 * HabitAlarmReceiver — handles both Notification and Alarm types.
 *
 * Fixes applied:
 * - Habit name always shown in notification title + expanded text
 * - Full-screen alarm card shown on lock screen when type = Alarm
 * - Dismiss button stops MediaPlayer sound immediately
 * - Channels recreated with v2 IDs to avoid stale cached channels
 * - Notification posted even when app is closed
 */
class HabitAlarmReceiver : BroadcastReceiver() {

    companion object {
        // v2 channel IDs — forces fresh channel creation, bypassing old broken cached channels
        const val CHANNEL_NOTIFICATION = "habit_notifications_v2"
        const val CHANNEL_ALARM = "habit_alarms_v2"
        const val ACTION_DISMISS = "com.example.DISMISS_ALARM"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_ALERT_TYPE = "alert_type"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // Handle dismiss — stop alarm sound and cancel notification
        if (intent.action == ACTION_DISMISS) {
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (habitId >= 0) nm.cancel(habitId.toInt()) else nm.cancel(10001)
            Log.d("HabitAlarmReceiver", "Alarm dismissed for habit $habitId")
            return
        }

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Your Habit"
        val alertType = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: "Notification"
        val isSmartNotification = intent.getBooleanExtra("is_smart_notification", false)

        Log.d("HabitAlarmReceiver", "Received [$alertType] for '$habitName' (id=$habitId)")

        // Quiet hours check (smart notifications only)
        if (isSmartNotification && isInQuietHours(context)) {
            Log.d("HabitAlarmReceiver", "Skipping — inside quiet hours")
            rescheduleForNextDay(context, habitId, habitName, alertType)
            return
        }

        // Wake the device so notification appears
        acquireWakelock(context)

        // Ensure channels exist with correct settings
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(nm)

        when (alertType) {
            "Alarm" -> handleAlarm(context, nm, habitId, habitName)
            else    -> handleNotification(context, nm, habitId, habitName)
        }

        // Reschedule for next day
        rescheduleForNextDay(context, habitId, habitName, alertType)
    }

    // ─── Notification (gentle reminder) ────────────────────────────────
    private fun handleNotification(
        context: Context, nm: NotificationManager, habitId: Long, habitName: String
    ) {
        val tapIntent = PendingIntent.getActivity(
            context, habitId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val appIcon = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.mipmap.ic_launcher)
        val notification = NotificationCompat.Builder(context, CHANNEL_NOTIFICATION)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setLargeIcon(appIcon)
            .setContentTitle("🔔 Habit Reminder: $habitName")
            .setContentText("Don't break your streak! Time to log \"$habitName\".")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Don't break your streak! Time to log \"$habitName\". Tap to open the app and mark it done.")
                    .setBigContentTitle("🔔 Habit Reminder: $habitName")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .setVibrate(longArrayOf(0, 300, 100, 300))
            .build()

        val notifId = if (habitId >= 0) habitId.toInt() else 10000
        try {
            nm.notify(notifId, notification)
            Log.d("HabitAlarmReceiver", "✅ Notification posted for '$habitName' (id=$notifId)")
        } catch (e: SecurityException) {
            Log.e("HabitAlarmReceiver", "❌ Notification permission denied", e)
        } catch (e: Exception) {
            Log.e("HabitAlarmReceiver", "❌ Failed to post notification", e)
        }
    }

    // ─── Alarm (loud + full-screen card on lock screen) ────────────────
    private fun handleAlarm(
        context: Context, nm: NotificationManager, habitId: Long, habitName: String
    ) {
        // Full screen intent to launch AlarmActivity
        val fullScreenIntent = Intent(context, com.example.ui.screens.AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, habitName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Dismiss button PendingIntent
        val dismissIntent = PendingIntent.getBroadcast(
            context,
            (habitId + 5000).toInt(),
            Intent(context, HabitAlarmReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_HABIT_ID, habitId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Tap → open app
        val tapIntent = PendingIntent.getActivity(
            context, habitId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val appIcon = android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.mipmap.ic_launcher)
        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setLargeIcon(appIcon)
            .setContentTitle("⏰ Alarm: $habitName")
            .setContentText("Your scheduled habit alarm is ringing. Tap Dismiss to stop.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your alarm for \"$habitName\" is ringing!\nTap [✓ Dismiss] below to stop the sound.")
                    .setBigContentTitle("⏰ Alarm: $habitName")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)           // Cannot be swiped away
            .setFullScreenIntent(fullScreenPendingIntent, true)   // Shows on lock screen as alarm card
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_delete, "✓ Dismiss Alarm", dismissIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        // FLAG_INSISTENT makes the alarm ringtone loop continuously until dismissed
        notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT

        val notifId = if (habitId >= 0) habitId.toInt() else 10001
        try {
            nm.notify(notifId, notification)
            Log.d("HabitAlarmReceiver", "✅ Alarm notification posted for '$habitName' (id=$notifId)")
        } catch (e: SecurityException) {
            Log.e("HabitAlarmReceiver", "❌ Notification permission denied", e)
        } catch (e: Exception) {
            Log.e("HabitAlarmReceiver", "❌ Failed to post alarm notification", e)
        }
    }

    // ─── Channel Creation ───────────────────────────────────────────────
    private fun ensureChannels(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // Notification channel — default sound + vibration
        if (nm.getNotificationChannel(CHANNEL_NOTIFICATION) == null) {
            val notifChannel = NotificationChannel(
                CHANNEL_NOTIFICATION,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to log your daily habits"
                enableVibration(true)
                // No setSound() → uses system default notification sound
            }
            nm.createNotificationChannel(notifChannel)
            Log.d("HabitAlarmReceiver", "Created notification channel")
        }

        // Alarm channel — high priority, alarm sound
        if (nm.getNotificationChannel(CHANNEL_ALARM) == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "Habit Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm-style reminders for habits"
                enableVibration(true)
                enableLights(true)
                setSound(
                    alarmUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            nm.createNotificationChannel(alarmChannel)
            Log.d("HabitAlarmReceiver", "Created alarm channel")
        }
    }

    // ─── Wakelock ──────────────────────────────────────────────────────
    private fun acquireWakelock(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "habitpro:alarm_wakelock")
                ?.acquire(15_000L)
        } catch (e: Exception) {
            Log.e("HabitAlarmReceiver", "Wakelock failed", e)
        }
    }

    // ─── Quiet Hours ───────────────────────────────────────────────────
    private fun isInQuietHours(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences("zenter_tracker_shared_prefs", Context.MODE_PRIVATE)
            val quietStart = prefs.getString("quiet_hours_start", "22:00") ?: "22:00"
            val quietEnd = prefs.getString("quiet_hours_end", "07:00") ?: "07:00"
            val cal = java.util.Calendar.getInstance()
            val now = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val startParts = quietStart.split(":")
            val endParts = quietEnd.split(":")
            val startVal = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endVal = endParts[0].toInt() * 60 + endParts[1].toInt()
            if (startVal > endVal) now >= startVal || now <= endVal
            else now in startVal..endVal
        } catch (e: Exception) { false }
    }

    // ─── Daily Reschedule ──────────────────────────────────────────────
    private fun rescheduleForNextDay(
        context: Context, habitId: Long, habitName: String, alertType: String
    ) {
        if (habitId < 0) return
        try {
            val prefs = context.getSharedPreferences("zenter_tracker_shared_prefs", Context.MODE_PRIVATE)
            val timeStr = prefs.getString("habit_alarm_$habitId", "") ?: ""
            if (timeStr.isBlank()) return
            val parts = timeStr.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            val nextDay = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            val nextIntent = Intent(context, HabitAlarmReceiver::class.java).apply {
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_HABIT_NAME, habitName)
                putExtra(EXTRA_ALERT_TYPE, alertType)
            }
            val pi = PendingIntent.getBroadcast(
                context, habitId.toInt() + 1000, nextIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, nextDay.timeInMillis, pi)
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, nextDay.timeInMillis, pi)
            }
            Log.d("HabitAlarmReceiver", "Rescheduled '$habitName' for next day at $hour:$minute")
        } catch (e: Exception) {
            Log.e("HabitAlarmReceiver", "Failed to reschedule", e)
        }
    }
}
