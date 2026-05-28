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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.ui.viewmodel.ProductViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
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
        topBar = { TopAppBar(title = { Text("Products") }) },
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
                    Text("No products yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add your first product",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(uiState.products, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        onDelete = { viewModel.deleteProduct(product) }
                    )
                }

                item { Spacer(modifier = Modifier.height(88.dp)) } // FAB clearance
            }
        }
    }
}

@Composable
private fun ProductCard(product: ProductEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "SKU: ${product.sku}  ·  Unit: ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Cost: ${formatAmount(product.basePrice)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Price: ${formatAmount(product.sellingPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
            text = "New Product",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        OutlinedTextField(
            value = uiState.nameInput,
            onValueChange = viewModel::onNameChanged,
            label = { Text("Product Name *") },
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { e -> { Text(e) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.skuInput,
            onValueChange = viewModel::onSkuChanged,
            label = { Text("SKU *") },
            placeholder = { Text("e.g. PRD-001") },
            isError = uiState.skuError != null,
            supportingText = uiState.skuError?.let { e -> { Text(e) } },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.unitInput,
            onValueChange = viewModel::onUnitChanged,
            label = { Text("Unit  (defaults to 'pcs')") },
            placeholder = { Text("pcs / box / kg / liter") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.basePriceInput,
                onValueChange = viewModel::onBasePriceChanged,
                label = { Text("Base / Cost *") },
                isError = uiState.priceError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = uiState.sellingPriceInput,
                onValueChange = viewModel::onSellingPriceChanged,
                label = { Text("Selling Price *") },
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
            label = { Text("Description (optional)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = { viewModel.closeForm() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Cancel") }

        androidx.compose.material3.Button(
            onClick = { viewModel.saveProduct() },
            enabled = !uiState.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Save Product")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatAmount(amount: Double): String =
    String.format(Locale.getDefault(), "%.2f", amount)
