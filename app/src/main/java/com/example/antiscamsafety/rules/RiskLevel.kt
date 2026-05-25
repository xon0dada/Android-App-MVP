package com.example.antiscamsafety.rules

enum class RiskLevel {
    SAFE,
    SUSPICIOUS,
    HIGH_RISK,
    BLOCKED;

    companion object {
        fun fromScore(score: Int): RiskLevel = when (score.coerceIn(0, 100)) {
            in 0..30 -> SAFE
            in 31..60 -> SUSPICIOUS
            in 61..80 -> HIGH_RISK
            else -> BLOCKED
        }
    }
}
