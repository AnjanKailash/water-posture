package com.anjan.hydrate

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * How-to page for one activity: steps, an optional countdown timer, and
 * (when opened from a reminder) "Something else" and "Done" buttons.
 */
class MoveActivity : Activity() {

    companion object {
        const val EXTRA_MOVE_ID = "move_id"
        const val EXTRA_FROM_REMINDER = "from_reminder"
    }

    private var timer: CountDownTimer? = null
    private var fromReminder = false
    private lateinit var move: Move

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fromReminder = intent.getBooleanExtra(EXTRA_FROM_REMINDER, false)
        move = Moves.byId(intent.getStringExtra(EXTRA_MOVE_ID))
            ?: Recommender.current(this) ?: Moves.all.first()
        AlarmSound.stop()  // opening the how-to quiets the alarm
        build()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    private fun build() {
        timer?.cancel()
        val col = Ui.page(this)
        col.addView(Ui.back(this))

        if (fromReminder) {
            col.addView(Ui.text(this, "💧  First, have a few sips of water", 15f, Ui.ON_BLUE,
                "sans-serif-medium"))
        }

        // Hero card
        val hero = Ui.card(this).apply { gravity = Gravity.CENTER_HORIZONTAL }
        hero.addView(Ui.text(this, move.emoji, 64f, Ui.NAVY).apply { gravity = Gravity.CENTER })
        hero.addView(Ui.text(this, move.title(this), 26f, Ui.NAVY, "sans-serif-black").apply {
            gravity = Gravity.CENTER
        })
        hero.addView(Ui.text(this, "${move.category.emoji}  ${move.category.label}", 13f, Ui.BLUE,
            "sans-serif-medium").apply {
            background = Ui.rounded(this@MoveActivity, Ui.MIST, 14)
            setPadding(Ui.dp(context, 12), Ui.dp(context, 6), Ui.dp(context, 12), Ui.dp(context, 6))
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = Ui.dp(this@MoveActivity, 8) })
        hero.addView(Ui.text(this, move.summary, 15f, Ui.MUTED).apply { gravity = Gravity.CENTER },
            Ui.topMargin(this, 12))
        col.addView(hero, Ui.topMargin(this, 12))

        // Steps
        val steps = Ui.card(this)
        steps.addView(Ui.label(this, "How to do it"))
        move.steps.forEachIndexed { i, s ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(Ui.text(this, "${i + 1}", 14f, android.graphics.Color.WHITE,
                "sans-serif-black").apply {
                gravity = Gravity.CENTER
                background = Ui.gradient(this@MoveActivity, 14)
            }, LinearLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28)))
            row.addView(Ui.text(this, s, 15f, Ui.NAVY).apply {
                setPadding(Ui.dp(context, 12), Ui.dp(context, 3), 0, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            steps.addView(row, Ui.topMargin(this, 12))
        }
        col.addView(steps, Ui.topMargin(this, 16))

        // Benefits
        val why = Ui.card(this)
        why.addView(Ui.label(this, "Why it helps"))
        move.benefits.forEach { b ->
            why.addView(Ui.text(this, "✅  $b", 15f, Ui.NAVY), Ui.topMargin(this, 10))
        }
        col.addView(why, Ui.topMargin(this, 16))

        // When not to do it (+ personal notes)
        val notes = mutableListOf<String>()
        if (Profile.isSenior(this) && move.seniorCaution)
            notes += "Not recommended for 60+ — we leave it out of Smart picks. Choose a gentler option if unsure."
        notes += move.cautions
        if (notes.isNotEmpty()) {
            val warn = Ui.card(this)
            warn.background = Ui.rounded(this, android.graphics.Color.parseColor("#FFF4E0"), 28)
            warn.addView(Ui.label(this, "When not to do it"))
            notes.forEach { n ->
                warn.addView(Ui.text(this, "⚠️  $n", 15f, android.graphics.Color.parseColor("#5C3B00")),
                    Ui.topMargin(this, 10))
            }
            col.addView(warn, Ui.topMargin(this, 16))
        }

        if (Prefs.age(this) >= 40 && (move.reps > 0 || move.scaleSeconds)) {
            col.addView(Ui.text(this, "🎯  Reps and hold time are adjusted for your age.", 13f, Ui.ON_BLUE),
                Ui.topMargin(this, 12))
        }

        // Optional countdown timer
        val secs = move.timerSeconds(this)
        if (secs > 0) {
            val t = Ui.card(this).apply { gravity = Gravity.CENTER_HORIZONTAL }
            val clock = Ui.text(this, format(secs), 48f, Ui.NAVY, "sans-serif-black")
            val start = Ui.button(this, "▶  Start $secs-second timer", true)
            start.setOnClickListener {
                timer?.cancel()
                start.text = "↻  Restart timer"
                timer = object : CountDownTimer(secs * 1000L, 250L) {
                    override fun onTick(left: Long) { clock.text = format(((left + 999) / 1000).toInt()) }
                    override fun onFinish() { clock.text = "🎉 Done!"; buzz() }
                }.start()
            }
            t.addView(clock)
            t.addView(start, Ui.topMargin(this, 12))
            col.addView(t, Ui.topMargin(this, 16))
        }

        col.addView(Ui.text(this, "Go at your own pace and skip anything that causes pain.",
            12f, Ui.ON_BLUE).apply { gravity = Gravity.CENTER }, Ui.topMargin(this, 16))

        // Actions
        if (fromReminder) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val swap = Ui.button(this, "🔀  Something else", false).apply {
                setOnClickListener {
                    move = Recommender.pickNext(this@MoveActivity)
                    ReminderReceiver.show(this@MoveActivity, withAlert = false)
                    build()
                }
            }
            val done = Ui.button(this, "✓  Done", true).apply {
                setOnClickListener {
                    DoneReceiver.markDone(this@MoveActivity)
                    Toast.makeText(this@MoveActivity, "Nice work! 💪", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            row.addView(swap, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = Ui.dp(this@MoveActivity, 6) })
            row.addView(done, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = Ui.dp(this@MoveActivity, 6) })
            col.addView(row, Ui.topMargin(this, 20))
        } else {
            col.addView(Ui.button(this, "Close", true).apply { setOnClickListener { finish() } },
                Ui.topMargin(this, 20))
        }
    }

    private fun format(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

    private fun buzz() {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    }
}
