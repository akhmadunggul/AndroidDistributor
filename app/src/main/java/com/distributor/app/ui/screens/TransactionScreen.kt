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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.distributor.app.ui.model.CartItem
import com.distributor.app.ui.viewmodel.TransactionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var resellerExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sales Order") }) },
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

            // Add-item form
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Add Item", style = MaterialTheme.typography.titleSmall)

                        // Product dropdown
                        ExposedDropdownMenuBox(
                            expanded = productExpanded,
                            onExpandedChange = { productExpanded = it }
                        ) {
                            val isOverStock = uiState.overStockProductId != null &&
                                uiState.overStockProductId == uiState.selectedProduct?.id
                            OutlinedTextField(
                                value = uiState.selectedProduct?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Product") },
                                placeholder = { Text("Select product") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                                isError = isOverStock,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = productExpanded,
                                onDismissRequest = { productExpanded = false }
                            ) {
                                uiState.products.forEach { product ->
                                    val available: Double = uiState.stockMap[product.id] ?: 0.0
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(product.name)
                                                Text(
                                                    text = "Stock: $available ${product.unit}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (available <= 0.0)
                                                        MaterialTheme.colorScheme.error
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onProductSelected(product)
                                            productExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isOverStock = uiState.overStockProductId != null

                            OutlinedTextField(
                                value = uiState.quantityInput,
                                onValueChange = viewModel::onQuantityChanged,
                                label = { Text("Qty") },
                                isError = isOverStock,
                                supportingText = if (isOverStock) {
                                    { Text("Exceeds stock", color = MaterialTheme.colorScheme.error) }
                                } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = if (isOverStock) OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.error,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.error
                                ) else OutlinedTextFieldDefaults.colors(),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = uiState.unitPriceInput,
                                onValueChange = viewModel::onUnitPriceChanged,
                                label = { Text("Unit Price") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = viewModel::addToCart,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add to Cart")
                        }
                    }
                }
            }

            // Cart header
            if (uiState.cartItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Cart (${uiState.cartItems.size} item${if (uiState.cartItems.size > 1) "s" else ""})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(uiState.cartItems, key = { "${it.product.id}_${it.unitPrice}" }) { item ->
                    CartItemRow(
                        item = item,
                        onRemove = { viewModel.removeFromCart(item) }
                    )
                    HorizontalDivider()
                }

                // Total + submit
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatAmount(uiState.cartTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = viewModel::submitTransaction,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Create Invoice")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CartItemRow(item: CartItem, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Medium)
            Text(
                text = "${item.quantity} ${item.product.unit}  ×  ${formatAmount(item.unitPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatAmount(item.subtotal),
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove item")
        }
    }
}

private fun formatAmount(amount: Double): String =
    String.format(Locale.getDefault(), "%.2f", amount)
