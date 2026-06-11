package com.distributor.app.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.distributor.app.R
import com.distributor.app.utils.PinStore
import kotlinx.coroutines.delay

// ── Lock screen ────────────────────────────────────────────────────────────

@Composable
fun PinLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    var input    by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    BackHandler { activity?.finish() }

    LaunchedEffect(input) {
        if (input.length < 4) return@LaunchedEffect
        if (PinStore.verifyPin(context, input)) {
            onUnlocked()
        } else {
            hasError = true
            errorMsg = context.getString(R.string.pin_wrong)
            delay(600)
            input    = ""
            hasError = false
            errorMsg = ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(80.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.pin_lock_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                PinDots(filled = input.length, hasError = hasError)
                Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                    if (errorMsg.isNotBlank()) {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            PinKeypad(
                modifier = Modifier.padding(bottom = 48.dp),
                onDigit  = { if (input.length < 4 && !hasError) input += it },
                onDelete = { if (input.isNotEmpty() && !hasError) input = input.dropLast(1) }
            )
        }
    }
}

// ── Setup screen ───────────────────────────────────────────────────────────

private enum class SetupStep { OPTIONS, VERIFY_CURRENT, ENTER_NEW, CONFIRM_NEW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(onComplete: () -> Unit) {
    val context      = LocalContext.current
    val pinAlreadySet = remember { PinStore.isPinEnabled(context) }

    var step        by remember { mutableStateOf(if (pinAlreadySet) SetupStep.OPTIONS else SetupStep.ENTER_NEW) }
    var disableMode by remember { mutableStateOf(false) }
    var pendingPin  by remember { mutableStateOf("") }
    var input       by remember { mutableStateOf("") }
    var hasError    by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf("") }

    fun resetInput() {
        input    = ""
        hasError = false
        errorMsg = ""
    }

    LaunchedEffect(input) {
        if (input.length < 4) return@LaunchedEffect
        when (step) {
            SetupStep.VERIFY_CURRENT -> {
                if (PinStore.verifyPin(context, input)) {
                    if (disableMode) {
                        PinStore.disablePin(context)
                        onComplete()
                    } else {
                        resetInput()
                        step = SetupStep.ENTER_NEW
                    }
                } else {
                    hasError = true
                    errorMsg = context.getString(R.string.pin_wrong)
                    delay(600)
                    resetInput()
                }
            }
            SetupStep.ENTER_NEW -> {
                pendingPin = input
                resetInput()
                step = SetupStep.CONFIRM_NEW
            }
            SetupStep.CONFIRM_NEW -> {
                if (input == pendingPin) {
                    PinStore.setPin(context, input)
                    onComplete()
                } else {
                    hasError = true
                    errorMsg = context.getString(R.string.pin_setup_mismatch)
                    delay(600)
                    resetInput()
                    step = SetupStep.ENTER_NEW
                }
            }
            SetupStep.OPTIONS -> {}
        }
    }

    BackHandler(enabled = step != SetupStep.OPTIONS) {
        if (pinAlreadySet) { resetInput(); step = SetupStep.OPTIONS } else onComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pin_setup_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step != SetupStep.OPTIONS && pinAlreadySet) {
                            resetInput(); step = SetupStep.OPTIONS
                        } else {
                            onComplete()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (step == SetupStep.OPTIONS) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.pin_setup_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { disableMode = false; resetInput(); step = SetupStep.VERIFY_CURRENT },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.pin_setup_change))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { disableMode = true; resetInput(); step = SetupStep.VERIFY_CURRENT },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.pin_setup_disable),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            val titleRes = when (step) {
                SetupStep.VERIFY_CURRENT -> R.string.pin_setup_verify_current
                SetupStep.ENTER_NEW      -> R.string.pin_setup_create
                SetupStep.CONFIRM_NEW    -> R.string.pin_setup_confirm
                SetupStep.OPTIONS        -> R.string.pin_setup_title
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(40.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    PinDots(filled = input.length, hasError = hasError)
                    Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                        if (errorMsg.isNotBlank()) {
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                PinKeypad(
                    modifier = Modifier.padding(bottom = 48.dp),
                    onDigit  = { if (input.length < 4 && !hasError) input += it },
                    onDelete = { if (input.isNotEmpty() && !hasError) input = input.dropLast(1) }
                )
            }
        }
    }
}

// ── Shared composables ─────────────────────────────────────────────────────

@Composable
private fun PinDots(filled: Int, total: Int = 4, hasError: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        repeat(total) { i ->
            val color by animateColorAsState(
                targetValue = when {
                    hasError   -> MaterialTheme.colorScheme.error
                    i < filled -> MaterialTheme.colorScheme.primary
                    else       -> MaterialTheme.colorScheme.outlineVariant
                },
                label = "dot_$i"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun PinKeypad(
    modifier: Modifier = Modifier,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("",  "0", "⌫")
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(80.dp))
                        "⌫" -> Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onDelete),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⌫",
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        else -> Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onDigit(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
