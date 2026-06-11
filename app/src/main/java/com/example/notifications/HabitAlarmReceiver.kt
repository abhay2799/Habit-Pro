package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class HabitAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("HabitAlarmReceiver", "Alarm broadcast received successfully!")
        if (context == null) return

        val title = intent?.getStringExtra("alarm_title") ?: "⏱️ Zenter Habit Alarm!"
        val message = intent?.getStringExtra("alarm_message") ?: "Time to lock in your active goal streak! Take a deep breath."
        val isSmartNotification = intent?.getBooleanExtra("is_smart_notification", false) ?: false

        if (isSmartNotification) {
            val prefs = context.getSharedPreferences("zenter_tracker_shared_prefs", Context.MODE_PRIVATE)
            val quietStart = prefs.getString("quiet_hours_start", "22:00") ?: "22:00"
            val quietEnd = prefs.getString("quiet_hours_end", "07:00") ?: "07:00"
            
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)
            val currentTimeVal = currentHour * 60 + currentMinute
            
            val startVal = try {
                val startParts = quietStart.split(":")
                startParts[0].trim().toInt() * 60 + startParts[1].trim().toInt()
            } catch (e: Exception) {
                22 * 60 // Default to 22:00
            }
            
            val endVal = try {
                val endParts = quietEnd.split(":")
                endParts[0].trim().toInt() * 60 + endParts[1].trim().toInt()
            } catch (e: Exception) {
                7 * 60 // Default to 07:00
            }
            
            val inQuietHours = if (startVal > endVal) {
                // Crosses midnight (e.g. 22:00 to 07:00)
                currentTimeVal >= startVal || currentTimeVal <= endVal
            } else {
                currentTimeVal >= startVal && currentTimeVal <= endVal
            }
            
            if (inQuietHours) {
                Log.d("HabitAlarmReceiver", "Skipping smart notification due to quiet hours.")
                return
            }
        }

        // Acquire temporary wakelock to make sure screen wakes up under screen-off
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "zenter:alarm_wakelock"
        )
        wakeLock?.acquire(3000L) // active for 3 seconds of wakeup vibration

        // Play default Notification/Alarm ringtone
        try {
            val notificationRing = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationRing)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("HabitAlarmReceiver", "Failed to play ringtone chime", e)
        }

        // Send high-priority notification with full-screen launcher intent
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "zenter_alarms_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "High-Priority Habit Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm triggers for critical tasks and hydration targets."
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to launch MainActivity with visual overlay parameters
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_alarm_overlay", true)
            putExtra("alarm_overlay_title", title)
            putExtra("alarm_overlay_message", message)
        }

        // High priority fullscreen pending intent
        val pendingIntent = PendingIntent.getActivity(
            context,
            2026,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // Force overlay popup on lock screen
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(777, builder.build())
        } catch (e: SecurityException) {
            Log.e("HabitAlarmReceiver", "Notification capability restricted", e)
        }
    }
}
