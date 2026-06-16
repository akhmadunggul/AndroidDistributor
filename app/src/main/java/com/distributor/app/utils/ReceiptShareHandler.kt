package com.distributor.app.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Handles secure PDF receipt sharing via FileProvider content URIs.
 *
 * Flow:
 *   1. Convert the [File] to a content:// URI via FileProvider (never a file:// URI).
 *   2. Grant temporary read permission to the receiving app via FLAG_GRANT_READ_URI_PERMISSION.
 *   3. Target WhatsApp directly; fall back to the OS system chooser if not installed.
 *
 * The FileProvider authority must match the <provider> declaration in AndroidManifest.xml:
 *   android:authorities="${applicationId}.fileprovider"
 */
object ReceiptShareHandler {

    private const val WHATSAPP_PACKAGE: String = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE: String = "com.whatsapp.w4b"
    private const val RECEIPT_MIME_TYPE: String = "application/pdf"

    /**
     * Attempts to open WhatsApp directly for sharing [file].
     * Falls back to [shareViaSystemSheet] if WhatsApp is not installed.
     */
    fun shareToWhatsApp(context: Context, file: File) {
        val uri: Uri = buildContentUri(context, file)
        val intent: Intent = buildShareIntent(uri).apply {
            setPackage(WHATSAPP_PACKAGE)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp not installed — present the OS chooser as fallback
            launchSystemChooser(context, uri)
        }
    }

    /**
     * Attempts to open WhatsApp Business directly for sharing [file].
     * Falls back to [shareViaSystemSheet] if WhatsApp Business is not installed.
     */
    fun shareToWhatsAppBusiness(context: Context, file: File) {
        val uri: Uri = buildContentUri(context, file)
        val intent: Intent = buildShareIntent(uri).apply {
            setPackage(WHATSAPP_BUSINESS_PACKAGE)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            launchSystemChooser(context, uri)
        }
    }

    /**
     * Presents the OS system share sheet without targeting a specific app.
     * Safe to call from any context where multiple share targets are acceptable.
     */
    fun shareViaSystemSheet(context: Context, file: File) {
        val uri: Uri = buildContentUri(context, file)
        launchSystemChooser(context, uri)
    }

    /**
     * Opens an email chooser with the PDF attached and the To/Subject pre-filled.
     * If [toEmail] is blank the email client opens without a pre-filled recipient.
     */
    fun shareViaEmail(context: Context, file: File, toEmail: String, subject: String = "") {
        val uri: Uri = buildContentUri(context, file)
        val intent = buildShareIntent(uri).apply {
            if (toEmail.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(toEmail))
            if (subject.isNotBlank())  putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        val chooser = Intent.createChooser(intent, "Send via Email").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Converts a [File] from the app's cache directory into a content:// URI
     * that external apps can read without direct filesystem access.
     *
     * Requires a matching <cache-path> entry in res/xml/file_paths.xml and
     * a <provider> block in AndroidManifest.xml.
     */
    private fun buildContentUri(context: Context, file: File): Uri {
        val authority: String = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Builds the base ACTION_SEND intent with the content URI and read permission flag.
     * The FLAG_GRANT_READ_URI_PERMISSION grant is scoped to the lifetime of this intent —
     * it is automatically revoked when the receiving task is cleared.
     */
    private fun buildShareIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = RECEIPT_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun launchSystemChooser(context: Context, uri: Uri) {
        val chooser: Intent = Intent.createChooser(
            buildShareIntent(uri),
            "Share Receipt"
        ).apply {
            // New task flag required when starting from a non-Activity context
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
