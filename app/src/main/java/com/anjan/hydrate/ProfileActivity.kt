package com.anjan.hydrate

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Optional "About you" page: age, weight and sex. Stored only on this phone. */
class ProfileActivity : Activity() {

    private var sex = Prefs.SEX_UNSET
    private val sexButtons = mutableListOf<Pair<Int, TextView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sex = Prefs.sex(this)

        val col = Ui.page(this)
        col.addView(Ui.back(this))
        col.addView(Ui.text(this, "About you", 30f, Color.WHITE, "sans-serif-black"))
        col.addView(Ui.text(this,
            "Optional. Used to adjust reps, skip moves that may not suit you, and suggest how much to drink.",
            14f, Ui.ON_BLUE), Ui.topMargin(this, 4))

        val card = Ui.card(this)

        card.addView(Ui.label(this, "Age"))
        val age = numberField(if (Prefs.age(this) > 0) Prefs.age(this).toString() else "", "e.g. 34")
        card.addView(age, Ui.topMargin(this, 8))

        card.addView(Ui.label(this, "Weight (kg)"), Ui.topMargin(this, 18))
        val weight = numberField(if (Prefs.weightKg(this) > 0) Prefs.weightKg(this).toString() else "", "e.g. 70")
        card.addView(weight, Ui.topMargin(this, 8))

        card.addView(Ui.label(this, "Sex"), Ui.topMargin(this, 18))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(Prefs.SEX_MALE to "Male", Prefs.SEX_FEMALE to "Female", Prefs.SEX_OTHER to "Prefer not\nto say")
            .forEachIndexed { i, (value, label) ->
                val chip = Ui.text(this, label, 14f, Ui.NAVY, "sans-serif-medium").apply {
                    gravity = Gravity.CENTER
                    setPadding(0, Ui.dp(context, 12), 0, Ui.dp(context, 12))
                    isClickable = true
                    setOnClickListener { sex = value; styleSex() }
                }
                sexButtons += value to chip
                row.addView(chip, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (i > 0) marginStart = Ui.dp(this@ProfileActivity, 4)
                    if (i < 2) marginEnd = Ui.dp(this@ProfileActivity, 4)
                })
            }
        card.addView(row, Ui.topMargin(this, 8))
        styleSex()
        col.addView(card, Ui.topMargin(this, 18))

        col.addView(Ui.text(this,
            "🔒  Stays on this phone — never uploaded or shared.\n" +
            "ℹ️  General wellness guidance, not medical advice. If you have a health condition, " +
            "are pregnant, or have been told to limit fluids, follow your doctor's advice.",
            12f, Ui.ON_BLUE), Ui.topMargin(this, 14))

        col.addView(Ui.button(this, "Save", true).apply {
            setOnClickListener {
                val a = age.text.toString().toIntOrNull() ?: 0
                val w = weight.text.toString().toIntOrNull() ?: 0
                if (a != 0 && a !in 10..110) { toast("Please enter an age between 10 and 110"); return@setOnClickListener }
                if (w != 0 && w !in 25..250) { toast("Please enter a weight between 25 and 250 kg"); return@setOnClickListener }
                Prefs.setAge(this@ProfileActivity, a)
                Prefs.setWeightKg(this@ProfileActivity, w)
                Prefs.setSex(this@ProfileActivity, sex)
                toast("Saved — about ${Profile.mlPerReminder(this@ProfileActivity)} ml per reminder")
                finish()
            }
        }, Ui.topMargin(this, 18))

        col.addView(Ui.button(this, "Clear my details", false).apply {
            setOnClickListener {
                Prefs.setAge(this@ProfileActivity, 0)
                Prefs.setWeightKg(this@ProfileActivity, 0)
                Prefs.setSex(this@ProfileActivity, Prefs.SEX_UNSET)
                toast("Cleared")
                finish()
            }
        }, Ui.topMargin(this, 10))
    }

    private fun numberField(value: String, hint: String) = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        setText(value)
        this.hint = hint
        textSize = 18f
        setTextColor(Ui.NAVY)
        background = Ui.rounded(this@ProfileActivity, Ui.MIST, 16)
        setPadding(Ui.dp(context, 16), Ui.dp(context, 12), Ui.dp(context, 16), Ui.dp(context, 12))
    }

    private fun styleSex() {
        sexButtons.forEach { (value, chip) ->
            val on = value == sex
            chip.background = if (on) Ui.gradient(this, 18) else Ui.rounded(this, Ui.MIST, 18)
            chip.setTextColor(if (on) Color.WHITE else Ui.NAVY)
        }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
