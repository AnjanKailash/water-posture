package com.anjan.hydrate

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** "Done" button: stops the sound, clears the reminder, counts it for today. */
class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = markDone(context)

    companion object {
        fun markDone(c: Context) {
            AlarmSound.stop()
            Scheduler.cancelSnooze(c)
            c.getSystemService(NotificationManager::class.java)
                .cancel(ReminderReceiver.NOTIFICATION_ID)
            Prefs.incrementDoneToday(c)
            Prefs.logEvent(c, "✓ Done")
        }
    }
}
