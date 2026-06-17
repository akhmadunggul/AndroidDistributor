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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.data.dao.PurchasePaymentEntry
import com.distributor.app.ui.components.LanguageMenuIcon
import com.distributor.app.ui.viewmodel.PurchaseViewModel
import com.distributor.app.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PURCHASE_METHODS = listOf(
    "CASH"     to R.string.payment_method_cash,
    "TRANSFER" to R.string.payment_method_transfer,
    "OTHER"    to R.string.payment_method_other
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: PurchaseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // ── Date picker dialog ─────────────────────────────────────────────────
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.purchaseDateMs
        )
        DatePickerDialog(
            onDismissRequest = viewModel::onDatePickerDismissed,
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) viewModel.onDateSelected(selected)
                    else viewModel.onDatePickerDismissed()
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDatePickerDismissed) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState, modifier = Modifier.fillMaxWidth())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_purchase)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                },
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
            item { Spacer(Modifier.height(4.dp)) }

            // ── Supplier selector ────────────────────────────────────────────
            item {
                var supplierExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = supplierExpanded,
                    onExpandedChange = { supplierExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedSupplier?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.purchase_select_supplier)) },
                        placeholder = { Text(stringResource(R.string.purchase_select_supplier)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = supplierExpanded,
                        onDismissRequest = { supplierExpanded = false }
                    ) {
                        if (uiState.suppliers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.suppliers_empty_title)) },
                                onClick = { supplierExpanded = false }
                            )
                        }
                        uiState.suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.name) },
                                onClick = {
                                    viewModel.onSupplierSelected(supplier)
                                    supplierExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Amount ───────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = viewModel::onAmountChanged,
                    label = { Text(stringResource(R.string.purchase_amount_label)) },
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { e -> { Text(e) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("Rp ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Date ─────────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(Date(uiState.purchaseDateMs)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.purchase_date_label)) },
                    trailingIcon = {
                        IconButton(onClick = viewModel::onShowDatePicker) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Payment method ───────────────────────────────────────────────
            item {
                var methodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = it }
                ) {
                    OutlinedTextField(
                        value = PURCHASE_METHODS.find { it.first == uiState.paymentMethod }
                            ?.second?.let { stringResource(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.purchase_method_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        PURCHASE_METHODS.forEach { (method, labelRes) ->
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
            }

            // ── Notes ────────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = uiState.notesInput,
                    onValueChange = viewModel::onNotesChanged,
                    label = { Text(stringResource(R.string.purchase_notes_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Submit button ────────────────────────────────────────────────
            item {
                Button(
                    onClick = viewModel::recordPayment,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.purchase_submit_button))
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // ── History header ────────────────────────────────────────────────
            item {
                Text(
                    text = stringResource(R.string.purchase_history_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.payments.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.purchase_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(uiState.payments, key = { it.id }) { entry ->
                    PurchaseEntryRow(entry)
                    HorizontalDivider()
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PurchaseEntryRow(entry: PurchasePaymentEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(entry.supplierName, fontWeight = FontWeight.Medium)
                Text(
                    text = entry.paymentMethod.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(entry.purchaseDateMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.notes.isNotBlank()) {
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = formatRupiah(entry.amount),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
    }
}

