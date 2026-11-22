package com.anjan.hydrate

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import java.text.DateFormat
import java.util.Calendar

/**
 * Activities page: choose Smart picks or My picks, set lunch time,
 * browse every activity (tap for how-to), and tick the ones you want.
 */
class ActivitiesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        build()
    }

    private fun build() {
        val col = Ui.page(this)
        col.addView(Ui.back(this))
        col.addView(Ui.text(this, "Activities", 30f, Color.WHITE, "sans-serif-black"))
        col.addView(Ui.text(this,
            "Each reminder pairs a glass of water with one quick activity.",
            14f, Ui.ON_BLUE), Ui.topMargin(this, 4))

        // On / off
        val onCard = Ui.card(this)
        val on = Prefs.moveEnabled(this)
        onCard.addView(Ui.label(this, "Suggest an activity with each reminder"))
        val onBtn = Ui.button(this, if (on) "✓  On — water + activity" else "Off — water reminders only", on)
        onBtn.setOnClickListener { Prefs.setMoveEnabled(this, !Prefs.moveEnabled(this)); build() }
        onCard.addView(onBtn, Ui.topMargin(this, 12))
        col.addView(onCard, Ui.topMargin(this, 18))

        if (!on) return

        // How to choose
        val mode = Prefs.moveMode(this)
        val modeCard = Ui.card(this)
        modeCard.addView(Ui.label(this, "How should we choose?"))
        modeCard.addView(modeTile("✨  Smart picks",
            "A healthy mix chosen for the time of day: stretches in the morning, a walk after lunch, " +
            "energy boosters in the afternoon, nothing heavy right after eating.",
            mode == Prefs.MOVE_SMART) { Prefs.setMoveMode(this, Prefs.MOVE_SMART); build() },
            Ui.topMargin(this, 12))
        modeCard.addView(modeTile("✋  My picks",
            "Only the activities you tick below. We'll still mix them up so it doesn't get boring.",
            mode == Prefs.MOVE_PICKS) { Prefs.setMoveMode(this, Prefs.MOVE_PICKS); build() },
            Ui.topMargin(this, 10))
        col.addView(modeCard, Ui.topMargin(this, 16))

        // Lunch time
        val lunchCard = Ui.card(this)
        lunchCard.addView(Ui.label(this, "Lunch time"))
        val lunch = Prefs.lunchMinutes(this)
        val lunchBtn = Ui.button(this, "🍱  Lunch at ${timeText(lunch)}   ✎", false)
        lunchBtn.setOnClickListener {
            TimePickerDialog(this, { _, h, m -> Prefs.setLunchMinutes(this, h * 60 + m); build() },
                lunch / 60, lunch % 60, android.text.format.DateFormat.is24HourFormat(this)).show()
        }
        lunchCard.addView(lunchBtn, Ui.topMargin(this, 12))
        lunchCard.addView(Ui.text(this,
            "The first reminder after lunch suggests a 15–30 minute walk.", 13f, Ui.MUTED),
            Ui.topMargin(this, 10))
        col.addView(lunchCard, Ui.topMargin(this, 16))

        // Library, grouped by category
        val picksMode = mode == Prefs.MOVE_PICKS
        val selected = Prefs.selectedMoves(this).toMutableSet()

        col.addView(Ui.text(this,
            if (picksMode) "Tick the activities you want. Tap a name to see how."
            else "All activities are in the mix. Tap one to see how it's done.",
            14f, Ui.ON_BLUE), Ui.topMargin(this, 22))

        if (picksMode) {
            val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val all = Ui.button(this, "Select all", false).apply {
                setOnClickListener {
                    Prefs.setSelectedMoves(this@ActivitiesActivity, Moves.all.map { it.id }.toSet()); build()
                }
            }
            val none = Ui.button(this, "Clear", false).apply {
                setOnClickListener { Prefs.setSelectedMoves(this@ActivitiesActivity, emptySet()); build() }
            }
            quick.addView(all, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = Ui.dp(this@ActivitiesActivity, 6) })
            quick.addView(none, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = Ui.dp(this@ActivitiesActivity, 6) })
            col.addView(quick, Ui.topMargin(this, 10))
        }

        Category.values().forEach { cat ->
            val moves = Moves.all.filter { it.category == cat }
            if (moves.isEmpty()) return@forEach
            val card = Ui.card(this)
            card.addView(Ui.text(this, "${cat.emoji}  ${cat.label}", 17f, Ui.NAVY, "sans-serif-black"))
            moves.forEach { m ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                if (picksMode) {
                    row.addView(CheckBox(this).apply {
                        isChecked = m.id in selected
                        setOnCheckedChangeListener { _, checked ->
                            if (checked) selected += m.id else selected -= m.id
                            Prefs.setSelectedMoves(this@ActivitiesActivity, selected)
                        }
                    })
                }
                val info = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(Ui.dp(context, 6), Ui.dp(context, 8), 0, Ui.dp(context, 8))
                    isClickable = true
                    setOnClickListener {
                        startActivity(Intent(this@ActivitiesActivity, MoveActivity::class.java)
                            .putExtra(MoveActivity.EXTRA_MOVE_ID, m.id))
                    }
                }
                info.addView(Ui.text(this, "${m.emoji}  ${m.name}", 16f, Ui.NAVY, "sans-serif-medium"))
                info.addView(Ui.text(this, m.summary, 13f, Ui.MUTED))
                row.addView(info, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(Ui.text(this, "›", 24f, Ui.MUTED).apply {
                    setPadding(Ui.dp(context, 8), 0, 0, 0)
                })
                card.addView(row, Ui.topMargin(this, 6))
            }
            col.addView(card, Ui.topMargin(this, 14))
        }
    }

    private fun modeTile(title: String, body: String, selected: Boolean, onTap: () -> Unit): View {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(context, 16), Ui.dp(context, 14), Ui.dp(context, 16), Ui.dp(context, 14))
            background = if (selected) Ui.gradient(context, 20) else Ui.rounded(context, Ui.MIST, 20)
            isClickable = true
            setOnClickListener { onTap() }
        }
        tile.addView(Ui.text(this, (if (selected) "● " else "○ ") + title, 16f,
            if (selected) Color.WHITE else Ui.NAVY, "sans-serif-medium"))
        tile.addView(Ui.text(this, body, 13f, if (selected) Ui.ON_BLUE else Ui.MUTED),
            Ui.topMargin(this, 4))
        return tile
    }

    private fun timeText(m: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, m / 60); set(Calendar.MINUTE, m % 60)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)
    }
}
