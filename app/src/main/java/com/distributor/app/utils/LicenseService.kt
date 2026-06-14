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
const val LICENSE_API_URL = "https://script.google.com/macros/s/AKfycbyYbI__DWC0i9KPvTK2Qcpf-9Eu3DQmwTTXpIK7rX1dmT8FHpjHTWphfKCauAMsRtGHOA/exec"

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

    var lastPackagesFetchDebug: String = ""

    suspend fun fetchPackages(): List<LicensePackage>? = withContext(Dispatchers.IO) {
        val (j, raw) = postJsonWithRaw(JSONObject().apply { put("action", "getPackages") })
        lastPackagesFetchDebug = raw ?: "null response (network/HTTP error)"
        if (j == null) return@withContext null
        if (!j.optBoolean("success", false)) {
            lastPackagesFetchDebug = "success=false · error=${j.optString("error")} · raw=$raw"
            return@withContext null
        }
        val arr = j.optJSONArray("packages") ?: return@withContext null
        (0 until arr.length()).map { i ->
            val pkg = arr.getJSONObject(i)
            LicensePackage(
                id           = pkg.getString("id"),
                name         = pkg.getString("name"),
                price        = pkg.getLong("price"),
                durationDays = pkg.getInt("duration_days")
            )
        }
    }

    var lastCreatePaymentDebug: String = ""

    suspend fun createPayment(
        packageId: String, amount: Long, deviceId: String, deviceModel: String,
        paymentType: String = "qris"
    ): PaymentOrder? = withContext(Dispatchers.IO) {
        val (j, raw) = postJsonWithRaw(JSONObject().apply {
            put("action",       "createPayment")
            put("package_id",   packageId)
            put("amount",       amount)
            put("device_id",    deviceId)
            put("device_model", deviceModel)
            put("payment_type", paymentType)
        })
        lastCreatePaymentDebug = raw ?: "null response (network/HTTP error)"
        if (j == null) return@withContext null
        if (!j.optBoolean("success", false)) {
            lastCreatePaymentDebug = "success=false · error=${j.optString("error")} · raw=$raw"
            return@withContext null
        }
        PaymentOrder(
            orderId     = j.getString("order_id"),
            qrString    = j.optString("qr_string", ""),
            expiryMs    = j.getLong("expiry_ms"),
            qrCodeUrl   = j.optString("qr_image_url", ""),
            paymentType = j.optString("payment_type", paymentType),
            deeplinkUrl = j.optString("deeplink_url", "")
        )
    }

    suspend fun checkPayment(orderId: String, deviceId: String): PaymentCheckResult? =
        withContext(Dispatchers.IO) {
            val (j, _) = postJsonWithRaw(JSONObject().apply {
                put("action",    "checkPayment")
                put("order_id",  orderId)
                put("device_id", deviceId)
            })
            if (j == null || !j.optBoolean("success", false)) return@withContext null
            PaymentCheckResult(
                status        = j.optString("payment_status", "PENDING"),
                licenseStatus = j.optString("license_status", ""),
                daysRemaining = j.optInt("days_remaining", 0),
                expiryDate    = j.optString("expiry_date", "")
            )
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
