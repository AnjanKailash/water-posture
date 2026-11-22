package com.anjan.hydrate

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Tiny wrapper around SharedPreferences for the app's settings. */
object Prefs {
    private const val FILE = "hydrate_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_INTERVAL = "interval_minutes"
    private const val KEY_NEXT_AT = "next_at"
    private const val KEY_ALERT = "alert_mode"
    private const val KEY_DONE_DAY = "done_day"
    private const val KEY_DONE_COUNT = "done_count"

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

    /** Daily window, in minutes after midnight. Default 9:00 AM – 9:00 PM. */
    fun startMinutes(c: Context) = sp(c).getInt("start_min", 9 * 60)
    fun setStartMinutes(c: Context, m: Int) = sp(c).edit().putInt("start_min", m).apply()
    fun endMinutes(c: Context) = sp(c).getInt("end_min", 21 * 60)
    fun setEndMinutes(c: Context, m: Int) = sp(c).edit().putInt("end_min", m).apply()

    fun alertMode(c: Context) = sp(c).getInt(KEY_ALERT, ALERT_BOTH)
    fun setAlertMode(c: Context, m: Int) = sp(c).edit().putInt(KEY_ALERT, m).apply()

    // "Done" taps today, for the little progress counter on the main screen.
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun doneToday(c: Context): Int =
        if (sp(c).getString(KEY_DONE_DAY, "") == today()) sp(c).getInt(KEY_DONE_COUNT, 0) else 0

    fun incrementDoneToday(c: Context) {
        val n = doneToday(c) + 1
        sp(c).edit().putString(KEY_DONE_DAY, today()).putInt(KEY_DONE_COUNT, n).apply()
    }
}
