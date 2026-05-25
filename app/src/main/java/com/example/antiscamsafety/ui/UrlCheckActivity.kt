package com.example.antiscamsafety.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.antiscamsafety.rules.UrlRiskAnalyzer

class UrlCheckActivity : Activity() {
    private val analyzer = UrlRiskAnalyzer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = extractTarget(intent)
        val result = analyzer.analyze(target)
        val warningIntent = Intent(this, WarningActivity::class.java)
            .putExtra(WarningActivity.EXTRA_TARGET, result.normalizedTarget)
            .putExtra(WarningActivity.EXTRA_SCORE, result.score)
            .putExtra(WarningActivity.EXTRA_LEVEL, result.level.name)
            .putStringArrayListExtra(WarningActivity.EXTRA_REASONS, ArrayList(result.reasons))
        startActivity(warningIntent)
        finish()
    }

    private fun extractTarget(intent: Intent): String {
        return when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            Intent.ACTION_VIEW -> intent.dataString.orEmpty()
            else -> intent.dataString.orEmpty()
        }
    }
}
