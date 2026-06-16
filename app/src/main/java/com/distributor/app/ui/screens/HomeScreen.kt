package com.distributor.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.distributor.app.R
import com.distributor.app.ui.components.LanguageMenuIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToProducts:  () -> Unit = {},
    onNavigateToStoking:   () -> Unit = {},
    onNavigateToOpname:    () -> Unit = {},
    onNavigateToSales:     () -> Unit = {},
    onNavigateToPayment:   () -> Unit = {},
    onNavigateToResellers: () -> Unit = {},
    onNavigateToReturn:    () -> Unit = {},
    onNavigateToLedger:    () -> Unit = {},
    onNavigateToSettings:  () -> Unit = {},
    onNavigateToFaq:       () -> Unit = {},
    onNavigateToSuppliers: () -> Unit = {},
    onNavigateToPurchase:  () -> Unit = {}
) {
    val primary   = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val error     = MaterialTheme.colorScheme.error
    val outline   = MaterialTheme.colorScheme.outline

    data class MenuItem(
        val icon: ImageVector,
        val color: Color,
        val titleRes: Int,
        val descRes: Int,
        val onClick: () -> Unit
    )

    val items = listOf(
        MenuItem(Icons.Default.Dashboard,                    Color(0xFF7B1FA2), R.string.screen_dashboard,   R.string.home_desc_dashboard,   onNavigateToDashboard),
        MenuItem(Icons.Default.Inventory2,                   primary,           R.string.tab_products,       R.string.home_desc_products,    onNavigateToProducts),
        MenuItem(Icons.Default.Inventory,                    Color(0xFF4CAF50), R.string.tab_stoking,        R.string.home_desc_stoking,     onNavigateToStoking),
        MenuItem(Icons.Default.Checklist,                    Color(0xFF00897B), R.string.tab_stock_opname,   R.string.home_desc_stock_opname, onNavigateToOpname),
        MenuItem(Icons.Default.ShoppingCart,                 Color(0xFFFF7043), R.string.tab_sales,          R.string.home_desc_sales,       onNavigateToSales),
        MenuItem(Icons.Default.Payments,                     secondary,         R.string.tab_payment,        R.string.home_desc_payment,     onNavigateToPayment),
        MenuItem(Icons.Default.Group,                        Color(0xFF29B6F6), R.string.tab_resellers,      R.string.home_desc_resellers,   onNavigateToResellers),
        MenuItem(Icons.AutoMirrored.Filled.AssignmentReturn, error,             R.string.tab_return,         R.string.home_desc_return,      onNavigateToReturn),
        MenuItem(Icons.Default.QueryStats,                   tertiary,          R.string.tab_ledger,         R.string.home_desc_ledger,      onNavigateToLedger),
        MenuItem(Icons.Default.Settings,                     outline,           R.string.menu_settings,      R.string.home_desc_settings,    onNavigateToSettings),
        MenuItem(Icons.AutoMirrored.Filled.Help,              Color(0xFF546E7A), R.string.tab_faq,            R.string.home_desc_faq,         onNavigateToFaq),
        MenuItem(Icons.Default.Store,                         Color(0xFF00695C), R.string.tab_suppliers,      R.string.home_desc_suppliers,   onNavigateToSuppliers),
        MenuItem(Icons.Default.LocalAtm,                      Color(0xFF6D4C41), R.string.tab_purchase,       R.string.home_desc_purchase,    onNavigateToPurchase),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text(stringResource(R.string.screen_home)) },
                actions = { LanguageMenuIcon() }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns               = GridCells.Fixed(4),
            contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement   = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(items) { item ->
                HomeIconItem(
                    icon      = item.icon,
                    iconColor = item.color,
                    titleRes  = item.titleRes,
                    onClick   = item.onClick
                )
            }
        }
    }
}

// ── Icon grid layout ───────────────────────────────────────────────────────

@Composable
private fun HomeIconItem(
    icon: ImageVector,
    iconColor: Color,
    titleRes: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(iconColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(26.dp)
            )
        }
        Text(
            text       = stringResource(titleRes),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.Center,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis
        )
    }
}
