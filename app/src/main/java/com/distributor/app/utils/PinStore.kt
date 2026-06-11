package com.distributor.app.utils

import android.content.Context
import java.security.MessageDigest

object PinStore {
    private const val PREF      = "app_pin"
    private const val K_HASH    = "pin_hash"
    private const val K_ENABLED = "pin_enabled"

    fun isPinEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(K_ENABLED, false)

    fun setPin(ctx: Context, pin: String) {
        prefs(ctx).edit()
            .putString(K_HASH,     sha256(pin))
            .putBoolean(K_ENABLED, true)
            .apply()
    }

    fun verifyPin(ctx: Context, pin: String): Boolean {
        val stored = prefs(ctx).getString(K_HASH, null) ?: return false
        return sha256(pin) == stored
    }

    fun disablePin(ctx: Context) {
        prefs(ctx).edit()
            .remove(K_HASH)
            .putBoolean(K_ENABLED, false)
            .apply()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
