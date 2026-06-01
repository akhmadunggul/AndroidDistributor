package com.distributor.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.data.dao.LowStockAlert
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.ui.components.LanguageMenuIcon
import com.distributor.app.ui.viewmodel.ProductViewModel
import com.distributor.app.utils.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.isFormOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeForm() },
            sheetState = sheetState
        ) {
            ProductForm(viewModel = viewModel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_products)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = { LanguageMenuIcon() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openForm() }) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
            }
        }
    ) { padding ->
        if (uiState.products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.products_empty_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.products_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val alertIds = uiState.lowStockAlerts.map { it.productId }.toSet()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                if (uiState.lowStockAlerts.isNotEmpty()) {
                    item { LowStockBanner(uiState.lowStockAlerts) }
                }

                items(uiState.products, key = { it.id }) { product ->
                    ProductCard(
                        product    = product,
                        isLowStock = product.id in alertIds,
                        onEdit     = { viewModel.openEditForm(product) },
                        onDelete   = { viewModel.deleteProduct(product) }
                    )
                }

                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun ProductCard(product: ProductEntity, isLowStock: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (isLowStock)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    if (isLowStock) {
                        Icon(
                            imageVector        = Icons.Default.Warning,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text  = stringResource(R.string.product_sku_unit_format, product.sku, product.unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text  = stringResource(R.string.product_cost_format, formatRupiah(product.basePrice)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text  = stringResource(R.string.product_price_format, formatRupiah(product.sellingPrice)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                product.stockThreshold?.let { threshold ->
                    Text(
                        text  = stringResource(R.string.product_threshold_format, formatStockQty(threshold), product.unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLowStock)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit ${product.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${product.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ProductForm(viewModel: ProductViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(
                if (uiState.editingProduct != null) R.string.product_edit_form_title
                else R.string.product_form_title
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        OutlinedTextField(
            value = uiState.nameInput,
            onValueChange = viewModel::onNameChanged,
            label = { Text(stringResource(R.string.product_name_label)) },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { e -> { Text(e) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.skuInput,
            onValueChange = viewModel::onSkuChanged,
            label = { Text(stringResource(R.string.product_sku_label)) },
            placeholder = { Text("e.g. PRD-001") },
            isError = uiState.skuError != null,
            supportingText = uiState.skuError?.let { e -> { Text(e) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.unitInput,
            onValueChange = viewModel::onUnitChanged,
            label = { Text(stringResource(R.string.product_unit_label)) },
            placeholder = { Text(stringResource(R.string.product_unit_placeholder)) },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.basePriceInput,
                onValueChange = viewModel::onBasePriceChanged,
                label = { Text(stringResource(R.string.product_base_price_label)) },
                isError = uiState.priceError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = uiState.sellingPriceInput,
                onValueChange = viewModel::onSellingPriceChanged,
                label = { Text(stringResource(R.string.product_selling_price_label)) },
                isError = uiState.priceError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        if (uiState.priceError != null) {
            Text(
                text = uiState.priceError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        OutlinedTextField(
            value = uiState.descriptionInput,
            onValueChange = viewModel::onDescriptionChanged,
            label = { Text(stringResource(R.string.product_description_label)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value         = uiState.thresholdInput,
            onValueChange = viewModel::onThresholdChanged,
            label         = { Text(stringResource(R.string.product_threshold_label)) },
            placeholder   = { Text(stringResource(R.string.product_threshold_placeholder)) },
            isError       = uiState.thresholdError != null,
            supportingText = uiState.thresholdError?.let { e -> { Text(e) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = { viewModel.closeForm() },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.action_cancel)) }

        androidx.compose.material3.Button(
            onClick = { viewModel.saveProduct() },
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    stringResource(
                        if (uiState.editingProduct != null) R.string.product_save_changes_button
                        else R.string.product_save_button
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LowStockBanner(alerts: List<LowStockAlert>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Warning,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.error,
                    modifier           = Modifier.size(16.dp)
                )
                Text(
                    text       = stringResource(R.string.stock_low_alert_title, alerts.size),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            alerts.forEach { alert ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = alert.productName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text  = stringResource(
                            R.string.stock_low_alert_stock_format,
                            formatStockQty(alert.currentStock),
                            alert.unit,
                            formatStockQty(alert.threshold)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun formatStockQty(qty: Double): String =
    if (qty == qty.toLong().toDouble()) qty.toLong().toString()
    else "%.2f".format(qty)

