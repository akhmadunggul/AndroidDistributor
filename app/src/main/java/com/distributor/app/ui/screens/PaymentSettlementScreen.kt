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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.data.entity.PaymentLogEntity
import com.distributor.app.ui.model.AllocationPreview
import com.distributor.app.ui.viewmodel.PaymentSettlementViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PAYMENT_METHODS: List<Pair<String, String>> = listOf(
    PaymentLogEntity.METHOD_CASH     to "Cash",
    PaymentLogEntity.METHOD_TRANSFER to "Bank Transfer",
    PaymentLogEntity.METHOD_OTHER    to "Other"
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

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Payment Settlement") }) },
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
                        label = { Text("Reseller") },
                        placeholder = { Text("Select reseller") },
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
                                Text("Outstanding Balance", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = formatAmount(uiState.outstandingBalance),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${uiState.outstandingInvoices.size} invoice${if (uiState.outstandingInvoices.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Outstanding invoice list
                if (uiState.outstandingInvoices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Outstanding Invoices (oldest first)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    items(uiState.outstandingInvoices, key = { it.id }) { invoice ->
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
                                    text = "Due: ${formatAmount(due)}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "of ${formatAmount(invoice.totalAmount)}",
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
                            Text("Record Payment", style = MaterialTheme.typography.titleSmall)

                            OutlinedTextField(
                                value = uiState.paymentAmountInput,
                                onValueChange = viewModel::onPaymentAmountChanged,
                                label = { Text("Payment Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            ExposedDropdownMenuBox(
                                expanded = methodExpanded,
                                onExpandedChange = { methodExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = PAYMENT_METHODS.find { it.first == uiState.paymentMethod }?.second ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Payment Method") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = methodExpanded,
                                    onDismissRequest = { methodExpanded = false }
                                ) {
                                    PAYMENT_METHODS.forEach { (method, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
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
                                label = { Text("Notes (optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // FIFO allocation preview
                if (uiState.allocationPreview.isNotEmpty()) {
                    item {
                        Text(
                            text = "Payment Allocation Preview",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    items(uiState.allocationPreview, key = { it.invoice.id }) { alloc ->
                        AllocationPreviewRow(alloc = alloc)
                        HorizontalDivider()
                    }
                }

                // Settle button
                item {
                    Button(
                        onClick = viewModel::settlePayment,
                        enabled = !uiState.isSubmitting && uiState.paymentAmountInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Settle Payment")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
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
                text = "Applied: ${formatAmount(alloc.amountApplied)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = if (alloc.balanceAfter <= 0.001) "PAID" else "Remaining: ${formatAmount(alloc.balanceAfter)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (alloc.balanceAfter <= 0.001)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
    }
}

private fun formatAmount(amount: Double): String =
    String.format(Locale.getDefault(), "%.2f", amount)

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
