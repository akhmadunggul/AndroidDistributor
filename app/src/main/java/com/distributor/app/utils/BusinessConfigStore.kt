package com.distributor.app.utils

import android.content.Context
import java.io.File

data class BusinessConfig(
    val businessName: String = "",
    val ownerPhone: String = "",
    val address: String = ""
)

object BusinessConfigStore {
    private const val PREFS_NAME        = "distributor_prefs"
    private const val KEY_BUSINESS_NAME = "business_name"
    private const val KEY_OWNER_PHONE   = "owner_phone"
    private const val KEY_ADDRESS       = "address"
    private const val LOGO_FILE_NAME    = "business_logo.jpg"
    fun get(context: Context): BusinessConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return BusinessConfig(
            businessName = prefs.getString(KEY_BUSINESS_NAME, "") ?: "",
            ownerPhone   = prefs.getString(KEY_OWNER_PHONE,   "") ?: "",
            address      = prefs.getString(KEY_ADDRESS,       "") ?: ""
        )
    }

    fun save(context: Context, config: BusinessConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_BUSINESS_NAME, config.businessName)
            .putString(KEY_OWNER_PHONE,   config.ownerPhone)
            .putString(KEY_ADDRESS,       config.address)
            .apply()
    }

    fun getLogoFile(context: Context): File = File(context.filesDir, LOGO_FILE_NAME)
}
