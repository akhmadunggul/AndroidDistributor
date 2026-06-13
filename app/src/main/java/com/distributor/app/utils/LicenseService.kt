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
const val LICENSE_API_URL = "https://script.google.com/macros/s/AKfycbwVFREcQSE1FDRT1QHSEZQp-3Qv5VvE_rcd5zr9Avv2gfOJ1g6f40Qv24D0q13zPHeuTA/exec"

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

    var lastWaFetchDebug: String = ""

    suspend fun fetchContactWa(): String? = withContext(Dispatchers.IO) {
        val (j, raw) = postJsonWithRaw(JSONObject().apply { put("action", "getContactWa") })
        lastWaFetchDebug = raw ?: "null response (network/HTTP error)"
        if (j == null) return@withContext null
        if (!j.optBoolean("success", false)) {
            lastWaFetchDebug = "success=false · error=${j.optString("error")} · raw=$raw"
            return@withContext null
        }
        j.optString("wa_number", null).takeIf { !it.isNullOrBlank() }
    }

    private fun post(body: JSONObject): LicenseResponse? {
        val (j, _) = postJsonWithRaw(body)
        j ?: return null
        return LicenseResponse(
            success       = j.optBoolean("success", false),
            status        = j.optString("status", ""),
            daysRemaining = j.optInt("days_remaining", 0),
            expiryDate    = j.optString("expiry_date", ""),
            error         = j.optString("error", "")
        )
    }

    private fun postJsonWithRaw(body: JSONObject): Pair<JSONObject?, String?> {
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
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect()
                return Pair(null, "HTTP $code")
            }
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            Pair(JSONObject(raw), raw)
        } catch (e: Exception) {
            Pair(null, "${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
