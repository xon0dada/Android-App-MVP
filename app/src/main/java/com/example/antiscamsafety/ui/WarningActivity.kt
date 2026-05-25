package com.example.antiscamsafety.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.antiscamsafety.rules.RiskLevel
import com.example.antiscamsafety.security.PinManager

class WarningActivity : Activity() {
    private lateinit var pinManager: PinManager
    private var target: String = ""
    private var level: RiskLevel = RiskLevel.SAFE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinManager = PinManager(this)
        target = intent.getStringExtra(EXTRA_TARGET).orEmpty()
        level = runCatching {
            RiskLevel.valueOf(intent.getStringExtra(EXTRA_LEVEL).orEmpty())
        }.getOrDefault(RiskLevel.SAFE)
        render()
    }

    private fun render() {
        val score = intent.getIntExtra(EXTRA_SCORE, 0)
        val reasons = intent.getStringArrayListExtra(EXTRA_REASONS).orEmpty()
        val color = UiKit.colorFor(level)
        val root = UiKit.verticalRoot(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(if (level == RiskLevel.SAFE) Color.rgb(237, 247, 237) else Color.rgb(255, 243, 224))
        }

        root.addView(UiKit.title(this, "風險：${UiKit.labelFor(level)}"))
        root.addView(TextView(this).apply {
            text = "$score"
            textSize = 76f
            setTextColor(color)
            gravity = Gravity.CENTER
        })
        root.addView(UiKit.body(this, scoreDescription(score)))
        root.addView(UiKit.body(this, "目標：\n${target.ifBlank { "未提供網址" }}"))
        root.addView(UiKit.body(this, reasons.joinToString("\n") { "• $it" }))

        val pinInput = EditText(this).apply {
            hint = "家人 PIN"
            textSize = 24f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            visibility = if (level.ordinal >= RiskLevel.HIGH_RISK.ordinal) android.view.View.VISIBLE else android.view.View.GONE
        }
        root.addView(pinInput)

        root.addView(UiKit.button(this, "安全離開", UiKit.GREEN).apply {
            setOnClickListener { finish() }
        })

        root.addView(UiKit.button(this, continueText(), if (level == RiskLevel.SAFE) UiKit.GREEN else UiKit.RED).apply {
            setOnClickListener {
                if (level.ordinal >= RiskLevel.HIGH_RISK.ordinal) {
                    if (!pinManager.hasPin()) {
                        Toast.makeText(this@WarningActivity, "請先請家人在主畫面設定 PIN", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    if (!pinManager.verify(pinInput.text?.toString().orEmpty())) {
                        Toast.makeText(this@WarningActivity, "PIN 不正確，已保護此操作", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                }
                continueToTarget()
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun continueToTarget() {
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            finish()
            return
        }
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        startActivity(Intent.createChooser(viewIntent, "選擇瀏覽器開啟"))
        finish()
    }

    private fun continueText(): String = when (level) {
        RiskLevel.SAFE -> "繼續開啟"
        RiskLevel.SUSPICIOUS -> "我了解，繼續"
        RiskLevel.HIGH_RISK,
        RiskLevel.BLOCKED -> "輸入 PIN 後繼續"
    }

    private fun scoreDescription(score: Int): String = when (score) {
        in 0..30 -> "目前看起來安全"
        in 31..60 -> "可疑，請先問家人"
        in 61..80 -> "高風險，需要家人 PIN"
        else -> "建議阻擋，不要繼續"
    }

    companion object {
        const val EXTRA_TARGET = "target"
        const val EXTRA_SCORE = "score"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_REASONS = "reasons"
    }
}
