package com.anjan.hydrate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {

    private lateinit var toggle: Button
    private lateinit var status: TextView
    private lateinit var interval: TextView
    private lateinit var exact: Button

    private lateinit var repeat: Button
    private val alertButtons = mutableListOf<Pair<Int, Button>>()

    private val presetMinutes = listOf(15, 30, 45, 60, 90)
    private val alertOptions = listOf(
        Prefs.ALERT_SOUND to "🔔 Sound",
        Prefs.ALERT_VIBRATE to "📳 Vibrate",
        Prefs.ALERT_BOTH to "🔔📳 Both"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ReminderReceiver.ensureChannel(this)

        toggle = findViewById(R.id.toggle)
        status = findViewById(R.id.status)
        interval = findViewById(R.id.interval)
        exact = findViewById(R.id.exact)

        toggle.setOnClickListener { setEnabled(!Prefs.isEnabled(this)) }
        findViewById<Button>(R.id.minus).setOnClickListener { changeInterval(Prefs.intervalMinutes(this) - 5) }
        findViewById<Button>(R.id.plus).setOnClickListener { changeInterval(Prefs.intervalMinutes(this) + 5) }
        findViewById<Button>(R.id.test).setOnClickListener {
            askNotificationPermissionIfNeeded()
            ReminderReceiver.show(this)
        }
        exact.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                awaitingExactPermission = true
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")))
            }
        }

        val presets = findViewById<LinearLayout>(R.id.presets)
        presetMinutes.forEach { m ->
            val b = Button(this).apply {
                text = "$m"
                textSize = 16f
                setOnClickListener { changeInterval(m) }
            }
            presets.addView(b, LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val modes = findViewById<LinearLayout>(R.id.alertModes)
        alertOptions.forEach { (mode, label) ->
            val b = Button(this).apply {
                textSize = 14f
                isAllCaps = false
                setOnClickListener {
                    Prefs.setAlertMode(this@MainActivity, mode)
                    refresh()
                    ReminderReceiver.show(this@MainActivity)  // instant preview of the new style
                }
            }
            alertButtons += mode to b
            modes.addView(b, LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        repeat = findViewById(R.id.repeat)
        repeat.isAllCaps = false
        repeat.setOnClickListener {
            Prefs.setRepeatUntilDone(this, !Prefs.repeatUntilDone(this))
            refresh()
        }

        // First launch: switch on straight away so it works with zero setup clicks.
        if (!getPreferences(MODE_PRIVATE).getBoolean("launched", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("launched", true).apply()
            setEnabled(true)
        }
    }

    private var awaitingExactPermission = false

    override fun onResume() {
        super.onResume()
        if (Prefs.isEnabled(this)) {
            val missed = Prefs.nextAt(this) < System.currentTimeMillis()
            val exactJustAllowed = awaitingExactPermission && Scheduler.canUseExact(this)
            // Only restart the countdown when needed, so opening the app never delays a reminder.
            if (missed || exactJustAllowed) Scheduler.scheduleNext(this)
        }
        awaitingExactPermission = false
        refresh()
    }

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

        toggle.text = if (on) "ON  •  Tap to stop" else "OFF  •  Tap to start"
        toggle.background = GradientDrawable().apply {
            cornerRadius = 48f
            setColor(if (on) Color.parseColor("#1A9E6E") else Color.parseColor("#8A9BA8"))
        }

        interval.text = "$mins min"

        status.text = when {
            !on -> "Reminders are off"
            ReminderReceiver.isAirplaneModeOn(this) -> "Paused: Airplane mode is on"
            ReminderReceiver.isDndOn(this) -> "Paused: Do Not Disturb is on"
            else -> {
                val next = Prefs.nextAt(this)
                if (next > 0) "Next reminder at " +
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(next))
                else "Running"
            }
        }

        val current = Prefs.alertMode(this)
        alertButtons.forEach { (mode, b) ->
            val label = alertOptions.first { it.first == mode }.second
            b.text = if (mode == current) "✓ $label" else label
            b.background = GradientDrawable().apply {
                cornerRadius = 32f
                setColor(Color.parseColor(if (mode == current) "#BFE3F5" else "#E3EBF0"))
            }
        }

        repeat.text = if (Prefs.repeatUntilDone(this))
            "🔁 Repeat every 2 min until I tap Done: ON"
        else
            "🔁 Repeat until I tap Done: OFF"

        exact.visibility = if (on && !Scheduler.canUseExact(this)) View.VISIBLE else View.GONE
    }
}
