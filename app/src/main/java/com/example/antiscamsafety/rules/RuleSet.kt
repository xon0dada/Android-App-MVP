package com.example.antiscamsafety.rules

object RuleSet {
    val trustedDomainSuffixes = setOf(
        "google.com",
        "google.com.tw",
        "gstatic.com",
        "youtube.com",
        "youtu.be",
        "line.me",
        "line.naver.jp",
        "gov.tw",
        "moi.gov.tw",
        "police.gov.tw",
        "165.gov.tw",
        "nta.gov.tw",
        "fisc.com.tw",
        "bot.com.tw",
        "landbank.com.tw",
        "tcb-bank.com.tw",
        "firstbank.com.tw",
        "huanans.com.tw",
        "changhuabank.com",
        "megabank.com.tw",
        "ctbcbank.com",
        "cathaybk.com.tw",
        "esunbank.com",
        "taishinbank.com.tw",
        "sinopac.com",
        "fubon.com",
        "momo.com.tw",
        "momoshop.com.tw",
        "pchome.com.tw",
        "24h.pchome.com.tw",
    )

    val shortUrlHosts = setOf(
        "bit.ly",
        "tinyurl.com",
        "goo.gl",
        "is.gd",
        "reurl.cc",
        "lihi.cc",
        "ppt.cc",
        "cutt.ly",
        "t.co",
        "ow.ly",
        "shorturl.at",
    )

    val highRiskKeywords = setOf(
        "airdrop",
        "claim",
        "wallet",
        "verify",
        "bonus",
        "investment",
        "teacher",
        "line group",
        "line-group",
        "line_group",
        "seed phrase",
        "seed-phrase",
        "seed_phrase",
        "private key",
        "private-key",
        "private_key",
        "crypto",
        "exchange",
        "metamask",
        "usdt",
        "bonus",
        "vip",
        "客服",
        "投資",
        "老師",
        "空投",
        "錢包",
        "認證",
        "交易所",
    )

    val apkExtensions = setOf(".apk", ".xapk", ".apks")

    fun isTrustedHost(host: String): Boolean {
        val value = host.lowercase().trimEnd('.')
        return trustedDomainSuffixes.any { value == it || value.endsWith(".$it") }
    }
}
