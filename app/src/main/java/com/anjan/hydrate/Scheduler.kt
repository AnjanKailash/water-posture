package com.anjan.hydrate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules one main alarm at a time; each alarm schedules the next one.
 * A second, short "nag" alarm re-alerts if the user hasn't tapped Done.
 */
object Scheduler {

    const val ACTION_NAG = "com.anjan.hydrate.NAG"
    const val NAG_DELAY_MS = 2 * 60_000L   // re-alert every 2 minutes
    const val MAX_NAGS = 3                 // at most 3 extra alerts

    private fun mainIntent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 1001,
            Intent(c, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nagIntent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 1002,
            Intent(c, ReminderReceiver::class.java).setAction(ACTION_NAG),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun canUseExact(c: Context): Boolean {
        val am = c.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    private fun setAlarm(c: Context, at: Long, pi: PendingIntent) {
        val am = c.getSystemService(AlarmManager::class.java)
        if (canUseExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** Schedule the next reminder [Prefs.intervalMinutes] from now. */
    fun scheduleNext(c: Context) {
        if (!Prefs.isEnabled(c)) return
        val at = System.currentTimeMillis() + Prefs.intervalMinutes(c) * 60_000L
        setAlarm(c, at, mainIntent(c))
        Prefs.setNextAt(c, at)
    }

    fun scheduleNag(c: Context) =
        setAlarm(c, System.currentTimeMillis() + NAG_DELAY_MS, nagIntent(c))

    fun cancelNag(c: Context) =
        c.getSystemService(AlarmManager::class.java).cancel(nagIntent(c))

    fun cancel(c: Context) {
        val am = c.getSystemService(AlarmManager::class.java)
        am.cancel(mainIntent(c))
        am.cancel(nagIntent(c))
        Prefs.setNextAt(c, 0L)
        Prefs.setPendingAck(c, false)
    }
}
