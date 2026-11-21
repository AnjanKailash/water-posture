package com.anjan.hydrate

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "Done" button (or tap / swipe): clears the notification and stops repeat alerts. */
class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.setPendingAck(context, false)
        Scheduler.cancelNag(context)
        context.getSystemService(NotificationManager::class.java)
            .cancel(ReminderReceiver.NOTIFICATION_ID)
    }
}
