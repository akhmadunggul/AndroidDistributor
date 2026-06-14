package com.distributor.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.ui.viewmodel.PaymentUiState
import com.distributor.app.ui.viewmodel.PaymentViewModel
import com.distributor.app.utils.LicensePackage
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensePaymentScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_license_payment)) },
                navigationIcon = {
                    if (state !is PaymentUiState.AwaitingPayment) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val s = state) {
                is PaymentUiState.LoadingPackages -> LoadingContent(stringResource(R.string.payment_loading_packages))

                is PaymentUiState.SelectPackage -> PackageSelectionContent(
                    packages = s.packages,
                    onSelect = viewModel::selectPackage,
                    onBack   = onNavigateBack
                )

                is PaymentUiState.SelectPaymentMethod -> PaymentMethodContent(
                    pkg      = s.pkg,
                    onSelect = viewModel::selectPaymentMethod,
                    onBack   = onNavigateBack
                )

                is PaymentUiState.CreatingPayment -> LoadingContent(stringResource(R.string.payment_creating_payment))

                is PaymentUiState.AwaitingPayment -> {
                    if (s.paymentType == "qris") {
                        QrPaymentContent(state = s)
                    } else {
                        val context = LocalContext.current
                        DeeplinkPaymentContent(
                            state         = s,
                            onOpenDeeplink = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s.deeplinkUrl))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                is PaymentUiState.PaymentSuccess -> {
                    LaunchedEffect(Unit) { onPaymentSuccess() }
                    StatusContent(
                        icon        = { Icon(Icons.Default.CheckCircle, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary) },
                        title       = stringResource(R.string.payment_qr_success_title),
                        body        = stringResource(R.string.payment_qr_success_body),
                        buttonLabel = null,
                        onAction    = null
                    )
                }

                is PaymentUiState.PaymentExpired -> StatusContent(
                    icon        = null,
                    title       = stringResource(R.string.payment_qr_expired_title),
                    body        = stringResource(R.string.payment_qr_expired_body),
                    buttonLabel = stringResource(R.string.payment_qr_retry),
                    onAction    = viewModel::retryWithSamePackage
                )

                is PaymentUiState.Error -> StatusContent(
                    icon        = null,
                    title       = stringResource(R.string.payment_error_title),
                    body        = s.message,
                    buttonLabel = stringResource(R.string.payment_qr_retry),
                    onAction    = if (s.retryPackages) viewModel::loadPackages else viewModel::retryWithSamePackage
                )
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PackageSelectionContent(
    packages: List<LicensePackage>,
    onSelect: (LicensePackage) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text       = stringResource(R.string.payment_license_packages_title),
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        packages.forEach { pkg ->
            PackageCard(pkg = pkg, onSelect = { onSelect(pkg) })
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

@Composable
private fun PackageCard(pkg: LicensePackage, onSelect: () -> Unit) {
    val priceStr = "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(pkg.price)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pkg.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text  = "${pkg.durationDays} hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text       = priceStr,
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
            Button(onClick = onSelect) {
                Text(stringResource(R.string.payment_license_buy))
            }
        }
    }
}

@Composable
private fun QrPaymentContent(state: PaymentUiState.AwaitingPayment) {
    val minutes     = TimeUnit.MILLISECONDS.toMinutes(state.timeLeftMs)
    val seconds     = TimeUnit.MILLISECONDS.toSeconds(state.timeLeftMs) % 60
    val timeStr     = "%02d:%02d".format(minutes, seconds)
    val priceStr    = "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(state.amount)
    val isUrgent    = state.timeLeftMs in 1..60_000

    Column(
        modifier                = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment     = Alignment.CenterHorizontally,
        verticalArrangement     = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = stringResource(R.string.payment_qr_title),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
        Text(
            text  = "${state.packageName} · $priceStr",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Card(
            elevation = CardDefaults.cardElevation(4.dp),
            shape     = MaterialTheme.shapes.medium
        ) {
            state.qrBitmap?.let { bmp ->
                Image(
                    bitmap             = bmp.asImageBitmap(),
                    contentDescription = "QRIS",
                    modifier           = Modifier.fillMaxWidth(0.82f).aspectRatio(1f),
                    contentScale       = ContentScale.Fit
                )
            }
        }
        Surface(
            color = if (isUrgent) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text      = stringResource(R.string.payment_qr_expires, timeStr),
                modifier  = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style     = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color     = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Text(
                text  = stringResource(R.string.payment_qr_waiting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (state.qrCodeUrl.isNotBlank()) {
            val clipboard = LocalClipboardManager.current
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { clipboard.setText(AnnotatedString(state.qrCodeUrl)) }
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("QR Image URL (tap to copy)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.qrCodeUrl, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodContent(
    pkg: LicensePackage,
    onSelect: (String) -> Unit,
    onBack: () -> Unit
) {
    val priceStr = "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(pkg.price)
    Column(
        modifier            = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text       = stringResource(R.string.payment_method_title),
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "${pkg.name} · $priceStr",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PaymentMethodCard(
            label       = "QRIS",
            description = stringResource(R.string.payment_method_qris_desc),
            onClick     = { onSelect("qris") }
        )
        PaymentMethodCard(
            label       = "GoPay",
            description = stringResource(R.string.payment_method_gopay_desc),
            onClick     = { onSelect("gopay") }
        )
        PaymentMethodCard(
            label       = "ShopeePay",
            description = stringResource(R.string.payment_method_shopeepay_desc),
            onClick     = { onSelect("shopeepay") }
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

@Composable
private fun PaymentMethodCard(label: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DeeplinkPaymentContent(
    state: PaymentUiState.AwaitingPayment,
    onOpenDeeplink: () -> Unit
) {
    val minutes  = TimeUnit.MILLISECONDS.toMinutes(state.timeLeftMs)
    val seconds  = TimeUnit.MILLISECONDS.toSeconds(state.timeLeftMs) % 60
    val timeStr  = "%02d:%02d".format(minutes, seconds)
    val isUrgent = state.timeLeftMs in 1..60_000
    val priceStr = "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(state.amount)
    val walletName = when (state.paymentType) {
        "gopay"     -> "GoPay"
        "shopeepay" -> "ShopeePay"
        else        -> "E-Wallet"
    }

    Column(
        modifier            = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text       = "${state.packageName} · $priceStr",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )
        Surface(
            color = if (isUrgent) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text       = stringResource(R.string.payment_qr_expires, timeStr),
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer
                             else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Button(
            onClick  = onOpenDeeplink,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.payment_open_wallet, walletName),
                style = MaterialTheme.typography.titleMedium)
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Text(
                text  = stringResource(R.string.payment_wallet_waiting, walletName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusContent(
    icon: (@Composable () -> Unit)?,
    title: String,
    body: String,
    buttonLabel: String?,
    onAction: (() -> Unit)?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        icon?.invoke()
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (buttonLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(buttonLabel) }
        }
    }
}
