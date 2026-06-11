package com.distributor.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.distributor.app.R
import com.distributor.app.data.dao.LowStockAlert
import com.distributor.app.data.dao.TopProductEntry
import com.distributor.app.data.dao.TopResellerEntry
import com.distributor.app.ui.viewmodel.DashboardPeriod
import com.distributor.app.ui.viewmodel.DashboardUiState
import com.distributor.app.ui.viewmodel.DashboardViewModel
import com.distributor.app.utils.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_dashboard)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PeriodFilterRow(selected = state.period, onSelect = viewModel::selectPeriod)

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                KpiCardGrid(state = state)

                InvoiceCountRow(count = state.invoiceCount)

                state.topReseller?.let { TopResellerSection(entry = it) }

                if (state.topProducts.isNotEmpty()) {
                    TopProductsSection(products = state.topProducts)
                }

                if (state.lowStockAlerts.isNotEmpty()) {
                    LowStockSection(alerts = state.lowStockAlerts)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Period filter ──────────────────────────────────────────────────────────

@Composable
private fun PeriodFilterRow(selected: DashboardPeriod, onSelect: (DashboardPeriod) -> Unit) {
    val labels = listOf(
        DashboardPeriod.TODAY    to stringResource(R.string.ledger_period_today),
        DashboardPeriod.WEEK     to stringResource(R.string.ledger_period_week),
        DashboardPeriod.MONTH    to stringResource(R.string.ledger_period_month),
        DashboardPeriod.ALL_TIME to stringResource(R.string.ledger_period_all)
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        labels.forEach { (period, label) ->
            FilterChip(
                selected = selected == period,
                onClick  = { onSelect(period) },
                label    = { Text(label) }
            )
        }
    }
}

// ── KPI 2×2 grid ───────────────────────────────────────────────────────────

@Composable
private fun KpiCardGrid(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KpiCard(
                modifier       = Modifier.weight(1f),
                label          = stringResource(R.string.ledger_revenue),
                value          = formatRupiah(state.revenue),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
            )
            KpiCard(
                modifier       = Modifier.weight(1f),
                label          = stringResource(R.string.ledger_collected),
                value          = formatRupiah(state.collected),
                containerColor = Color(0xFFD7F0DC),
                contentColor   = Color(0xFF1B5E20)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KpiCard(
                modifier       = Modifier.weight(1f),
                label          = stringResource(R.string.ledger_outstanding),
                value          = formatRupiah(state.outstanding),
                subLabel       = stringResource(R.string.dashboard_outstanding_note),
                containerColor = Color(0xFFFFF3E0),
                contentColor   = Color(0xFFE65100)
            )
            KpiCard(
                modifier       = Modifier.weight(1f),
                label          = stringResource(R.string.ledger_gross_profit),
                value          = formatRupiah(state.grossProfit),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor   = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subLabel: String = "",
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f)
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = contentColor
            )
            if (subLabel.isNotEmpty()) {
                Text(
                    text  = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ── Invoice count ──────────────────────────────────────────────────────────

@Composable
private fun InvoiceCountRow(count: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier              = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.Receipt,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(22.dp)
                )
                Text(
                    text  = stringResource(R.string.dashboard_invoices),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text       = "$count",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Top Reseller ───────────────────────────────────────────────────────────

@Composable
private fun TopResellerSection(entry: TopResellerEntry) {
    DashboardCard(title = stringResource(R.string.dashboard_top_reseller)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector        = Icons.Default.Person,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(20.dp)
                )
                Text(
                    text       = entry.resellerName,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text       = formatRupiah(entry.totalPurchase),
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Top Products ───────────────────────────────────────────────────────────

@Composable
private fun TopProductsSection(products: List<TopProductEntry>) {
    DashboardCard(title = stringResource(R.string.dashboard_top_products)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            products.forEachIndexed { index, product ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.weight(1f)
                    ) {
                        Text(
                            text       = "${index + 1}",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier   = Modifier.width(18.dp)
                        )
                        Text(
                            text     = product.productName,
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text       = formatRupiah(product.totalRevenue),
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
                if (index < products.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── Low Stock ──────────────────────────────────────────────────────────────

@Composable
private fun LowStockSection(alerts: List<LowStockAlert>) {
    val errorContainer   = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = errorContainer)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.Warning,
                    contentDescription = null,
                    tint               = onErrorContainer,
                    modifier           = Modifier.size(18.dp)
                )
                Text(
                    text       = stringResource(R.string.dashboard_low_stock, alerts.size),
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = onErrorContainer
                )
            }
            alerts.forEach { alert ->
                val current = if (alert.currentStock % 1.0 == 0.0) alert.currentStock.toInt().toString()
                              else alert.currentStock.toString()
                val threshold = if (alert.threshold % 1.0 == 0.0) alert.threshold.toInt().toString()
                                else alert.threshold.toString()
                Text(
                    text  = "• ${alert.productName}   $current / $threshold ${alert.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = onErrorContainer
                )
            }
        }
    }
}

// ── Shared card container ──────────────────────────────────────────────────

@Composable
private fun DashboardCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text  = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}
