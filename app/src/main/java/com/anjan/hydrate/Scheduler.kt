package com.anjan.hydrate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

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

    /**
     * Daily timetable: every day, reminders fall on start, start + interval, start + 2×interval…
     * up to and including the end time. Slots during Airplane mode / DND are skipped,
     * so the timetable never drifts. If end is earlier than start, the window runs past midnight.
     */
    fun nextSlot(c: Context, from: Long): Long {
        val step = Prefs.intervalMinutes(c) * 60_000L
        val startMin = Prefs.startMinutes(c)
        var durationMin = (Prefs.endMinutes(c) - startMin + 1440) % 1440
        if (durationMin == 0) durationMin = 1440  // same start and end = all day
        val duration = durationMin * 60_000L

        var best = Long.MAX_VALUE
        for (dayOffset in -1..2) {
            val windowStart = Calendar.getInstance().apply {
                timeInMillis = from
                add(Calendar.DAY_OF_MONTH, dayOffset)
                set(Calendar.HOUR_OF_DAY, startMin / 60)
                set(Calendar.MINUTE, startMin % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val windowEnd = windowStart + duration
            val candidate = if (from < windowStart) windowStart else {
                val slot = windowStart + ((from - windowStart) / step + 1) * step
                if (slot <= windowEnd) slot else Long.MAX_VALUE
            }
            if (candidate < best) best = candidate
        }
        return best
    }

    /** Schedule the next slot on the timetable (30 s margin so a just-fired slot isn't reused). */
    fun scheduleNext(c: Context) =
        scheduleAt(c, nextSlot(c, System.currentTimeMillis() + 30_000L))

    private fun scheduleAt(c: Context, at: Long) {
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
