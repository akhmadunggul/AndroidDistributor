package com.distributor.app.utils

import com.distributor.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// Ganti dengan URL deployment Apps Script Anda setelah deploy
const val LICENSE_API_URL = "https://script.google.com/macros/s/AKfycbxVceXNdZnYFcm7ShmwxltFh3Z8ImjsO8nJZ10nqKci8YAHWDFQnmnkJTeojt130jqCOQ/exec"

// Hari grace period offline sebelum muncul peringatan
const val OFFLINE_GRACE_DAYS = 14

data class LicensePackage(
    val id: String,
    val name: String,
    val price: Long,
    val durationDays: Int
)

data class PaymentOrder(
    val orderId: String,
    val qrString: String,
    val expiryMs: Long,
    val qrCodeUrl: String = "",
    val paymentType: String = "qris",
    val deeplinkUrl: String = ""
)

data class PaymentCheckResult(
    val status: String,
    val licenseStatus: String = "",
    val daysRemaining: Int = 0,
    val expiryDate: String = ""
)

data class LicenseResponse(
    val success: Boolean,
    val status: String      = "",
    val label: String       = "",
    val daysRemaining: Int  = 0,
    val expiryDate: String  = "",
    val error: String       = ""
)

sealed class LicenseState {
    object Idle                                                        : LicenseState()
    data class TrialWarning(val days: Int, val registered: Boolean = false) : LicenseState()
    data class Expired(val registered: Boolean = false)                : LicenseState()
    object Revoked                                                     : LicenseState()
    object OfflineWarning                                              : LicenseState()
}

object LicenseService {

    suspend fun activate(
        key: String, deviceId: String, deviceModel: String
    ): LicenseResponse? = post(JSONObject().apply {
        put("action",       "activate")
        put("key",          key)
        put("device_id",    deviceId)
        put("device_model", deviceModel)
        put("app_version",  BuildConfig.VERSION_NAME)
    })

    suspend fun check(key: String, deviceId: String): LicenseResponse? =
        post(JSONObject().apply {
            put("action",      "check")
            put("key",         key)
            put("device_id",   deviceId)
            put("app_version", BuildConfig.VERSION_NAME)
        })

    fun parseExpiryDate(isoStr: String): Long {
        if (isoStr.isBlank()) return 0L
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .also { it.timeZone = TimeZone.getTimeZone("UTC") }
                .parse(isoStr)?.time ?: 0L
        } catch (_: Exception) { 0L }
    }

    suspend fun fetchPackages(): List<LicensePackage>? {
        val (j, _) = postJsonWithRaw(JSONObject().apply { put("action", "getPackages") })
        if (j == null || !j.optBoolean("success", false)) return null
        val arr = j.optJSONArray("packages") ?: return null
        return (0 until arr.length()).map { i ->
            val pkg = arr.getJSONObject(i)
            LicensePackage(
                id           = pkg.getString("id"),
                name         = pkg.getString("name"),
                price        = pkg.getLong("price"),
                durationDays = pkg.getInt("duration_days")
            )
        }
    }

    suspend fun createPayment(
        packageId: String, amount: Long, deviceId: String, deviceModel: String,
        paymentType: String = "qris"
    ): PaymentOrder? {
        val (j, _) = postJsonWithRaw(JSONObject().apply {
            put("action",       "createPayment")
            put("package_id",   packageId)
            put("amount",       amount)
            put("device_id",    deviceId)
            put("device_model", deviceModel)
            put("payment_type", paymentType)
        })
        if (j == null || !j.optBoolean("success", false)) return null
        return PaymentOrder(
            orderId     = j.getString("order_id"),
            qrString    = j.optString("qr_string", ""),
            expiryMs    = j.getLong("expiry_ms"),
            qrCodeUrl   = j.optString("qr_image_url", ""),
            paymentType = j.optString("payment_type", paymentType),
            deeplinkUrl = j.optString("deeplink_url", "")
        )
    }

    suspend fun checkPayment(orderId: String, deviceId: String): PaymentCheckResult? {
        val (j, _) = postJsonWithRaw(JSONObject().apply {
            put("action",    "checkPayment")
            put("order_id",  orderId)
            put("device_id", deviceId)
        })
        if (j == null || !j.optBoolean("success", false)) return null
        return PaymentCheckResult(
            status        = j.optString("payment_status", "PENDING"),
            licenseStatus = j.optString("license_status", ""),
            daysRemaining = j.optInt("days_remaining", 0),
            expiryDate    = j.optString("expiry_date", "")
        )
    }

    suspend fun fetchContactWa(): String? {
        val (j, _) = postJsonWithRaw(JSONObject().apply { put("action", "getContactWa") })
        if (j == null || !j.optBoolean("success", false)) return null
        return j.optString("wa_number", null).takeIf { !it.isNullOrBlank() }
    }

    private suspend fun post(body: JSONObject): LicenseResponse? {
        val (j, _) = postJsonWithRaw(body)
        j ?: return null
        return LicenseResponse(
            success       = j.optBoolean("success", false),
            status        = j.optString("status", ""),
            label         = j.optString("label", ""),
            daysRemaining = j.optInt("days_remaining", 0),
            expiryDate    = j.optString("expiry_date", ""),
            error         = j.optString("error", "")
        )
    }

    // Hard 12-second deadline per request. Google Apps Script always issues an HTTP
    // redirect, and HttpURLConnection does NOT carry connectTimeout/readTimeout to the
    // redirected connection (known JDK bug), so the second leg can hang forever.
    // runInterruptible makes blocking I/O respond to thread interrupts, and withTimeout
    // fires the interrupt when the deadline is reached.
    private suspend fun postJsonWithRaw(body: JSONObject): Pair<JSONObject?, String?> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(12_000L) {
                    runInterruptible {
                        val conn = URL(LICENSE_API_URL).openConnection() as HttpURLConnection
                        conn.requestMethod           = "POST"
                        conn.connectTimeout          = 5_000
                        conn.readTimeout             = 8_000
                        conn.doOutput                = true
                        conn.instanceFollowRedirects = true
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.setRequestProperty("User-Agent",   "DistributorApp/1.0")
                        try {
                            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
                            val code = conn.responseCode
                            if (code != HttpURLConnection.HTTP_OK) {
                                conn.disconnect()
                                return@runInterruptible Pair(null, "HTTP $code")
                            }
                            val raw = conn.inputStream.bufferedReader().use { it.readText() }
                            conn.disconnect()
                            Pair(JSONObject(raw), raw)
                        } finally {
                            conn.disconnect()
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                Pair(null, "Timeout")
            } catch (e: Exception) {
                Pair(null, "${e.javaClass.simpleName}: ${e.message}")
            }
        }
}
