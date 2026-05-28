package com.distributor.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.ui.model.OpnameRow
import com.distributor.app.ui.viewmodel.StockOpnameViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StockOpnameScreen(
    viewModel: StockOpnameViewModel = viewModel()
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
        topBar = { TopAppBar(title = { Text("Stock Opname") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Button(
                    onClick = viewModel::saveOpname,
                    enabled = !uiState.isSubmitting && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save Adjustments")
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sticky column header
            stickyHeader {
                OpnameTableHeader()
            }

            if (uiState.rows.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No products found.\nAdd products first.",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.rows, key = { it.product.id }) { row ->
                    OpnameRowItem(
                        row = row,
                        onPhysicalInputChanged = { input ->
                            viewModel.onPhysicalInputChanged(row.product.id, input)
                        }
                    )
                    HorizontalDivider()
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun OpnameTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Product",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(2.5f),
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "System",
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Physical",
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Diff",
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun OpnameRowItem(
    row: OpnameRow,
    onPhysicalInputChanged: (String) -> Unit
) {
    val discrepancyColor: Color = when {
        row.physicalInput.isBlank() -> Color.Unspecified
        row.discrepancy > 0.001     -> Color(0xFF2E7D32)  // green — surplus → ADJUST_IN
        row.discrepancy < -0.001    -> MaterialTheme.colorScheme.error // shortage → ADJUST_OUT
        else                        -> Color(0xFF1B5E20)  // exact match
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product name + unit
        Column(modifier = Modifier.weight(2.5f)) {
            Text(
                text = row.product.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = row.product.unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // System stock
        Text(
            text = formatQty(row.systemStock),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Physical count input
        OutlinedTextField(
            value = row.physicalInput,
            onValueChange = onPhysicalInputChanged,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            modifier = Modifier.weight(1.5f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Discrepancy
        val discrepancyLabel: String = when {
            row.physicalInput.isBlank() -> "—"
            row.discrepancy > 0.001 -> "+${formatQty(row.discrepancy)}"
            row.discrepancy < -0.001 -> formatQty(row.discrepancy)
            else -> "✓"
        }
        Text(
            text = discrepancyLabel,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.SemiBold,
            color = discrepancyColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatQty(qty: Double): String =
    if (qty == qty.toLong().toDouble())
        qty.toLong().toString()
    else
        String.format(Locale.getDefault(), "%.2f", qty)
