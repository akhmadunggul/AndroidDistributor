package com.distributor.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.data.entity.ReturnEntity
import com.distributor.app.ui.components.LanguageMenuIcon
import com.distributor.app.ui.viewmodel.ReturnCartItem
import com.distributor.app.ui.viewmodel.ReturnViewModel
import com.distributor.app.utils.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnScreen(
    viewModel: ReturnViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_return)) },
                actions = { LanguageMenuIcon() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Reseller selector ──────────────────────────────────────────────
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedReseller?.name
                            ?: stringResource(R.string.label_select_reseller),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_reseller)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.resellers.forEach { reseller ->
                            DropdownMenuItem(
                                text = { Text(reseller.name) },
                                onClick = {
                                    viewModel.onResellerSelected(reseller)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Reason selector ────────────────────────────────────────────────
            item {
                var expanded by remember { mutableStateOf(false) }
                val reasons = listOf(
                    ReturnEntity.REASON_UNSOLD to stringResource(R.string.return_reason_unsold),
                    ReturnEntity.REASON_DEFECT to stringResource(R.string.return_reason_defect),
                    ReturnEntity.REASON_OTHER  to stringResource(R.string.return_reason_other)
                )
                val reasonLabel = reasons.firstOrNull { it.first == uiState.reason }?.second
                    ?: uiState.reason

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = reasonLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.return_reason_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        reasons.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.onReasonChanged(key)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Add item section ───────────────────────────────────────────────
            item {
                Text(
                    text  = stringResource(R.string.return_add_item_title),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedProduct?.name
                            ?: stringResource(R.string.label_select_product),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_product)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.products.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(product.name)
                                        Text(
                                            text  = product.sku,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.onProductSelected(product)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.quantityInput,
                        onValueChange = viewModel::onQuantityChanged,
                        label = { Text(stringResource(R.string.label_quantity)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.unitPriceInput,
                        onValueChange = viewModel::onUnitPriceChanged,
                        label = { Text(stringResource(R.string.sales_unit_price_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Button(
                    onClick = viewModel::addToCart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.return_add_to_cart_button))
                }
            }

            // ── Cart ───────────────────────────────────────────────────────────
            if (uiState.cartItems.isNotEmpty()) {
                item {
                    Text(
                        text  = stringResource(R.string.return_cart_header, uiState.cartItems.size),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                items(uiState.cartItems, key = { "cart_${it.product.id}" }) { item ->
                    ReturnCartRow(
                        item     = item,
                        onRemove = { viewModel.removeFromCart(item) }
                    )
                    HorizontalDivider()
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text       = stringResource(R.string.return_total),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text       = formatRupiah(uiState.cartTotal),
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Notes ──────────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChanged,
                    label = { Text(stringResource(R.string.label_notes_optional)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Submit ─────────────────────────────────────────────────────────
            item {
                Button(
                    onClick  = viewModel::submitReturn,
                    enabled  = !uiState.isSubmitting && uiState.cartItems.isNotEmpty() && uiState.selectedReseller != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.return_submit_button))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ReturnCartRow(
    item: ReturnCartItem,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Medium)
            Text(
                text  = "${item.quantity.toLong()} × ${formatRupiah(item.unitPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text       = formatRupiah(item.subtotal),
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Hapus",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
