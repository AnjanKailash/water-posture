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
            "We recommend what's best for you: stretches in the morning, a walk after lunch and dinner, " +
            "energy boosters in the afternoon, nothing strenuous right after meals, and gentler options " +
            "if you're 60+.",
            mode == Prefs.MOVE_SMART) { Prefs.setMoveMode(this, Prefs.MOVE_SMART); build() },
            Ui.topMargin(this, 12))
        modeCard.addView(modeTile("✋  My picks",
            "You choose. Only the activities you tick below — we'll still mix them up and keep " +
            "strenuous ones away from mealtimes.",
            mode == Prefs.MOVE_PICKS) { Prefs.setMoveMode(this, Prefs.MOVE_PICKS); build() },
            Ui.topMargin(this, 10))
        col.addView(modeCard, Ui.topMargin(this, 16))

        // About you
        val me = Ui.card(this)
        me.addView(Ui.label(this, "About you (optional)"))
        me.addView(Ui.text(this, profileSummary(), 15f, Ui.NAVY), Ui.topMargin(this, 10))
        me.addView(Ui.button(this, if (Profile.hasProfile(this)) "✎  Edit" else "Add age, weight & sex", false)
            .apply {
                setOnClickListener {
                    startActivity(Intent(this@ActivitiesActivity, ProfileActivity::class.java))
                }
            }, Ui.topMargin(this, 12))
        col.addView(me, Ui.topMargin(this, 16))

        // Meal times
        val mealCard = Ui.card(this)
        mealCard.addView(Ui.label(this, "Meal times"))
        mealCard.addView(Ui.text(this,
            "No strenuous moves for an hour after eating. After lunch and dinner we suggest a 15–30 min walk.",
            13f, Ui.MUTED), Ui.topMargin(this, 8))
        listOf(
            Triple("🍳  Breakfast", Prefs.breakfastMinutes(this)) { m: Int -> Prefs.setBreakfastMinutes(this, m) },
            Triple("🍱  Lunch", Prefs.lunchMinutes(this)) { m: Int -> Prefs.setLunchMinutes(this, m) },
            Triple("🍽  Dinner", Prefs.dinnerMinutes(this)) { m: Int -> Prefs.setDinnerMinutes(this, m) }
        ).forEach { (label, value, save) ->
            val btn = Ui.button(this, "$label  ·  ${timeText(value)}   ✎", false)
            btn.setOnClickListener {
                TimePickerDialog(this, { _, h, mm -> save(h * 60 + mm); build() },
                    value / 60, value % 60, android.text.format.DateFormat.is24HourFormat(this)).show()
            }
            mealCard.addView(btn, Ui.topMargin(this, 10))
        }
        col.addView(mealCard, Ui.topMargin(this, 16))

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
                info.addView(Ui.text(this, "${m.emoji}  ${m.title(this@ActivitiesActivity)}", 16f, Ui.NAVY, "sans-serif-medium"))
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

    private fun profileSummary(): String {
        if (!Profile.hasProfile(this))
            return "Tell us a little about you and we'll adjust reps, skip moves that aren't suitable, " +
                   "and suggest how much to drink."
        val parts = mutableListOf<String>()
        if (Prefs.age(this) > 0) parts += "${Prefs.age(this)} yrs"
        if (Prefs.weightKg(this) > 0) parts += "${Prefs.weightKg(this)} kg"
        when (Prefs.sex(this)) {
            Prefs.SEX_MALE -> parts += "Male"
            Prefs.SEX_FEMALE -> parts += "Female"
            Prefs.SEX_OTHER -> parts += "Prefer not to say"
        }
        return "👤  " + parts.joinToString("  ·  ") +
               "\n💧  About ${Profile.dailyWaterMl(this) / 100 / 10.0} L a day → ~${Profile.mlPerReminder(this)} ml per reminder"
    }

    override fun onResume() {
        super.onResume()
        build()  // refresh after editing the profile
    }

    private fun timeText(m: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, m / 60); set(Calendar.MINUTE, m % 60)
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)
    }
}
