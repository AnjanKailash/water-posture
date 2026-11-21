package com.anjan.hydrate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules one alarm at a time; each alarm schedules the next one.
 * Uses exact alarms when the user has allowed them, otherwise a
 * battery-friendly alarm that may arrive a few minutes late.
 */
object Scheduler {

    private fun pendingIntent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 1001,
            Intent(c, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun canUseExact(c: Context): Boolean {
        val am = c.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    /** Schedule the next reminder [Prefs.intervalMinutes] from now. */
    fun scheduleNext(c: Context) {
        if (!Prefs.isEnabled(c)) return
        val at = System.currentTimeMillis() + Prefs.intervalMinutes(c) * 60_000L
        val am = c.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(c)
        if (canUseExact(c)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
        Prefs.setNextAt(c, at)
    }

    fun cancel(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(pendingIntent(c))
        Prefs.setNextAt(c, 0L)
    }
}
