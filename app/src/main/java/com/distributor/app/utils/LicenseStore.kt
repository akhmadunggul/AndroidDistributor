package com.distributor.app.utils

import android.content.Context
import android.provider.Settings

object LicenseStore {
    private const val PREF         = "dist_license"
    private const val K_KEY        = "key"
    private const val K_STATUS     = "status"
    private const val K_EXPIRY_MS  = "expiry_ms"
    private const val K_DAYS       = "days"
    private const val K_LAST_CHECK = "last_check_ms"
    private const val K_CONTACT_WA = "contact_wa"

    fun getDeviceId(ctx: Context): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

    fun getLicenseKey(ctx: Context): String = prefs(ctx).getString(K_KEY, "") ?: ""
    fun isActivated(ctx: Context): Boolean  = getLicenseKey(ctx).isNotBlank()
    fun getStatus(ctx: Context): String     = prefs(ctx).getString(K_STATUS, "") ?: ""
    fun getDaysRemaining(ctx: Context): Int = prefs(ctx).getInt(K_DAYS, 0)
    fun getExpiryMs(ctx: Context): Long     = prefs(ctx).getLong(K_EXPIRY_MS, 0L)
    fun getLastCheckMs(ctx: Context): Long  = prefs(ctx).getLong(K_LAST_CHECK, 0L)

    fun save(ctx: Context, key: String, status: String, expiryMs: Long, days: Int) {
        prefs(ctx).edit()
            .putString(K_KEY,       key)
            .putString(K_STATUS,    status)
            .putLong(K_EXPIRY_MS,   expiryMs)
            .putInt(K_DAYS,         days)
            .putLong(K_LAST_CHECK,  System.currentTimeMillis())
            .apply()
    }

    fun updateCheck(ctx: Context, status: String, expiryMs: Long, days: Int) {
        prefs(ctx).edit()
            .putString(K_STATUS,   status)
            .putLong(K_EXPIRY_MS,  expiryMs)
            .putInt(K_DAYS,        days)
            .putLong(K_LAST_CHECK, System.currentTimeMillis())
            .apply()
    }

    fun getContactWa(ctx: Context): String? =
        prefs(ctx).getString(K_CONTACT_WA, null).takeIf { !it.isNullOrBlank() }

    fun saveContactWa(ctx: Context, number: String) {
        prefs(ctx).edit().putString(K_CONTACT_WA, number).apply()
    }

    fun clear(ctx: Context) = prefs(ctx).edit().clear().apply()

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}
