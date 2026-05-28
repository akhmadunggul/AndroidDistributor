package com.distributor.app.ui.components

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.distributor.app.LocalAppNavController
import com.distributor.app.R
import com.distributor.app.utils.LocaleHelper

@Composable
fun LanguageMenuIcon() {
    val context = LocalContext.current
    val navController = LocalAppNavController.current
    var expanded by remember { mutableStateOf(false) }
    val currentLang = LocaleHelper.getLanguage(context)

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_settings))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

        // ── Settings ──────────────────────────────────────────────────────────
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_settings)) },
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = {
                expanded = false
                navController.navigate("settings")
            }
        )

        HorizontalDivider()

        // ── Language ─────────────────────────────────────────────────────────
        DropdownMenuItem(
            text = { Text(stringResource(R.string.language_english)) },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
            trailingIcon = if (currentLang == "en") {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null,
            onClick = {
                expanded = false
                if (currentLang != "en") {
                    LocaleHelper.setLanguage(context, "en")
                    (context as Activity).recreate()
                }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.language_indonesian)) },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
            trailingIcon = if (currentLang == "id") {
                { Icon(Icons.Default.Check, contentDescription = null) }
            } else null,
            onClick = {
                expanded = false
                if (currentLang != "id") {
                    LocaleHelper.setLanguage(context, "id")
                    (context as Activity).recreate()
                }
            }
        )
    }
}
