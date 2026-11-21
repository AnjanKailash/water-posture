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
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings

/**
 * Main alarm: shows the reminder (unless Airplane mode / DND is on) and queues the next one.
 * Nag alarm: re-alerts every 2 minutes, up to 3 times, until the user taps Done.
 *
 * The notification itself is silent; the app vibrates and plays the sound directly,
 * because many phone brands mute sound/vibration for sideloaded apps' notifications.
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
        show(c)
        if (n < Scheduler.MAX_NAGS) Scheduler.scheduleNag(c) else Prefs.setPendingAck(c, false)
    }

    companion object {
        const val NOTIFICATION_ID = 42

        private const val CHANNEL = "reminders_v3"   // silent channel; app does the alerting
        private val OLD_CHANNELS = listOf(
            "reminders", "alert_sound_vibrate", "alert_vibrate", "alert_sound"
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

        /** Vibrate and/or ring according to the chosen style and the phone's ringer mode. */
        fun alert(c: Context) {
            val mode = Prefs.alertMode(c)
            val ringer = c.getSystemService(AudioManager::class.java).ringerMode
            if (ringer == AudioManager.RINGER_MODE_SILENT) return  // phone is on Silent

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

            if (wantSound) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                RingtoneManager.getRingtone(c, uri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    play()
                }
            }
        }

        fun show(c: Context) {
            ensureChannel(c)
            val nm = c.getSystemService(NotificationManager::class.java)

            // One tap on "Done" (or on the notification, or swiping it away) stops the repeats.
            val donePi = PendingIntent.getBroadcast(
                c, 2002, Intent(c, DoneReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val n = Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_water)
                .setContentTitle("💧 Drink water  •  🧍 Sit up straight")
                .setContentText("Take a sip and fix your posture. Tap Done when finished.")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(donePi)
                .setDeleteIntent(donePi)
                .addAction(Notification.Action.Builder(null as Icon?, "Done ✓", donePi).build())
                .setTimeoutAfter(Prefs.intervalMinutes(c) * 60_000L - 10_000L) // auto-clear before next
                .build()

            nm.notify(NOTIFICATION_ID, n)
            alert(c)
        }
    }
}
