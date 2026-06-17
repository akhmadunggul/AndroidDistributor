package com.distributor.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.data.entity.TransactionEntity
import com.distributor.app.ui.components.LanguageMenuIcon
import com.distributor.app.ui.viewmodel.LedgerEntry
import com.distributor.app.ui.viewmodel.LedgerPdfState
import com.distributor.app.ui.viewmodel.LedgerPeriod
import com.distributor.app.ui.viewmodel.LedgerViewModel
import com.distributor.app.ui.viewmodel.ReportPeriod
import com.distributor.app.utils.PaymentReceiptData
import com.distributor.app.utils.ReceiptData
import com.distributor.app.utils.ReceiptPdfGenerator
import com.distributor.app.utils.ReceiptShareHandler
import com.distributor.app.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: LedgerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateRangePickerState = rememberDateRangePickerState()

    // ── Reshare sheets ─────────────────────────────────────────────────────
    uiState.pendingReshareReceipt?.let { ReshareInvoiceSheet(it) { viewModel.clearReshare() } }
    uiState.pendingResharePayment?.let { ResharePaymentSheet(it) { viewModel.clearReshare() } }

    // ── PDF dialogs ────────────────────────────────────────────────────────
    when (val pdf = uiState.pdfState) {
        is LedgerPdfState.PeriodPicker -> {
            AlertDialog(
                onDismissRequest = viewModel::onPdfDialogDismissed,
                title   = { Text(stringResource(R.string.ledger_pdf_dialog_title)) },
                text    = {
                    Column {
                        ReportPeriod.entries.forEach { period ->
                            TextButton(
                                onClick  = { viewModel.onPdfPeriodSelected(period) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text     = stringResource(period.labelRes()),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                },
                confirmButton  = {},
                dismissButton  = {
                    TextButton(onClick = viewModel::onPdfDialogDismissed) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
        is LedgerPdfState.DateRangePicker -> {
            DatePickerDialog(
                onDismissRequest = viewModel::onPdfDialogDismissed,
                confirmButton = {
                    TextButton(
                        onClick = {
                            val s = dateRangePickerState.selectedStartDateMillis
                            val e = dateRangePickerState.selectedEndDateMillis
                            if (s != null) viewModel.onPdfCustomRangeConfirmed(s, e ?: s)
                            else viewModel.onPdfDialogDismissed()
                        },
                        enabled = dateRangePickerState.selectedStartDateMillis != null
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onPdfDialogDismissed) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            ) {
                DateRangePicker(state = dateRangePickerState, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
        is LedgerPdfState.Generating -> {
            AlertDialog(
                onDismissRequest = {},
                title = null,
                text  = {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text(stringResource(R.string.share_generating))
                    }
                },
                confirmButton = {}
            )
        }
        is LedgerPdfState.ReadyToShare -> {
            PdfPreviewSheet(
                file         = pdf.file,
                isGenerating = false,
                title        = stringResource(R.string.ledger_pdf_share_title),
                onDismiss    = viewModel::onPdfShareDismissed
            )
        }
        else -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_ledger)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                },
                actions = { LanguageMenuIcon() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onPrintFabClicked) {
                Icon(Icons.Default.Print, contentDescription = stringResource(R.string.ledger_print_report))
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Period filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LedgerPeriod.entries) { period ->
                        FilterChip(
                            selected = uiState.period == period,
                            onClick  = { viewModel.onPeriodSelected(period) },
                            label    = { Text(stringResource(period.labelRes())) }
                        )
                    }
                }
            }

            // Compact period summary
            item { PeriodSummaryRow(uiState) }

            // Entries section
            item {
                Text(
                    text  = stringResource(R.string.ledger_entries_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.entries.isEmpty()) {
                item {
                    Text(
                        text  = stringResource(R.string.ledger_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(uiState.entries, key = { entry ->
                    when (entry) {
                        is LedgerEntry.Sale    -> "sale_${entry.invoiceNumber}"
                        is LedgerEntry.Payment -> "pay_${entry.id}"
                        is LedgerEntry.Return  -> "rtn_${entry.returnNumber}"
                    }
                }) { entry ->
                    when (entry) {
                        is LedgerEntry.Sale    -> SaleEntryRow(entry) { viewModel.reshareInvoice(entry.invoiceNumber) }
                        is LedgerEntry.Payment -> PaymentEntryRow(entry) { viewModel.resharePayment(entry.id) }
                        is LedgerEntry.Return  -> ReturnEntryRow(entry)
                    }
                    HorizontalDivider()
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PeriodSummaryRow(uiState: com.distributor.app.ui.viewmodel.LedgerUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryCell(label = stringResource(R.string.ledger_summary_omzet),   amount = uiState.totalRevenue,        color = MaterialTheme.colorScheme.primary)
            SummaryCell(label = stringResource(R.string.ledger_summary_terkumpul), amount = uiState.totalCollected,    color = MaterialTheme.colorScheme.secondary)
            SummaryCell(label = stringResource(R.string.ledger_summary_piutang),  amount = uiState.outstandingBalance, color = if (uiState.outstandingBalance > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryCell(label: String, amount: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatRupiah(amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SaleEntryRow(entry: LedgerEntry.Sale, onViewInvoice: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Row(
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Column {
                Text(entry.invoiceNumber, fontWeight = FontWeight.Medium)
                Text(
                    text  = entry.resellerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = formatLedgerDate(entry.timestampMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = formatRupiah(entry.totalAmount),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = stringResource(entry.statusLabelRes()),
                style = MaterialTheme.typography.bodySmall,
                color = when (entry.status) {
                    TransactionEntity.STATUS_PAID    -> MaterialTheme.colorScheme.primary
                    TransactionEntity.STATUS_PARTIAL -> MaterialTheme.colorScheme.tertiary
                    else                             -> MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            AssistChip(
                onClick = onViewInvoice,
                label   = { Text(stringResource(R.string.ledger_action_nota), style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize))
                }
            )
        }
    }
}

@Composable
private fun PaymentEntryRow(entry: LedgerEntry.Payment, onViewReceipt: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.Top
    ) {
        Row(
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Icon(
                imageVector        = Icons.Default.Payments,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.secondary,
                modifier           = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Column {
                Text(entry.resellerName, fontWeight = FontWeight.Medium)
                Text(
                    text  = entry.paymentMethod
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }
                        .replace('_', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = formatLedgerDate(entry.timestampMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text       = formatRupiah(entry.amount),
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(4.dp))
            AssistChip(
                onClick = onViewReceipt,
                label   = { Text(stringResource(R.string.ledger_action_kuitansi), style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Default.Payments, contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize))
                }
            )
        }
    }
}

@Composable
private fun ReturnEntryRow(entry: LedgerEntry.Return) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.AssignmentReturn,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.error,
                modifier           = Modifier.size(20.dp)
            )
            Column {
                Text(entry.returnNumber, fontWeight = FontWeight.Medium)
                Text(
                    text  = entry.resellerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = formatLedgerDate(entry.timestampMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text       = "-${formatRupiah(entry.totalAmount)}",
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ReshareInvoiceSheet(receiptData: ReceiptData, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<java.io.File?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    LaunchedEffect(receiptData) {
        try { pdfFile = withContext(Dispatchers.IO) { ReceiptPdfGenerator(context).generate(receiptData) } }
        catch (_: Exception) { onDismiss() }
        finally { isGenerating = false }
    }
    PdfPreviewSheet(
        file           = pdfFile,
        isGenerating   = isGenerating,
        title          = stringResource(R.string.share_receipt_title),
        subtitle       = receiptData.transaction.invoiceNumber,
        resellerEmail  = receiptData.resellerEmail,
        onDismiss      = onDismiss
    )
}

@Composable
private fun ResharePaymentSheet(receiptData: PaymentReceiptData, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<java.io.File?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    LaunchedEffect(receiptData) {
        try { pdfFile = withContext(Dispatchers.IO) { ReceiptPdfGenerator(context).generatePaymentReceipt(receiptData) } }
        catch (_: Exception) { onDismiss() }
        finally { isGenerating = false }
    }
    PdfPreviewSheet(
        file          = pdfFile,
        isGenerating  = isGenerating,
        title         = stringResource(R.string.payment_receipt_title),
        resellerEmail = receiptData.resellerEmail,
        onDismiss     = onDismiss
    )
}

private fun LedgerPeriod.labelRes(): Int = when (this) {
    LedgerPeriod.TODAY      -> R.string.ledger_period_today
    LedgerPeriod.THIS_WEEK  -> R.string.ledger_period_week
    LedgerPeriod.THIS_MONTH -> R.string.ledger_period_month
    LedgerPeriod.ALL_TIME   -> R.string.ledger_period_all
}

private fun ReportPeriod.labelRes(): Int = when (this) {
    ReportPeriod.TODAY      -> R.string.ledger_period_today
    ReportPeriod.THIS_WEEK  -> R.string.ledger_period_week
    ReportPeriod.THIS_MONTH -> R.string.ledger_period_month
    ReportPeriod.THIS_YEAR  -> R.string.ledger_period_year
    ReportPeriod.ALL_TIME   -> R.string.ledger_period_all
    ReportPeriod.CUSTOM     -> R.string.ledger_period_custom
}

private fun LedgerEntry.Sale.statusLabelRes(): Int = when (status) {
    TransactionEntity.STATUS_PAID    -> R.string.status_paid
    TransactionEntity.STATUS_PARTIAL -> R.string.status_partial
    else                             -> R.string.status_unpaid
}

private fun formatLedgerDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
