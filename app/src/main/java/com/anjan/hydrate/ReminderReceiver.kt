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
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

/**
 * Fires on every interval: shows the reminder (unless Airplane mode / DND is on),
 * rings the alarm tone once and/or vibrates, and queues the next reminder.
 * The reminder stays on screen until the user taps Done.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context)) return

        if (intent.action == Scheduler.ACTION_SNOOZE_FIRE) {
            // Snoozed reminder coming back: show it again, regular schedule untouched.
            if (isAirplaneModeOn(context) || isDndOn(context)) return
            val pending = goAsync()
            show(context) { pending.finish() }
            return
        }

        // Always queue the next one first, so a skipped reminder never stops the cycle.
        Scheduler.scheduleNext(context)
        Scheduler.cancelSnooze(context)  // a fresh reminder replaces any pending snooze

        if (isAirplaneModeOn(context) || isDndOn(context)) return  // stay silent

        // Keep the receiver alive while the alarm tone plays (a few seconds).
        val pending = goAsync()
        show(context) { pending.finish() }
    }

    companion object {
        const val NOTIFICATION_ID = 42

        private const val CHANNEL = "reminders_v4"   // silent channel; the app does the alerting
        private val OLD_CHANNELS = listOf(
            "reminders", "alert_sound_vibrate", "alert_vibrate", "alert_sound", "reminders_v3"
        )

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
            OLD_CHANNELS.forEach { nm.deleteNotificationChannel(it) }
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Water & posture reminders",
                        NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Reminds you to drink water and sit up straight"
                        setSound(null, null)
                        enableVibration(false)
                    }
                )
            }
        }

        /**
         * Vibrate and/or ring the alarm tone once, following the chosen style and the
         * phone's ringer (OnePlus alert slider): Silent = nothing, Vibrate = buzz only.
         */
        private fun alert(c: Context, finished: () -> Unit) {
            val mode = Prefs.alertMode(c)
            val ringer = c.getSystemService(AudioManager::class.java).ringerMode
            if (ringer == AudioManager.RINGER_MODE_SILENT) { finished(); return }

            val wantVibe = mode == Prefs.ALERT_VIBRATE || mode == Prefs.ALERT_BOTH
            val wantSound = (mode == Prefs.ALERT_SOUND || mode == Prefs.ALERT_BOTH) &&
                            ringer == AudioManager.RINGER_MODE_NORMAL

            if (wantVibe) {
                val vibrator: Vibrator =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        c.getSystemService(VibratorManager::class.java).defaultVibrator
                    else
                        @Suppress("DEPRECATION") c.getSystemService(Vibrator::class.java)
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION, -1), attrs)
            }

            if (wantSound) AlarmSound.playOnce(c, finished) else finished()
        }

        fun show(c: Context, finished: () -> Unit = {}) {
            ensureChannel(c)
            val nm = c.getSystemService(NotificationManager::class.java)

            val donePi = PendingIntent.getBroadcast(
                c, 2002, Intent(c, DoneReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozePi = PendingIntent.getBroadcast(
                c, 2003, Intent(c, SnoozeReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val n = Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_water)
                .setColor(0xFF1A8FD6.toInt())
                .setContentTitle("💧 Drink water  •  🧍 Sit up straight")
                .setContentText("Take a sip, straighten your back, then tap Done.")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setOngoing(true)            // stays until Done is tapped
                .setAutoCancel(true)
                .setContentIntent(donePi)
                .addAction(Notification.Action.Builder(null as Icon?,
                    "Snooze ${Scheduler.SNOOZE_MINUTES} min", snoozePi).build())
                .addAction(Notification.Action.Builder(null as Icon?, "Done ✓", donePi).build())
                .build()

            nm.notify(NOTIFICATION_ID, n)
            alert(c, finished)
        }
    }
}
