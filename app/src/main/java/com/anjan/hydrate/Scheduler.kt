package com.anjan.hydrate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Schedules one alarm at a time; each alarm schedules the next one. */
object Scheduler {

    private fun mainIntent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 1001,
            Intent(c, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    const val ACTION_SNOOZE_FIRE = "com.anjan.hydrate.SNOOZE_FIRE"
    const val SNOOZE_MINUTES = 5

    private fun snoozeIntent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 1003,
            Intent(c, ReminderReceiver::class.java).setAction(ACTION_SNOOZE_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Bring the reminder back in [SNOOZE_MINUTES] without touching the regular schedule. */
    fun scheduleSnooze(c: Context) {
        val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = snoozeIntent(c)
        if (canUseExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun cancelSnooze(c: Context) =
        c.getSystemService(AlarmManager::class.java).cancel(snoozeIntent(c))

    fun canUseExact(c: Context): Boolean {
        val am = c.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    const val PAUSED_CHECK_MINUTES = 5

    /** Schedule the next reminder [Prefs.intervalMinutes] from now. */
    fun scheduleNext(c: Context) =
        scheduleAt(c, System.currentTimeMillis() + Prefs.intervalMinutes(c) * 60_000L)

    /** While Airplane mode / DND is on, look again shortly to see if it has ended. */
    fun schedulePausedCheck(c: Context) =
        scheduleAt(c, System.currentTimeMillis() + PAUSED_CHECK_MINUTES * 60_000L)

    /** Schedule the next reminder at an exact moment (e.g. a time the user picked). */
    fun scheduleAt(c: Context, at: Long) {
        if (!Prefs.isEnabled(c)) return
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = mainIntent(c)
        if (canUseExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        Prefs.setNextAt(c, at)
    }

    fun cancel(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(mainIntent(c))
        cancelSnooze(c)
        Prefs.setNextAt(c, 0L)
    }
}
