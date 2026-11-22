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
    const val ALERT_NONE = 3   // notification only: no sound, no vibration

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

    // ---- Activities ("move with each reminder") ----
    const val MOVE_SMART = 0
    const val MOVE_PICKS = 1

    fun moveEnabled(c: Context) = sp(c).getBoolean("move_on", true)
    fun setMoveEnabled(c: Context, on: Boolean) = sp(c).edit().putBoolean("move_on", on).apply()

    fun moveMode(c: Context) = sp(c).getInt("move_mode", MOVE_SMART)
    fun setMoveMode(c: Context, m: Int) = sp(c).edit().putInt("move_mode", m).apply()

    fun selectedMoves(c: Context): Set<String> =
        sp(c).getStringSet("move_picks", null)?.toSet() ?: Moves.all.map { it.id }.toSet()
    fun setSelectedMoves(c: Context, ids: Set<String>) =
        sp(c).edit().putStringSet("move_picks", HashSet(ids)).apply()

    fun lunchMinutes(c: Context) = sp(c).getInt("lunch_min", 13 * 60)
    fun setLunchMinutes(c: Context, m: Int) = sp(c).edit().putInt("lunch_min", m).apply()

    fun currentMove(c: Context): String? = sp(c).getString("move_current", null)
    fun setCurrentMove(c: Context, id: String) = sp(c).edit().putString("move_current", id).apply()

    fun recentMoves(c: Context): List<String> =
        (sp(c).getString("move_recent", "") ?: "").split(",").filter { it.isNotEmpty() }
    fun pushRecentMove(c: Context, id: String) =
        sp(c).edit().putString("move_recent", (recentMoves(c) + id).takeLast(5).joinToString(",")).apply()

    fun breakfastMinutes(c: Context) = sp(c).getInt("breakfast_min", 8 * 60 + 30)
    fun setBreakfastMinutes(c: Context, m: Int) = sp(c).edit().putInt("breakfast_min", m).apply()
    fun dinnerMinutes(c: Context) = sp(c).getInt("dinner_min", 20 * 60 + 30)
    fun setDinnerMinutes(c: Context, m: Int) = sp(c).edit().putInt("dinner_min", m).apply()

    fun mealWalkDay(c: Context, meal: String): String = sp(c).getString("walk_day_$meal", "") ?: ""
    fun setMealWalkDay(c: Context, meal: String, d: String) =
        sp(c).edit().putString("walk_day_$meal", d).apply()

    // ---- Blink reminder ----
    fun blinkSeconds(c: Context) = sp(c).getInt("blink_seconds", 20)
    fun setBlinkSeconds(c: Context, s: Int) = sp(c).edit().putInt("blink_seconds", s).apply()

    // ---- Optional profile (stored only on this phone) ----
    const val SEX_UNSET = 0
    const val SEX_MALE = 1
    const val SEX_FEMALE = 2
    const val SEX_OTHER = 3

    fun age(c: Context) = sp(c).getInt("age", 0)
    fun setAge(c: Context, v: Int) = sp(c).edit().putInt("age", v).apply()
    fun weightKg(c: Context) = sp(c).getInt("weight_kg", 0)
    fun setWeightKg(c: Context, v: Int) = sp(c).edit().putInt("weight_kg", v).apply()
    fun sex(c: Context) = sp(c).getInt("sex", SEX_UNSET)
    fun setSex(c: Context, v: Int) = sp(c).edit().putInt("sex", v).apply()

    // "Done" taps today, for the little progress counter on the main screen.
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun doneToday(c: Context): Int =
        if (sp(c).getString(KEY_DONE_DAY, "") == today()) sp(c).getInt(KEY_DONE_COUNT, 0) else 0

    fun incrementDoneToday(c: Context) {
        val n = doneToday(c) + 1
        sp(c).edit().putString(KEY_DONE_DAY, today()).putInt(KEY_DONE_COUNT, n).apply()
    }
}
