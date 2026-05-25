package com.example.antiscamsafety.rules

data class RiskResult(
    val score: Int,
    val level: RiskLevel,
    val normalizedTarget: String,
    val host: String?,
    val reasons: List<String>,
    val requiresPin: Boolean,
    val shouldBlock: Boolean,
)
