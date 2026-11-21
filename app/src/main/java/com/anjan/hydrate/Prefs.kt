package com.anjan.hydrate

import android.content.Context

/** Tiny wrapper around SharedPreferences for the two settings the app needs. */
object Prefs {
    private const val FILE = "hydrate_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL = "interval_minutes"
    private const val KEY_NEXT_AT = "next_at"

    const val DEFAULT_INTERVAL = 30
    const val MIN_INTERVAL = 5
    const val MAX_INTERVAL = 240

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = sp(c).getBoolean(KEY_ENABLED, false)
    fun setEnabled(c: Context, on: Boolean) = sp(c).edit().putBoolean(KEY_ENABLED, on).apply()

    fun intervalMinutes(c: Context) = sp(c).getInt(KEY_INTERVAL, DEFAULT_INTERVAL)
    fun setIntervalMinutes(c: Context, m: Int) =
        sp(c).edit().putInt(KEY_INTERVAL, m.coerceIn(MIN_INTERVAL, MAX_INTERVAL)).apply()

    fun nextAt(c: Context) = sp(c).getLong(KEY_NEXT_AT, 0L)
    fun setNextAt(c: Context, t: Long) = sp(c).edit().putLong(KEY_NEXT_AT, t).apply()
}
