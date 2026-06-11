package com.distributor.app.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.BuildConfig
import com.distributor.app.utils.BusinessConfigStore
import com.distributor.app.utils.HomeLayoutStyle
import com.distributor.app.R
import com.distributor.app.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPinSetup: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    var homeLayout by remember { mutableStateOf(BusinessConfigStore.getHomeLayoutStyle(ctx)) }

    val savedMessage  = stringResource(R.string.settings_saved)
    val logoErrKey    = "logo_error"
    val logoErrMsg    = stringResource(R.string.settings_logo_error)
    val resetSuccess  = stringResource(R.string.reset_success)

    var showResetConfirm1 by remember { mutableStateOf(false) }
    var showResetConfirm2 by remember { mutableStateOf(false) }
    var showDemoConfirm   by remember { mutableStateOf(false) }
    val demoLoaded = stringResource(R.string.demo_data_loaded)

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(if (msg == logoErrKey) logoErrMsg else msg)
            viewModel.clearSnackbar()
        }
    }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.saveLogo(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── App ───────────────────────────────────────────────────────────
            OutlinedButton(
                onClick  = onNavigateToAbout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_about))
            }

            HorizontalDivider()

            // ── Logo ──────────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_logo_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LogoSection(
                logoFile   = uiState.logoFile,
                onUpload   = { logoPickerLauncher.launch("image/*") },
                onRemove   = { viewModel.removeLogo() }
            )

            HorizontalDivider()

            // ── Business fields ───────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.businessName,
                onValueChange = viewModel::onBusinessNameChanged,
                label = { Text(stringResource(R.string.settings_business_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.ownerPhone,
                onValueChange = viewModel::onOwnerPhoneChanged,
                label = { Text(stringResource(R.string.settings_owner_phone)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChanged,
                label = { Text(stringResource(R.string.settings_address)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.saveConfig(savedMessage) },
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.settings_save_button))
                }
            }

            HorizontalDivider()

            // ── Display ───────────────────────────────────────────────────────
            Text(
                text  = stringResource(R.string.settings_display_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text  = stringResource(R.string.home_layout_label),
                style = MaterialTheme.typography.bodyMedium
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                HomeLayoutStyle.entries.forEachIndexed { index, style ->
                    SegmentedButton(
                        selected = homeLayout == style,
                        onClick  = {
                            homeLayout = style
                            BusinessConfigStore.saveHomeLayoutStyle(ctx, style)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                    ) {
                        Text(
                            stringResource(
                                if (style == HomeLayoutStyle.CARD) R.string.home_layout_card
                                else R.string.home_layout_icon_grid
                            )
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Security ──────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_security_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick  = onNavigateToPinSetup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_pin_setup))
            }

            HorizontalDivider()

            // ── Danger Zone ───────────────────────────────────────────────────
            Text(
                text  = stringResource(R.string.settings_danger_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(
                onClick  = { showDemoConfirm = true },
                enabled  = !uiState.isResetting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_demo_button))
            }

            Button(
                onClick  = { showResetConfirm1 = true },
                enabled  = !uiState.isResetting,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isResetting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.settings_reset_button))
                }
            }

            HorizontalDivider()

            // ── About ─────────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = stringResource(R.string.settings_version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = stringResource(
                        R.string.settings_version_format,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    ),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = stringResource(R.string.settings_release_date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = BuildConfig.RELEASE_DATE,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ── First confirmation ─────────────────────────────────────────────────
    if (showResetConfirm1) {
        AlertDialog(
            onDismissRequest = { showResetConfirm1 = false },
            title   = { Text(stringResource(R.string.reset_confirm1_title)) },
            text    = { Text(stringResource(R.string.reset_confirm1_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm1 = false
                    showResetConfirm2 = true
                }) {
                    Text(
                        stringResource(R.string.reset_confirm1_proceed),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm1 = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // ── Demo data confirmation ─────────────────────────────────────────────
    if (showDemoConfirm) {
        AlertDialog(
            onDismissRequest = { showDemoConfirm = false },
            title   = { Text(stringResource(R.string.demo_confirm_title)) },
            text    = { Text(stringResource(R.string.demo_confirm_message)) },
            confirmButton = {
                Button(onClick = {
                    showDemoConfirm = false
                    viewModel.seedDemoData(demoLoaded)
                }) {
                    Text(stringResource(R.string.demo_confirm_load))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // ── Second confirmation ────────────────────────────────────────────────
    if (showResetConfirm2) {
        AlertDialog(
            onDismissRequest = { showResetConfirm2 = false },
            title   = { Text(stringResource(R.string.reset_confirm2_title)) },
            text    = { Text(stringResource(R.string.reset_confirm2_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm2 = false
                        viewModel.resetAllData(resetSuccess)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.reset_confirm2_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm2 = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun LogoSection(
    logoFile: File?,
    onUpload: () -> Unit,
    onRemove: () -> Unit
) {
    // Load bitmap on IO — null while loading or if no file
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = logoFile) {
        value = withContext(Dispatchers.IO) {
            logoFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Logo preview box
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Business Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (logoFile != null)
                        stringResource(R.string.settings_change_logo)
                    else
                        stringResource(R.string.settings_upload_logo)
                )
            }
            if (logoFile != null) {
                OutlinedButton(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.settings_remove_logo),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
