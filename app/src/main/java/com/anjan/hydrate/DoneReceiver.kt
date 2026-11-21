package com.anjan.hydrate

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "Done" button: just clears the notification. Nothing to open. */
class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(ReminderReceiver.NOTIFICATION_ID)
    }
}
