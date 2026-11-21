package com.anjan.hydrate

import android.content.Context

/** Tiny wrapper around SharedPreferences for the app's settings. */
object Prefs {
    private const val FILE = "hydrate_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL = "interval_minutes"
    private const val KEY_NEXT_AT = "next_at"
    private const val KEY_ALERT = "alert_mode"
    private const val KEY_REPEAT = "repeat_until_done"
    private const val KEY_PENDING = "pending_ack"
    private const val KEY_NAG_COUNT = "nag_count"

    const val DEFAULT_INTERVAL = 30
    const val MIN_INTERVAL = 5
    const val MAX_INTERVAL = 240

    // Alert modes
    const val ALERT_SOUND = 0
    const val ALERT_VIBRATE = 1
    const val ALERT_BOTH = 2

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = sp(c).getBoolean(KEY_ENABLED, false)
    fun setEnabled(c: Context, on: Boolean) = sp(c).edit().putBoolean(KEY_ENABLED, on).apply()

    fun intervalMinutes(c: Context) = sp(c).getInt(KEY_INTERVAL, DEFAULT_INTERVAL)
    fun setIntervalMinutes(c: Context, m: Int) =
        sp(c).edit().putInt(KEY_INTERVAL, m.coerceIn(MIN_INTERVAL, MAX_INTERVAL)).apply()

    fun nextAt(c: Context) = sp(c).getLong(KEY_NEXT_AT, 0L)
    fun setNextAt(c: Context, t: Long) = sp(c).edit().putLong(KEY_NEXT_AT, t).apply()

    fun alertMode(c: Context) = sp(c).getInt(KEY_ALERT, ALERT_BOTH)
    fun setAlertMode(c: Context, m: Int) = sp(c).edit().putInt(KEY_ALERT, m).apply()

    fun repeatUntilDone(c: Context) = sp(c).getBoolean(KEY_REPEAT, true)
    fun setRepeatUntilDone(c: Context, on: Boolean) = sp(c).edit().putBoolean(KEY_REPEAT, on).apply()

    /** True while a reminder is showing and the user hasn't tapped Done yet. */
    fun pendingAck(c: Context) = sp(c).getBoolean(KEY_PENDING, false)
    fun setPendingAck(c: Context, on: Boolean) = sp(c).edit().putBoolean(KEY_PENDING, on).apply()

    fun nagCount(c: Context) = sp(c).getInt(KEY_NAG_COUNT, 0)
    fun setNagCount(c: Context, n: Int) = sp(c).edit().putInt(KEY_NAG_COUNT, n).apply()
}
