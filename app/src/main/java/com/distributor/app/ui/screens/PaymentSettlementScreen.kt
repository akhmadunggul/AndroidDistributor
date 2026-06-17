package com.distributor.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.data.entity.PaymentLogEntity
import com.distributor.app.ui.components.LanguageMenuIcon
import com.distributor.app.ui.model.AllocationPreview
import com.distributor.app.ui.viewmodel.PaymentSettlementViewModel
import com.distributor.app.utils.PaymentReceiptData
import com.distributor.app.utils.ReceiptPdfGenerator
import com.distributor.app.utils.ReceiptShareHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.distributor.app.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PAYMENT_METHODS: List<Pair<String, Int>> = listOf(
    PaymentLogEntity.METHOD_CASH     to R.string.payment_method_cash,
    PaymentLogEntity.METHOD_TRANSFER to R.string.payment_method_transfer,
    PaymentLogEntity.METHOD_OTHER    to R.string.payment_method_other
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSettlementScreen(
    viewModel: PaymentSettlementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var resellerExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }
    var showPaymentConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    uiState.pendingPaymentReceipt?.let { receiptData ->
        SharePaymentReceiptSheet(
            receiptData       = receiptData,
            snackbarHostState = snackbarHostState,
            onDismiss         = { viewModel.clearPendingPaymentReceipt() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_payment)) },
                actions = { LanguageMenuIcon() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Reseller selector
            item {
                ExposedDropdownMenuBox(
                    expanded = resellerExpanded,
                    onExpandedChange = { resellerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedReseller?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_reseller)) },
                        placeholder = { Text(stringResource(R.string.label_select_reseller)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resellerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = resellerExpanded,
                        onDismissRequest = { resellerExpanded = false }
                    ) {
                        uiState.resellers.forEach { reseller ->
                            DropdownMenuItem(
                                text = { Text("${reseller.name}  ·  ${reseller.phoneNumber}") },
                                onClick = {
                                    viewModel.onResellerSelected(reseller)
                                    resellerExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Outstanding balance summary
            if (uiState.selectedReseller != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.outstandingBalance > 0.0)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.payment_outstanding_balance), style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = formatRupiah(uiState.outstandingBalance),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = pluralStringResource(
                                    R.plurals.payment_invoice_count,
                                    uiState.outstandingInvoices.size,
                                    uiState.outstandingInvoices.size
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Outstanding invoice list
                if (uiState.outstandingInvoices.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.payment_outstanding_invoices),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    items(uiState.outstandingInvoices, key = { "inv_${it.id}" }) { invoice ->
                        val due: Double = invoice.totalAmount - invoice.amountPaid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(invoice.invoiceNumber, fontWeight = FontWeight.Medium)
                                Text(
                                    text = formatDate(invoice.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.payment_due_format, formatRupiah(due)),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = stringResource(R.string.payment_of_format, formatRupiah(invoice.totalAmount)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }

                // Payment form
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(stringResource(R.string.payment_record_title), style = MaterialTheme.typography.titleSmall)

                            OutlinedTextField(
                                value          = uiState.paymentAmountInput,
                                onValueChange  = viewModel::onPaymentAmountChanged,
                                label          = { Text(stringResource(R.string.payment_amount_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError        = uiState.amountError != null,
                                supportingText = uiState.amountError?.let { err ->
                                    { Text(err, color = MaterialTheme.colorScheme.error) }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            ExposedDropdownMenuBox(
                                expanded = methodExpanded,
                                onExpandedChange = { methodExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = PAYMENT_METHODS.find { it.first == uiState.paymentMethod }
                                        ?.second?.let { stringResource(it) } ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.payment_method_label)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = methodExpanded,
                                    onDismissRequest = { methodExpanded = false }
                                ) {
                                    PAYMENT_METHODS.forEach { (method, labelRes) ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(labelRes)) },
                                            onClick = {
                                                viewModel.onPaymentMethodChanged(method)
                                                methodExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = uiState.notesInput,
                                onValueChange = viewModel::onNotesChanged,
                                label = { Text(stringResource(R.string.payment_notes_label)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // FIFO allocation preview
                if (uiState.allocationPreview.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.payment_allocation_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    items(uiState.allocationPreview, key = { "alloc_${it.invoice.id}" }) { alloc ->
                        AllocationPreviewRow(alloc = alloc)
                        HorizontalDivider()
                    }
                }

                // Settle button
                item {
                    Button(
                        onClick = { showPaymentConfirm = true },
                        enabled = !uiState.isSubmitting &&
                                  uiState.paymentAmountInput.isNotBlank() &&
                                  uiState.amountError == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.payment_settle_button))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showPaymentConfirm) {
        AlertDialog(
            onDismissRequest = { showPaymentConfirm = false },
            title = { Text(stringResource(R.string.payment_confirm_title)) },
            text  = {
                Text(
                    stringResource(
                        R.string.payment_confirm_message,
                        formatRupiah(uiState.paymentAmountInput.toDoubleOrNull() ?: 0.0),
                        uiState.selectedReseller?.name ?: ""
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPaymentConfirm = false
                    viewModel.settlePayment()
                }) {
                    Text(stringResource(R.string.payment_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun AllocationPreviewRow(alloc: AllocationPreview) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(alloc.invoice.invoiceNumber, fontWeight = FontWeight.Medium)
            Text(
                text = stringResource(R.string.payment_applied_format, formatRupiah(alloc.amountApplied)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = if (alloc.balanceAfter <= 0.001)
                stringResource(R.string.payment_paid)
            else
                stringResource(R.string.payment_remaining_format, formatRupiah(alloc.balanceAfter)),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (alloc.balanceAfter <= 0.001)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun SharePaymentReceiptSheet(
    receiptData: PaymentReceiptData,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pdfFile      by remember { mutableStateOf<java.io.File?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    LaunchedEffect(receiptData) {
        try {
            pdfFile = withContext(Dispatchers.IO) {
                ReceiptPdfGenerator(context).generatePaymentReceipt(receiptData)
            }
        } catch (_: Exception) {
            onDismiss()
        } finally {
            isGenerating = false
        }
    }

    PdfPreviewSheet(
        file          = pdfFile,
        isGenerating  = isGenerating,
        title         = stringResource(R.string.payment_receipt_title),
        resellerEmail = receiptData.resellerEmail,
        onDismiss     = onDismiss
    )
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
