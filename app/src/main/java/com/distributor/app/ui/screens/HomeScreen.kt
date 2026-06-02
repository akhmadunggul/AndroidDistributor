package com.distributor.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.distributor.app.R
import com.distributor.app.ui.components.LanguageMenuIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProducts:  () -> Unit = {},
    onNavigateToStock:     () -> Unit = {},
    onNavigateToSales:     () -> Unit = {},
    onNavigateToPayment:   () -> Unit = {},
    onNavigateToResellers: () -> Unit = {},
    onNavigateToReturn:    () -> Unit = {},
    onNavigateToLedger:    () -> Unit = {},
    onNavigateToSettings:  () -> Unit = {}
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
        MenuItem(Icons.Default.Inventory2,                   primary,           R.string.tab_products,  R.string.home_desc_products,  onNavigateToProducts),
        MenuItem(Icons.Default.Warehouse,                    Color(0xFF4CAF50), R.string.tab_stock,     R.string.home_desc_stock,     onNavigateToStock),
        MenuItem(Icons.Default.ShoppingCart,                 Color(0xFFFF7043), R.string.tab_sales,     R.string.home_desc_sales,     onNavigateToSales),
        MenuItem(Icons.Default.Payments,                     secondary,         R.string.tab_payment,   R.string.home_desc_payment,   onNavigateToPayment),
        MenuItem(Icons.Default.Group,                        Color(0xFF29B6F6), R.string.tab_resellers, R.string.home_desc_resellers, onNavigateToResellers),
        MenuItem(Icons.AutoMirrored.Filled.AssignmentReturn, error,             R.string.tab_return,    R.string.home_desc_return,    onNavigateToReturn),
        MenuItem(Icons.Default.QueryStats,                   tertiary,          R.string.tab_ledger,    R.string.home_desc_ledger,    onNavigateToLedger),
        MenuItem(Icons.Default.Settings,                     outline,           R.string.menu_settings, R.string.home_desc_settings,  onNavigateToSettings),
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
            columns               = GridCells.Fixed(2),
            contentPadding        = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(items) { item ->
                HomeMenuCard(
                    icon      = item.icon,
                    iconColor = item.color,
                    titleRes  = item.titleRes,
                    descRes   = item.descRes,
                    onClick   = item.onClick
                )
            }
        }
    }
}

@Composable
private fun HomeMenuCard(
    icon: ImageVector,
    iconColor: Color,
    titleRes: Int,
    descRes: Int,
    onClick: () -> Unit
) {
    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconColor,
                    modifier           = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text       = stringResource(titleRes),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text      = stringResource(descRes),
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
