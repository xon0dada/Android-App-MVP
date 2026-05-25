package com.example.antiscamsafety.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.antiscamsafety.rules.RiskLevel

object UiKit {
    val GREEN = Color.rgb(27, 128, 68)
    val YELLOW = Color.rgb(245, 166, 35)
    val RED = Color.rgb(198, 40, 40)
    val DARK = Color.rgb(32, 33, 36)
    val LIGHT_BG = Color.rgb(250, 250, 250)

    fun verticalRoot(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(36, 44, 36, 36)
        setBackgroundColor(LIGHT_BG)
    }

    fun title(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 30f
        setTextColor(DARK)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        addBottomMargin(24)
    }

    fun body(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = 22f
        setTextColor(DARK)
        gravity = Gravity.CENTER
        setLineSpacing(8f, 1.0f)
        addBottomMargin(18)
    }

    fun button(context: Context, text: String, color: Int = DARK): Button = Button(context).apply {
        this.text = text
        textSize = 22f
        setTextColor(Color.WHITE)
        setBackgroundColor(color)
        minHeight = 88
        isAllCaps = false
        addBottomMargin(16)
    }

    fun colorFor(level: RiskLevel): Int = when (level) {
        RiskLevel.SAFE -> GREEN
        RiskLevel.SUSPICIOUS -> YELLOW
        RiskLevel.HIGH_RISK,
        RiskLevel.BLOCKED -> RED
    }

    fun labelFor(level: RiskLevel): String = when (level) {
        RiskLevel.SAFE -> "安全"
        RiskLevel.SUSPICIOUS -> "可疑"
        RiskLevel.HIGH_RISK -> "高風險"
        RiskLevel.BLOCKED -> "阻擋"
    }

    private fun View.addBottomMargin(bottom: Int) {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            setMargins(0, 0, 0, bottom)
        }
    }
}
