package com.example.antiscamsafety.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PinManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("family_pin", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setPin(pin: String): Boolean {
        if (!pin.matches(Regex("""\d{4,8}"""))) return false
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
        return true
    }

    fun verify(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("anti-scam-mvp:$pin").toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val KEY_PIN_HASH = "pin_hash"
    }
}
