package com.distributor.app.utils

import android.content.Context
import com.distributor.app.BuildConfig
import com.distributor.app.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object DriveBackupManager {

    private const val BUCKET      = "backups"
    private const val BACKUP_FILE = "distro-ku_backup.db"
    private const val DB_NAME     = "distributor.db"

    // ── Key + path derivation ─────────────────────────────────────────────────

    // AES-256 key tied to email — same email on any device = same decryption key
    private fun deriveKey(email: String): SecretKey {
        val material = "${email.lowercase(java.util.Locale.ROOT)}:${BuildConfig.APPLICATION_ID}"
        val hash = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, "AES")
    }

    // SHA-256 hex of email = unique bucket path, email not exposed to Supabase
    private fun pathFor(email: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(email.lowercase(java.util.Locale.ROOT).toByteArray(Charsets.UTF_8))
        val hex = hash.joinToString("") { "%02x".format(it) }
        return "$hex/$BACKUP_FILE"
    }

    // ── AES-256/CBC encryption ────────────────────────────────────────────────

    private fun encryptToFile(src: File, dst: File, email: String) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(email))
        dst.outputStream().use { out ->
            out.write(cipher.iv)
            CipherOutputStream(out, cipher).use { cOut ->
                GZIPOutputStream(cOut).use { gOut ->
                    src.inputStream().use { it.copyTo(gOut) }
                }
            }
        }
    }

    private fun decryptToFile(src: File, dst: File, email: String) {
        src.inputStream().use { input ->
            val iv = ByteArray(16).also { input.read(it) }
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(email), IvParameterSpec(iv))
            CipherInputStream(input, cipher).use { cIn ->
                GZIPInputStream(cIn).use { gIn ->
                    dst.outputStream().use { gIn.copyTo(it) }
                }
            }
        }
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    suspend fun backup(context: Context): Boolean = withContext(Dispatchers.IO) {
        val email = LicenseStore.getEmail(context)
        if (email.isBlank()) return@withContext false

        val base = BuildConfig.SUPABASE_URL
        if (base.contains("REPLACE")) return@withContext false

        val encFile = File(context.cacheDir, "backup_enc.tmp")
        try {
            runCatching {
                AppDatabase.getInstance(context)
                    .openHelper.writableDatabase
                    .execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
            }

            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) return@withContext false

            encryptToFile(dbFile, encFile, email)

            val conn = URL("$base/storage/v1/object/$BUCKET/${pathFor(email)}")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_KEY}")
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.setRequestProperty("x-upsert", "true")
            conn.connectTimeout = 30_000
            conn.readTimeout    = 60_000
            conn.doOutput       = true
            return@withContext try {
                encFile.inputStream().use { it.copyTo(conn.outputStream) }
                conn.responseCode in 200..201
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            false
        } finally {
            encFile.delete()
        }
    }

    // ── Download / Restore ────────────────────────────────────────────────────

    suspend fun downloadBackup(context: Context): File? = withContext(Dispatchers.IO) {
        val email = LicenseStore.getEmail(context)
        if (email.isBlank()) return@withContext null

        val base = BuildConfig.SUPABASE_URL
        if (base.contains("REPLACE")) return@withContext null

        val encFile = File(context.cacheDir, "restore_enc.tmp")
        try {
            val conn = URL("$base/storage/v1/object/$BUCKET/${pathFor(email)}")
                .openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_KEY}")
            conn.connectTimeout = 15_000
            conn.readTimeout    = 60_000
            try {
                if (conn.responseCode != 200) return@withContext null
                conn.inputStream.use { encFile.outputStream().use { o -> it.copyTo(o) } }
            } finally {
                conn.disconnect()
            }

            val decFile = File(context.cacheDir, "restore_tmp.db")
            decryptToFile(encFile, decFile, email)
            decFile
        } catch (e: Exception) {
            null
        } finally {
            encFile.delete()
        }
    }
}
