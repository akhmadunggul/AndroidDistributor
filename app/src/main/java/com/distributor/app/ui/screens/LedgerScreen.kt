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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.data.entity.TransactionEntity
import com.distributor.app.ui.components.LanguageMenuIcon
import com.distributor.app.ui.viewmodel.LedgerEntry
import com.distributor.app.ui.viewmodel.LedgerPeriod
import com.distributor.app.ui.viewmodel.LedgerViewModel
import com.distributor.app.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: LedgerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_ledger)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = { LanguageMenuIcon() }
            )
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

            // Summary cards — row 1: Revenue / Collected
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label  = stringResource(R.string.ledger_revenue),
                        amount = uiState.totalRevenue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label  = stringResource(R.string.ledger_collected),
                        amount = uiState.totalCollected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Summary cards — row 2: Outstanding / Gross Profit
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label          = stringResource(R.string.ledger_outstanding),
                        amount         = uiState.outstandingBalance,
                        containerColor = if (uiState.outstandingBalance > 0.0)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label          = stringResource(R.string.ledger_gross_profit),
                        amount         = uiState.grossProfit,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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
                        is LedgerEntry.Payment -> "pay_${entry.timestampMillis}_${entry.resellerName}"
                        is LedgerEntry.Return  -> "rtn_${entry.returnNumber}"
                    }
                }) { entry ->
                    when (entry) {
                        is LedgerEntry.Sale    -> SaleEntryRow(entry)
                        is LedgerEntry.Payment -> PaymentEntryRow(entry)
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
private fun StatCard(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = formatRupiah(amount),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SaleEntryRow(entry: LedgerEntry.Sale) {
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
                imageVector      = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint             = MaterialTheme.colorScheme.primary,
                modifier         = Modifier.size(20.dp)
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
                text  = entry.status,
                style = MaterialTheme.typography.labelSmall,
                color = when (entry.status) {
                    TransactionEntity.STATUS_PAID    -> MaterialTheme.colorScheme.primary
                    TransactionEntity.STATUS_PARTIAL -> MaterialTheme.colorScheme.tertiary
                    else                             -> MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PaymentEntryRow(entry: LedgerEntry.Payment) {
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
                imageVector      = Icons.Default.Payments,
                contentDescription = null,
                tint             = MaterialTheme.colorScheme.secondary,
                modifier         = Modifier.size(20.dp)
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
        Text(
            text       = formatRupiah(entry.amount),
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.secondary
        )
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

private fun LedgerPeriod.labelRes(): Int = when (this) {
    LedgerPeriod.TODAY      -> R.string.ledger_period_today
    LedgerPeriod.THIS_WEEK  -> R.string.ledger_period_week
    LedgerPeriod.THIS_MONTH -> R.string.ledger_period_month
    LedgerPeriod.ALL_TIME   -> R.string.ledger_period_all
}

private fun formatLedgerDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(millis))
