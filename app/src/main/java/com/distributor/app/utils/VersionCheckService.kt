package com.distributor.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ganti URL ini dengan raw URL dari GitHub Gist Anda.
 *
 * Cara membuat Gist:
 * 1. Buka https://gist.github.com
 * 2. Buat file baru bernama version.json dengan isi:
 *    {
 *      "latest_version_code": 13,
 *      "minimum_version_code": 13,
 *      "download_url": "https://link-download-apk-anda.com/app.apk",
 *      "release_notes": "Catatan versi terbaru"
 *    }
 * 3. Klik "Create public gist"
 * 4. Klik tombol "Raw", salin URL-nya, tempel di bawah ini
 */
const val VERSION_CHECK_URL =
    "https://gist.githubusercontent.com/akhmadunggul/94acf5da94eb498394d96a152d988691/raw/version.json"

data class VersionInfo(
    val latestVersionCode: Int,
    val minimumVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String
)

sealed class UpdateCheckState {
    object Idle                                        : UpdateCheckState()
    data class UpdateAvailable(val info: VersionInfo) : UpdateCheckState()
    data class MustUpdate(val info: VersionInfo)      : UpdateCheckState()
}

object VersionCheckService {
    var debugLastError: String = ""

    suspend fun fetch(url: String): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout          = 8_000
            conn.readTimeout             = 8_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent",  "DistributorApp/1.0")
            conn.setRequestProperty("Cache-Control", "no-cache")
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                debugLastError = "HTTP $code"
                conn.disconnect()
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val j = JSONObject(body)
            VersionInfo(
                latestVersionCode  = j.getInt("latest_version_code"),
                minimumVersionCode = j.getInt("minimum_version_code"),
                downloadUrl        = j.optString("download_url", ""),
                releaseNotes       = j.optString("release_notes", "")
            )
        } catch (e: Exception) {
            debugLastError = "${e.javaClass.simpleName}: ${e.message}"
            null
        }
    }
}

