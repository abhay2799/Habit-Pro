package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("ReminderReceiver", "Broadcast received.")
        if (context == null) return

        val title = intent?.getStringExtra("reminder_title") ?: "Habits Check-In!"
        val message = intent?.getStringExtra("reminder_message") ?: "Keep up your daily streaks. Tap here to complete your habits now."

        NotificationHelper.showImmediateNotification(context, title, message)
    }
}
