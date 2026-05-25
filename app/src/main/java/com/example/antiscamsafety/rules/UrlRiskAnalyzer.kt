package com.example.antiscamsafety.rules

import java.net.IDN
import java.net.URI
import java.util.Locale

class UrlRiskAnalyzer {
    fun analyze(rawTarget: String?): RiskResult {
        val normalized = normalize(rawTarget)
        val uri = parseUri(normalized)
        val host = uri?.host?.let { IDN.toUnicode(it).lowercase(Locale.US).trimEnd('.') }
        val hostAscii = host?.let { runCatching { IDN.toASCII(it) }.getOrDefault(it) }
        val haystack = listOfNotNull(normalized, host, uri?.path, uri?.query)
            .joinToString(" ")
            .lowercase(Locale.US)

        val reasons = mutableListOf<String>()
        var score = 0

        if (normalized.isBlank()) {
            return RiskResult(
                score = 0,
                level = RiskLevel.SAFE,
                normalizedTarget = "",
                host = null,
                reasons = listOf("沒有網址可檢查"),
                requiresPin = false,
                shouldBlock = false,
            )
        }

        if (host != null && RuleSet.isTrustedHost(host)) {
            reasons += "白名單網域"
            score -= 20
        }

        if (host != null && RuleSet.shortUrlHosts.contains(host)) {
            score += 25
            reasons += "短網址會隱藏真實目的地"
        }

        if (uri?.scheme != null && uri.scheme.lowercase(Locale.US) != "https") {
            score += 15
            reasons += "不是 HTTPS 連線"
        }

        val matchedKeywords = RuleSet.highRiskKeywords.filter { haystack.contains(it) }
        if (matchedKeywords.isNotEmpty()) {
            score += (matchedKeywords.size * 15).coerceAtMost(45)
            reasons += "含高風險字詞：${matchedKeywords.take(4).joinToString(", ")}"
        }

        if (RuleSet.apkExtensions.any { haystack.contains(it) }) {
            score += 50
            reasons += "疑似 APK / XAPK / APKS 下載連結"
        }

        if (hostAscii?.startsWith("xn--") == true || hostAscii?.contains(".xn--") == true) {
            score += 10
            reasons += "網域含國際化字元，需小心仿冒"
        }

        if (host != null && host.count { it == '-' } >= 3) {
            score += 10
            reasons += "網域使用大量連字號，可能是仿冒站"
        }

        if (host != null && looksLikeFinanceImpersonation(host, haystack)) {
            score += 25
            reasons += "疑似投資、交易所或客服仿冒頁"
        }

        val finalScore = score.coerceIn(0, 100)
        val level = RiskLevel.fromScore(finalScore)
        if (reasons.isEmpty()) reasons += "未命中本地高風險規則"

        return RiskResult(
            score = finalScore,
            level = level,
            normalizedTarget = normalized,
            host = host,
            reasons = reasons,
            requiresPin = finalScore >= 61,
            shouldBlock = finalScore >= 81,
        )
    }

    private fun normalize(rawTarget: String?): String {
        val trimmed = rawTarget.orEmpty().trim()
        if (trimmed.isBlank()) return ""
        val firstUrl = Regex("""https?://[^\s]+""").find(trimmed)?.value
        val candidate = firstUrl ?: trimmed
        return candidate.trim().trimEnd('。', '，', ',', ')', ']', '"', '\'')
    }

    private fun parseUri(value: String): URI? {
        if (value.isBlank()) return null
        return runCatching {
            val withScheme = if (value.contains("://")) value else "https://$value"
            URI(withScheme)
        }.getOrNull()
    }

    private fun looksLikeFinanceImpersonation(host: String, haystack: String): Boolean {
        val financeWords = listOf("invest", "trade", "exchange", "coin", "crypto", "wallet", "bank", "客服", "投資", "交易")
        val trustWords = listOf("verify", "login", "support", "service", "bonus", "vip", "認證", "客服")
        return financeWords.any { haystack.contains(it) } && trustWords.any { haystack.contains(it) } &&
            !RuleSet.isTrustedHost(host)
    }
}
