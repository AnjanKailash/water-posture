package com.anjan.hydrate

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Small shared helpers so every screen has the same look. */
object Ui {
    val NAVY = Color.parseColor("#0B2E4F")
    val BLUE = Color.parseColor("#1468B8")
    val SKY = Color.parseColor("#2BC0E4")
    val MIST = Color.parseColor("#EEF5FB")
    val MUTED = Color.parseColor("#6E8AA3")
    val ON_BLUE = Color.parseColor("#E6F4FF")

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun rounded(c: Context, color: Int, radiusDp: Int) = GradientDrawable().apply {
        cornerRadius = dp(c, radiusDp).toFloat(); setColor(color)
    }

    fun gradient(c: Context, radiusDp: Int) =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(BLUE, SKY)).apply {
            cornerRadius = dp(c, radiusDp).toFloat()
        }

    fun text(c: Context, s: CharSequence, sizeSp: Float, color: Int, font: String? = null) =
        TextView(c).apply {
            text = s; textSize = sizeSp; setTextColor(color)
            if (font != null) typeface = android.graphics.Typeface.create(font, android.graphics.Typeface.NORMAL)
        }

    fun label(c: Context, s: String) = text(c, s.uppercase(), 12f, MUTED, "sans-serif-medium").apply {
        letterSpacing = 0.12f
    }

    /** Full-screen gradient page; returns the column to add content to. */
    fun page(a: Activity): LinearLayout {
        a.window.statusBarColor = Color.parseColor("#0B3C7A")
        a.window.navigationBarColor = SKY
        val column = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 20), dp(a, 28), dp(a, 20), dp(a, 32))
        }
        val scroll = ScrollView(a).apply {
            setBackgroundResource(R.drawable.bg_main)
            isFillViewport = true
            addView(column)
        }
        a.setContentView(scroll)
        return column
    }

    fun back(a: Activity) = text(a, "‹  Back", 16f, Color.WHITE, "sans-serif-medium").apply {
        setPadding(0, dp(a, 4), dp(a, 16), dp(a, 12))
        setOnClickListener { a.finish() }
    }

    fun card(c: Context) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_card)
        elevation = dp(c, 6).toFloat()
        setPadding(dp(c, 20), dp(c, 20), dp(c, 20), dp(c, 20))
    }

    fun topMargin(c: Context, dpTop: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(c, dpTop) }

    /** Big rounded button: filled gradient or soft mist. */
    fun button(c: Context, s: String, filled: Boolean) = text(
        c, s, 16f, if (filled) Color.WHITE else NAVY, "sans-serif-medium"
    ).apply {
        gravity = Gravity.CENTER
        setPadding(dp(c, 12), dp(c, 15), dp(c, 12), dp(c, 15))
        background = if (filled) gradient(c, 26) else rounded(c, MIST, 26)
        isClickable = true; isFocusable = true
    }
}
