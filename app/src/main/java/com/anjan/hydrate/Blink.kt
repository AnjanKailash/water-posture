package com.anjan.hydrate

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Blink reminder: a gentle double buzz every N seconds until the user stops it.
 *
 * Each buzz is an alarm-clock alarm, which Android (and OnePlus battery management)
 * always delivers on time — even with the screen off and the phone asleep.
 * No notification needed; Android shows its small alarm icon while it's on.
 * Silent while Do Not Disturb or Airplane mode is on. Stops itself after 8 hours.
 */
object Blink {
    private const val MAX_RUN_MS = 8 * 60 * 60 * 1000L

    fun isRunning(c: Context) =
        Prefs.blinkOn(c) && System.currentTimeMillis() < Prefs.blinkUntil(c)

    fun start(c: Context) {
        c.getSystemService(NotificationManager::class.java).apply {
            deleteNotificationChannel("blink"); deleteNotificationChannel("blink_min")  // old versions
        }
        Prefs.setBlinkOn(c, true)
        Prefs.setBlinkUntil(c, System.currentTimeMillis() + MAX_RUN_MS)
        scheduleNext(c)
    }

    fun stop(c: Context) {
        Prefs.setBlinkOn(c, false)
        c.getSystemService(AlarmManager::class.java).cancel(pulseIntent(c))
        vibrator(c).cancel()
    }

    /** After a reboot, carry on if it was running. */
    fun resumeIfRunning(c: Context) { if (isRunning(c)) scheduleNext(c) }

    internal fun scheduleNext(c: Context) {
        val at = System.currentTimeMillis() + Prefs.blinkSeconds(c) * 1000L
        val am = c.getSystemService(AlarmManager::class.java)
        if (Scheduler.canUseExact(c)) {
            val showPi = PendingIntent.getActivity(
                c, 4002,
                Intent(c, MoveActivity::class.java)
                    .putExtra(MoveActivity.EXTRA_MOVE_ID, "eyes")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(at, showPi), pulseIntent(c))
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pulseIntent(c))
        }
    }

    private fun pulseIntent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 4001, Intent(c, BlinkReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun vibrator(c: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            c.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") c.getSystemService(Vibrator::class.java)

    /** One gentle double buzz, using the alarm channel so it works with the screen off. */
    internal fun pulse(c: Context) {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 90, 120, 90), -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator(c).vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator(c).vibrate(effect, attrs)
        }
    }
}

/** Fires every N seconds while blink mode is on: buzz, then set the next one. */
class BlinkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Blink.isRunning(context)) { Blink.stop(context); return }
        Blink.scheduleNext(context)  // queue the next buzz first
        val quiet = ReminderReceiver.isDndOn(context) || ReminderReceiver.isAirplaneModeOn(context)
        if (!quiet) Blink.pulse(context)
    }
}
