package com.distributor.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.data.entity.StockLedgerEntity
import com.distributor.app.ui.components.LanguageMenuIcon
import com.distributor.app.ui.viewmodel.StockOperationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OPERATION_TYPES: List<Pair<String, Int>> = listOf(
    StockLedgerEntity.TYPE_IN         to R.string.stock_type_in,
    StockLedgerEntity.TYPE_OUT        to R.string.stock_type_out,
    StockLedgerEntity.TYPE_ADJUST_IN  to R.string.stock_type_adjust_in,
    StockLedgerEntity.TYPE_ADJUST_OUT to R.string.stock_type_adjust_out
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOperationScreen(
    onNavigateToOpname: () -> Unit = {},
    viewModel: StockOperationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDateMillis
    )
    var productDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.isDatePickerOpen) {
        DatePickerDialog(
            onDismissRequest = { viewModel.closeDatePicker() },
            confirmButton = {
                TextButton(onClick = { viewModel.onDateSelected(datePickerState.selectedDateMillis) }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDatePicker() }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_stock)) },
                actions = {
                    IconButton(onClick = onNavigateToOpname) {
                        Icon(Icons.Default.Checklist, contentDescription = "Stock Opname")
                    }
                    LanguageMenuIcon()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Stok Saat Ini ────────────────────────────────────────────────
            Text(
                text  = stringResource(R.string.stock_section_current),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            StockSummaryGrid(
                products  = uiState.products,
                allStocks = uiState.allStocks
            )

            HorizontalDivider()

            // ── Catat Entri ───────────────────────────────────────────────────
            Text(
                text  = stringResource(R.string.stock_section_record),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Operation type
            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = OPERATION_TYPES.find { it.first == uiState.typeSelection }
                        ?.second?.let { stringResource(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.stock_op_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false }
                ) {
                    OPERATION_TYPES.forEach { (type, labelRes) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(labelRes)) },
                            onClick = {
                                viewModel.onTypeSelected(type)
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Product
            ExposedDropdownMenuBox(
                expanded = productDropdownExpanded,
                onExpandedChange = { productDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.selectedProduct?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.label_product)) },
                    placeholder = { Text(stringResource(R.string.stock_op_product_placeholder)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = productDropdownExpanded,
                    onDismissRequest = { productDropdownExpanded = false }
                ) {
                    uiState.products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text("${product.name}  ·  ${product.sku}") },
                            onClick = {
                                viewModel.onProductSelected(product)
                                productDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            if (uiState.selectedProduct != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.stock_op_current_stock,
                            uiState.currentStock.toString(),
                            uiState.selectedProduct!!.unit
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Quantity
            OutlinedTextField(
                value = uiState.quantityInput,
                onValueChange = viewModel::onQuantityChanged,
                label = { Text(stringResource(R.string.label_quantity)) },
                isError = uiState.quantityError != null,
                supportingText = uiState.quantityError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Unit cost — only shown for inbound operations
            if (uiState.typeSelection in StockLedgerEntity.INBOUND_TYPES) {
                OutlinedTextField(
                    value = uiState.unitCostInput,
                    onValueChange = viewModel::onUnitCostChanged,
                    label = { Text(stringResource(R.string.stock_op_unit_cost_label)) },
                    isError = uiState.unitCostError != null,
                    supportingText = uiState.unitCostError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Date
            OutlinedTextField(
                value = formatDate(uiState.selectedDateMillis),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.label_date)) },
                trailingIcon = {
                    IconButton(onClick = viewModel::openDatePicker) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = uiState.notesInput,
                onValueChange = viewModel::onNotesChanged,
                label = { Text(stringResource(R.string.label_notes_optional)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::submitEntry,
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.stock_op_save_button))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StockSummaryGrid(
    products: List<ProductEntity>,
    allStocks: Map<Long, Double>
) {
    if (products.isEmpty()) {
        Text(
            text  = "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val rows = products.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { product ->
                    val stock = allStocks[product.id] ?: 0.0
                    StockCard(
                        product  = product,
                        stock    = stock,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StockCard(
    product:  ProductEntity,
    stock:    Double,
    modifier: Modifier = Modifier
) {
    val isLow = product.stockThreshold != null && stock <= product.stockThreshold
    val containerColor = if (isLow)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text       = product.name,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1
            )
            Text(
                text  = product.sku,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text       = formatStock(stock),
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text     = product.unit,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            if (isLow) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Warning,
                        contentDescription = null,
                        modifier           = Modifier.size(12.dp),
                        tint               = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text  = "Stok rendah",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatStock(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else value.toString()

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
