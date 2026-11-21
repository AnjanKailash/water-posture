package com.anjan.hydrate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.provider.Settings

/**
 * Main alarm: shows the reminder (unless Airplane mode / DND is on) and queues the next one.
 * Nag alarm: re-alerts every 2 minutes, up to 3 times, until the user taps Done.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context)) return

        if (intent.action == Scheduler.ACTION_NAG) {
            handleNag(context)
            return
        }

        // Always queue the next one first, so a skipped reminder never stops the cycle.
        Scheduler.scheduleNext(context)
        Scheduler.cancelNag(context)
        Prefs.setPendingAck(context, false)

        if (isAirplaneModeOn(context) || isDndOn(context)) return  // stay silent

        show(context)
        if (Prefs.repeatUntilDone(context)) {
            Prefs.setPendingAck(context, true)
            Prefs.setNagCount(context, 0)
            Scheduler.scheduleNag(context)
        }
    }

    private fun handleNag(c: Context) {
        if (!Prefs.pendingAck(c) || !Prefs.repeatUntilDone(c)) return
        if (isAirplaneModeOn(c) || isDndOn(c)) return
        val n = Prefs.nagCount(c) + 1
        Prefs.setNagCount(c, n)
        show(c)  // re-posting with the same ID buzzes / rings again
        if (n < Scheduler.MAX_NAGS) Scheduler.scheduleNag(c) else Prefs.setPendingAck(c, false)
    }

    companion object {
        const val NOTIFICATION_ID = 42

        // Android locks sound/vibration per channel, so each alert style gets its own channel.
        private const val CH_BOTH = "alert_sound_vibrate"
        private const val CH_VIBRATE = "alert_vibrate"
        private const val CH_SOUND = "alert_sound"
        private const val OLD_CHANNEL = "reminders"  // from v1, silent-ish

        private val VIBRATION = longArrayOf(0, 700, 300, 700, 300, 700)

        fun isAirplaneModeOn(c: Context): Boolean =
            Settings.Global.getInt(c.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1

        fun isDndOn(c: Context): Boolean {
            val f = c.getSystemService(NotificationManager::class.java).currentInterruptionFilter
            return f != NotificationManager.INTERRUPTION_FILTER_ALL &&
                   f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        }

        fun ensureChannel(c: Context) {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.deleteNotificationChannel(OLD_CHANNEL)

            val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            fun make(id: String, name: String, withSound: Boolean, withVibe: Boolean) {
                if (nm.getNotificationChannel(id) != null) return
                nm.createNotificationChannel(
                    NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Reminds you to drink water and sit up straight"
                        if (withSound) setSound(sound, attrs) else setSound(null, null)
                        enableVibration(withVibe)
                        if (withVibe) vibrationPattern = VIBRATION
                    }
                )
            }
            make(CH_BOTH, "Reminders: sound + vibrate", true, true)
            make(CH_VIBRATE, "Reminders: vibrate only", false, true)
            make(CH_SOUND, "Reminders: sound only", true, false)
        }

        private fun channelFor(c: Context) = when (Prefs.alertMode(c)) {
            Prefs.ALERT_SOUND -> CH_SOUND
            Prefs.ALERT_VIBRATE -> CH_VIBRATE
            else -> CH_BOTH
        }

        fun show(c: Context) {
            ensureChannel(c)
            val nm = c.getSystemService(NotificationManager::class.java)

            // One tap on "Done" (or on the notification, or swiping it away) stops the repeats.
            val donePi = PendingIntent.getBroadcast(
                c, 2002, Intent(c, DoneReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val n = Notification.Builder(c, channelFor(c))
                .setSmallIcon(R.drawable.ic_water)
                .setContentTitle("💧 Drink water  •  🧍 Sit up straight")
                .setContentText("Take a sip and fix your posture. Tap Done when finished.")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(donePi)
                .setDeleteIntent(donePi)
                .addAction(Notification.Action.Builder(null as Icon?, "Done ✓", donePi).build())
                .setTimeoutAfter(Prefs.intervalMinutes(c) * 60_000L - 10_000L) // auto-clear before next
                .build()

            nm.cancel(NOTIFICATION_ID)  // ensure the re-post alerts again
            nm.notify(NOTIFICATION_ID, n)
        }
    }
}
