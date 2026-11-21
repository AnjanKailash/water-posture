package com.anjan.hydrate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.provider.Settings

/** Fires on every interval: shows the reminder unless the phone is in airplane or DND mode. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context)) return

        // Always queue the next one first, so a skipped reminder never stops the cycle.
        Scheduler.scheduleNext(context)

        if (isAirplaneModeOn(context) || isDndOn(context)) return  // stay silent

        show(context)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val NOTIFICATION_ID = 42

        fun isAirplaneModeOn(c: Context): Boolean =
            Settings.Global.getInt(c.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

        fun isDndOn(c: Context): Boolean {
            val f = c.getSystemService(NotificationManager::class.java).currentInterruptionFilter
            return f != NotificationManager.INTERRUPTION_FILTER_ALL &&
                   f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        }

        fun ensureChannel(c: Context) {
            val nm = c.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Water & posture reminders",
                        NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Reminds you to drink water and sit up straight"
                        enableVibration(true)
                    }
                )
            }
        }

        fun show(c: Context) {
            ensureChannel(c)
            val nm = c.getSystemService(NotificationManager::class.java)

            // One tap on "Done" (or on the notification itself) clears it.
            val donePi = PendingIntent.getBroadcast(
                c, 2002, Intent(c, DoneReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val n = Notification.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_water)
                .setContentTitle("💧 Drink water  •  🧍 Sit up straight")
                .setContentText("Take a sip and fix your posture.")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(donePi)
                .addAction(Notification.Action.Builder(null as Icon?, "Done ✓", donePi).build())
                .setTimeoutAfter(Prefs.intervalMinutes(c) * 60_000L - 10_000L) // auto-clear before next
                .build()

            nm.notify(NOTIFICATION_ID, n)
        }
    }
}
