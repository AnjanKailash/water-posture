package com.anjan.hydrate

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class MainActivity : Activity() {

    // Palette
    private val navy = Color.parseColor("#0B2E4F")
    private val blue = Color.parseColor("#1468B8")
    private val sky = Color.parseColor("#2BC0E4")
    private val mist = Color.parseColor("#EEF5FB")
    private val muted = Color.parseColor("#6E8AA3")

    private lateinit var toggle: LinearLayout
    private lateinit var toggleState: TextView
    private lateinit var toggleHint: TextView
    private lateinit var toggleEmoji: TextView
    private lateinit var status: TextView
    private lateinit var countdown: TextView
    private lateinit var statDone: TextView
    private lateinit var statEvery: TextView
    private lateinit var interval: TextView
    private lateinit var exact: TextView
    private lateinit var startTimeView: TextView
    private lateinit var moveSummary: TextView
    private lateinit var moveToggle: TextView
    private lateinit var endTimeView: TextView

    private val presetButtons = mutableListOf<Pair<Int, TextView>>()
    private val alertButtons = mutableListOf<Pair<Int, TextView>>()

    private val presetMinutes = listOf(15, 30, 45, 60, 90)
    private val alertOptions = listOf(
        Triple(Prefs.ALERT_SOUND, "🔔", "Sound"),
        Triple(Prefs.ALERT_VIBRATE, "📳", "Vibrate"),
        Triple(Prefs.ALERT_BOTH, "🔔📳", "Both"),
        Triple(Prefs.ALERT_NONE, "🔕", "Notify only")
    )

    private val ticker = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { refresh(); ticker.postDelayed(this, 20_000L) }
    }

    private var awaitingExactPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0B3C7A")
        window.navigationBarColor = Color.parseColor("#2BC0E4")
        setContentView(R.layout.activity_main)
        ReminderReceiver.ensureChannel(this)

        toggle = findViewById(R.id.toggle)
        toggleState = findViewById(R.id.toggleState)
        toggleHint = findViewById(R.id.toggleHint)
        toggleEmoji = findViewById(R.id.toggleEmoji)
        status = findViewById(R.id.status)
        countdown = findViewById(R.id.countdown)
        statDone = findViewById(R.id.statDone)
        statEvery = findViewById(R.id.statEvery)
        interval = findViewById(R.id.interval)
        exact = findViewById(R.id.exact)

        findViewById<TextView>(R.id.greeting).text = greeting()

        statDone.background = rounded(mist, 18f)
        statEvery.background = rounded(mist, 18f)

        toggle.setOnClickListener {
            it.animate().scaleX(0.92f).scaleY(0.92f).setDuration(90).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }.start()
            setEnabled(!Prefs.isEnabled(this))
        }

        findViewById<TextView>(R.id.minus).apply {
            background = oval(mist)
            setOnClickListener { changeInterval(Prefs.intervalMinutes(this@MainActivity) - 5) }
        }
        findViewById<TextView>(R.id.plus).apply {
            background = oval(mist)
            setOnClickListener { changeInterval(Prefs.intervalMinutes(this@MainActivity) + 5) }
        }

        val presets = findViewById<LinearLayout>(R.id.presets)
        presetMinutes.forEachIndexed { i, m ->
            val chip = chip("$m").apply { setOnClickListener { changeInterval(m) } }
            presetButtons += m to chip
            presets.addView(chip, weighted(i, presetMinutes.size))
        }

        val modes = findViewById<LinearLayout>(R.id.alertModes)
        alertOptions.forEachIndexed { i, (mode, emoji, label) ->
            val chip = chip("$emoji\n$label").apply {
                textSize = 13f
                setPadding(0, dp(12), 0, dp(12))
                setOnClickListener {
                    Prefs.setAlertMode(this@MainActivity, mode)
                    refresh()
                }
            }
            alertButtons += mode to chip
            modes.addView(chip, weighted(i, alertOptions.size))
        }

        exact.background = rounded(Color.parseColor("#FFE7A8"), 22f)
        exact.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                awaitingExactPermission = true
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")))
            }
        }

        findViewById<TextView>(R.id.test).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(28).toFloat()
                setColor(Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), Color.parseColor("#88FFFFFF"))
            }
            setOnClickListener {
                askNotificationPermissionIfNeeded()
                if (Prefs.moveEnabled(this@MainActivity)) Recommender.pickNext(this@MainActivity)
                ReminderReceiver.show(this@MainActivity)
            }
        }

        val pillBg = { GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(blue, sky))
            .apply { cornerRadius = dp(18).toFloat() } }

        startTimeView = findViewById<TextView>(R.id.startTime).apply {
            background = pillBg()
            setOnClickListener { pickWindowTime(isStart = true) }
        }
        endTimeView = findViewById<TextView>(R.id.endTime).apply {
            background = pillBg()
            setOnClickListener { pickWindowTime(isStart = false) }
        }

        moveSummary = findViewById(R.id.moveSummary)
        moveToggle = findViewById<TextView>(R.id.moveToggle).apply {
            setOnClickListener {
                Prefs.setMoveEnabled(this@MainActivity, !Prefs.moveEnabled(this@MainActivity))
                refresh()
            }
        }
        findViewById<TextView>(R.id.moveBrowse).apply {
            background = pillBg()
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ActivitiesActivity::class.java))
            }
        }

        // First launch: switch on straight away so it works with zero setup clicks.
        if (!getPreferences(MODE_PRIVATE).getBoolean("launched", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("launched", true).apply()
            setEnabled(true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.isEnabled(this)) {
            val missed = Prefs.nextAt(this) < System.currentTimeMillis()
            val exactJustAllowed = awaitingExactPermission && Scheduler.canUseExact(this)
            // Only restart the countdown when needed, so opening the app never delays a reminder.
            if (missed || exactJustAllowed) Scheduler.scheduleNext(this)
        }
        awaitingExactPermission = false
        ticker.post(tick)
    }

    override fun onPause() {
        super.onPause()
        ticker.removeCallbacks(tick)
    }

    /** Change the daily start or end time; the timetable updates immediately. */
    private fun pickWindowTime(isStart: Boolean) {
        val current = if (isStart) Prefs.startMinutes(this) else Prefs.endMinutes(this)
        TimePickerDialog(this, { _, hour, minute ->
            val m = hour * 60 + minute
            if (isStart) Prefs.setStartMinutes(this, m) else Prefs.setEndMinutes(this, m)
            if (Prefs.isEnabled(this)) Scheduler.scheduleNext(this)
            refresh()
            toast("Next reminder ${dayAndTime(Prefs.nextAt(this))}")
        }, current / 60, current % 60,
            android.text.format.DateFormat.is24HourFormat(this)).show()
    }

    private fun minutesText(m: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, m / 60); set(Calendar.MINUTE, m % 60)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)
    }

    private fun timeText(t: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t))

    /** "at 9:30 AM" today, or "tomorrow at 9:00 AM". */
    private fun dayAndTime(t: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = t }
        val today = Calendar.getInstance()
        val sameDay = target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                      target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        return if (sameDay) "at ${timeText(t)}" else "tomorrow at ${timeText(t)}"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun setEnabled(on: Boolean) {
        Prefs.setEnabled(this, on)
        if (on) {
            askNotificationPermissionIfNeeded()
            Scheduler.scheduleNext(this)
        } else {
            Scheduler.cancel(this)
        }
        refresh()
    }

    private fun changeInterval(m: Int) {
        Prefs.setIntervalMinutes(this, m)
        if (Prefs.isEnabled(this)) Scheduler.scheduleNext(this)  // restart countdown
        refresh()
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun refresh() {
        val on = Prefs.isEnabled(this)
        val mins = Prefs.intervalMinutes(this)

        // Big circle
        toggle.background = if (on) GradientDrawable(
            GradientDrawable.Orientation.TL_BR, intArrayOf(sky, blue)
        ).apply { shape = GradientDrawable.OVAL } else oval(Color.parseColor("#E4ECF2"))
        toggleState.text = if (on) "ON" else "OFF"
        toggleState.setTextColor(if (on) Color.WHITE else muted)
        toggleHint.text = if (on) "tap to pause" else "tap to start"
        toggleHint.setTextColor(if (on) Color.parseColor("#D6F1FF") else muted)
        toggleEmoji.alpha = if (on) 1f else 0.45f

        // Status lines
        val next = Prefs.nextAt(this)
        when {
            !on -> { status.text = "Reminders are paused"; countdown.text = "Tap the circle to start" }
            ReminderReceiver.isAirplaneModeOn(this) -> {
                status.text = "✈️  Airplane mode is on"
                countdown.text = "Silent for now · next slot ${dayAndTime(next)}"
            }
            ReminderReceiver.isDndOn(this) -> {
                status.text = "🌙  Do Not Disturb is on"
                countdown.text = "Silent for now · next slot ${dayAndTime(next)}"
            }
            next > 0 -> {
                status.text = "Next " + dayAndTime(next)
                val left = ((next - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
                countdown.text = when {
                    left < 1 -> "any moment now"
                    left < 60 -> "in $left min"
                    else -> "in ${left / 60} h ${left % 60} min"
                }
            }
            else -> { status.text = "Running"; countdown.text = "" }
        }

        val done = Prefs.doneToday(this)
        statDone.text = "💧  $done done today"
        statEvery.text = "⏱  every $mins min"
        val moveOn = Prefs.moveEnabled(this)
        moveToggle.text = if (moveOn) "✓  On" else "Off"
        moveToggle.setTextColor(if (moveOn) Color.WHITE else navy)
        moveToggle.background = if (moveOn)
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(blue, sky))
                .apply { cornerRadius = dp(18).toFloat() }
        else rounded(mist, 18f)
        moveSummary.text = when {
            !moveOn -> "Water reminders only. Turn on to add a quick push-up, stretch, " +
                       "brain game or walk to each reminder."
            Prefs.moveMode(this) == Prefs.MOVE_SMART ->
                "✨ Smart picks — a healthy mix for the time of day.\n🍱 Walk suggested after lunch (" +
                minutesText(Prefs.lunchMinutes(this)) + ")."
            else -> "✋ My picks — ${Prefs.selectedMoves(this).size} activities chosen.\n" +
                    "🍱 Lunch at " + minutesText(Prefs.lunchMinutes(this)) + "."
        }
        startTimeView.text = "☀️  Starts\n" + minutesText(Prefs.startMinutes(this))
        endTimeView.text = "🌙  Ends\n" + minutesText(Prefs.endMinutes(this))

        interval.text = "$mins"

        presetButtons.forEach { (m, chip) -> styleChip(chip, m == mins) }
        val current = Prefs.alertMode(this)
        alertButtons.forEach { (mode, chip) -> styleChip(chip, mode == current) }

        exact.visibility = if (on && !Scheduler.canUseExact(this)) View.VISIBLE else View.GONE
    }

    // ---------- small UI helpers ----------

    private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning ☀️"
        in 12..16 -> "Good afternoon 🌤"
        in 17..21 -> "Good evening 🌆"
        else -> "Hello 🌙"
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply {
        cornerRadius = radiusDp * resources.displayMetrics.density
        setColor(color)
    }

    private fun oval(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun chip(text: String) = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        textSize = 15f
        setPadding(0, dp(10), 0, dp(10))
        isClickable = true
        isFocusable = true
    }

    private fun styleChip(chip: TextView, selected: Boolean) {
        chip.background = if (selected)
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(blue, sky))
                .apply { cornerRadius = dp(18).toFloat() }
        else rounded(mist, 18f)
        chip.setTextColor(if (selected) Color.WHITE else navy)
    }

    private fun weighted(index: Int, count: Int) =
        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            if (index > 0) marginStart = dp(4)
            if (index < count - 1) marginEnd = dp(4)
        }
}
