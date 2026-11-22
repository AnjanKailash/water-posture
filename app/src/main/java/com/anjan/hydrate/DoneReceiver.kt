package com.anjan.hydrate

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "Done" button (or tapping the reminder): stops the sound, clears it, counts it for today. */
class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmSound.stop()
        context.getSystemService(NotificationManager::class.java)
            .cancel(ReminderReceiver.NOTIFICATION_ID)
        Prefs.incrementDoneToday(context)
    }
}
