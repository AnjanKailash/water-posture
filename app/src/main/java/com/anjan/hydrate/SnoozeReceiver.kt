package com.anjan.hydrate

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "Snooze" button: stops the sound, hides the reminder, and brings it back in a few minutes. */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmSound.stop()
        context.getSystemService(NotificationManager::class.java)
            .cancel(ReminderReceiver.NOTIFICATION_ID)
        Scheduler.scheduleSnooze(context)
    }
}
