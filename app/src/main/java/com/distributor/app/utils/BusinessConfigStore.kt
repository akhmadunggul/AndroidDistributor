package com.distributor.app.utils

import android.content.Context
import android.content.SharedPreferences
import java.io.File

enum class HomeLayoutStyle { CARD, ICON_GRID }

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
    const val KEY_HOME_LAYOUT           = "home_layout"

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

    fun getHomeLayoutStyle(ctx: Context): HomeLayoutStyle {
        val name = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HOME_LAYOUT, HomeLayoutStyle.CARD.name) ?: ""
        return HomeLayoutStyle.entries.firstOrNull { it.name == name } ?: HomeLayoutStyle.CARD
    }

    fun saveHomeLayoutStyle(ctx: Context, style: HomeLayoutStyle) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_HOME_LAYOUT, style.name).apply()
    }

    fun registerLayoutListener(
        ctx: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)

    fun unregisterLayoutListener(
        ctx: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
}
