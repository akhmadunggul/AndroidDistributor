package com.distributor.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.distributor.app.R
import com.distributor.app.utils.LicenseService
import com.distributor.app.utils.LicenseStore
import kotlinx.coroutines.launch

@Composable
fun ActivationScreen(onActivated: () -> Unit) {
    var keyInput   by remember { mutableStateOf(TextFieldValue("")) }
    var emailInput by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf("") }
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val keyboard   = LocalSoftwareKeyboardController.current

    fun activate() {
        val key   = keyInput.text.trim()
        val email = emailInput.trim()
        if (key.isBlank()) { errorMsg = context.getString(R.string.activation_error_empty); return }
        if (!email.contains("@") || !email.contains(".")) {
            errorMsg = context.getString(R.string.activation_email_error_invalid); return
        }
        keyboard?.hide()
        scope.launch {
            isLoading = true
            errorMsg  = ""
            val resp = LicenseService.activate(key, LicenseStore.getDeviceId(context), Build.MODEL)
            isLoading = false
            when {
                resp == null   -> errorMsg = context.getString(R.string.activation_error_network)
                !resp.success  -> errorMsg = when (resp.error) {
                    "KEY_NOT_FOUND"                  -> context.getString(R.string.activation_error_not_found)
                    "REVOKED"                        -> context.getString(R.string.activation_error_revoked)
                    "ALREADY_ACTIVATED_OTHER_DEVICE" -> context.getString(R.string.activation_error_other_device)
                    else                             -> context.getString(R.string.activation_error_generic)
                }
                else -> {
                    LicenseStore.save(
                        context, key, resp.status,
                        LicenseService.parseExpiryDate(resp.expiryDate),
                        resp.daysRemaining
                    )
                    LicenseStore.saveEmail(context, email)
                    onActivated()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text       = stringResource(R.string.app_name),
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = stringResource(R.string.activation_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text      = stringResource(R.string.activation_subtitle),
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = keyInput,
                onValueChange = {
                    val formatted = formatLicenseKey(it.text)
                    keyInput = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                    errorMsg = ""
                },
                label         = { Text(stringResource(R.string.activation_key_label)) },
                placeholder   = { Text("DIST-XXXX-XXXX") },
                singleLine    = true,
                isError       = errorMsg.isNotBlank(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction      = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value         = emailInput,
                onValueChange = { emailInput = it; errorMsg = "" },
                label         = { Text(stringResource(R.string.activation_email_label)) },
                placeholder   = { Text("contoh@email.com") },
                singleLine    = true,
                isError       = errorMsg.isNotBlank(),
                supportingText = if (errorMsg.isNotBlank()) {
                    { Text(errorMsg, color = MaterialTheme.colorScheme.error) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { activate() }),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick  = { activate() },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.activation_button))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text      = stringResource(R.string.activation_trial_prompt),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            val trialMessage = stringResource(R.string.activation_trial_wa_message)
            Text(
                text           = stringResource(R.string.activation_trial_link),
                style          = MaterialTheme.typography.bodyMedium,
                color          = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                textAlign      = TextAlign.Center,
                modifier       = Modifier.clickable {
                    val number = "6285196197819"
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/$number?text=${Uri.encode(trialMessage)}"))
                    )
                }
            )
        }
    }
}

// Auto-format: DISTA1B2C3D4 → DIST-A1B2-C3D4
private fun formatLicenseKey(raw: String): String {
    val clean = raw.filter { it.isLetterOrDigit() }.uppercase().take(12)
    return buildString {
        clean.forEachIndexed { i, c ->
            if (i == 4 || i == 8) append('-')
            append(c)
        }
    }
}
