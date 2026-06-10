package com.distributor.app.utils

import com.distributor.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Ganti dengan URL deployment Apps Script Anda setelah deploy
const val LICENSE_API_URL = "https://script.google.com/macros/s/AKfycbz8NmWor-KZSqSY0SajpkleF5nyH8uXnYKPAt-b3S21PL7Ja8gcFR6NakWN3hD4W676Cw/exec"

// Hari grace period offline sebelum muncul peringatan
const val OFFLINE_GRACE_DAYS = 14

data class LicenseResponse(
    val success: Boolean,
    val status: String      = "",
    val daysRemaining: Int  = 0,
    val expiryDate: String  = "",
    val error: String       = ""
)

sealed class LicenseState {
    object Idle                             : LicenseState()
    data class TrialWarning(val days: Int)  : LicenseState()
    object Expired                          : LicenseState()
    object Revoked                          : LicenseState()
    object OfflineWarning                   : LicenseState()
}

object LicenseService {

    suspend fun activate(
        key: String, deviceId: String, deviceModel: String
    ): LicenseResponse? = withContext(Dispatchers.IO) {
        post(JSONObject().apply {
            put("action",       "activate")
            put("key",          key)
            put("device_id",    deviceId)
            put("device_model", deviceModel)
            put("app_version",  BuildConfig.VERSION_NAME)
        })
    }

    suspend fun check(key: String, deviceId: String): LicenseResponse? =
        withContext(Dispatchers.IO) {
            post(JSONObject().apply {
                put("action",      "check")
                put("key",         key)
                put("device_id",   deviceId)
                put("app_version", BuildConfig.VERSION_NAME)
            })
        }

    fun parseExpiryDate(isoStr: String): Long {
        if (isoStr.isBlank()) return 0L
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .also { it.timeZone = TimeZone.getTimeZone("UTC") }
                .parse(isoStr)?.time ?: 0L
        } catch (_: Exception) { 0L }
    }

    private fun post(body: JSONObject): LicenseResponse? {
        return try {
            val conn = URL(LICENSE_API_URL).openConnection() as HttpURLConnection
            conn.requestMethod           = "POST"
            conn.connectTimeout          = 8_000
            conn.readTimeout             = 8_000
            conn.doOutput                = true
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent",   "DistributorApp/1.0")
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) { conn.disconnect(); return null }
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val j = JSONObject(raw)
            LicenseResponse(
                success       = j.optBoolean("success", false),
                status        = j.optString("status", ""),
                daysRemaining = j.optInt("days_remaining", 0),
                expiryDate    = j.optString("expiry_date", ""),
                error         = j.optString("error", "")
            )
        } catch (_: Exception) { null }
    }
}
