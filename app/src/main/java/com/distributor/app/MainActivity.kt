package com.distributor.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.distributor.app.ui.screens.PaymentSettlementScreen
import com.distributor.app.ui.screens.ProductListScreen
import com.distributor.app.ui.screens.ResellerListScreen
import com.distributor.app.ui.screens.StockOpnameScreen
import com.distributor.app.ui.screens.StockOperationScreen
import com.distributor.app.ui.screens.TransactionScreen
import com.distributor.app.utils.LocaleHelper

// ── Routes ─────────────────────────────────────────────────────────────────

private object Routes {
    const val PRODUCTS        = "products"
    const val STOCK           = "stock"
    const val SALES           = "sales"
    const val PAYMENT         = "payment"
    const val RESELLERS       = "resellers"
    const val STOCK_OPNAME    = "stock_opname"
}

// ── Bottom-nav tab descriptors ──────────────────────────────────────────────

private data class NavTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

private val NAV_TABS: List<NavTab> = listOf(
    NavTab(Routes.PRODUCTS,  R.string.tab_products,  Icons.Default.Inventory2),
    NavTab(Routes.STOCK,     R.string.tab_stock,     Icons.Default.Warehouse),
    NavTab(Routes.SALES,     R.string.tab_sales,     Icons.Default.ShoppingCart),
    NavTab(Routes.PAYMENT,   R.string.tab_payment,   Icons.Default.Payments),
    NavTab(Routes.RESELLERS, R.string.tab_resellers, Icons.Default.Group)
)

// ── Activity ───────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                DistributorNavHost()
            }
        }
    }
}

// ── Root composable ────────────────────────────────────────────────────────

@Composable
private fun DistributorNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar: Boolean = currentRoute != Routes.STOCK_OPNAME

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NAV_TABS.forEach { tab ->
                        val selected: Boolean =
                            backStackEntry?.destination?.hierarchy
                                ?.any { it.route == tab.route } == true

                        NavigationBarItem(
                            icon     = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label    = { Text(stringResource(tab.labelRes)) },
                            selected = selected,
                            onClick  = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.PRODUCTS,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Routes.PRODUCTS) {
                ProductListScreen()
            }
            composable(Routes.STOCK) {
                StockOperationScreen(
                    onNavigateToOpname = {
                        navController.navigate(Routes.STOCK_OPNAME)
                    }
                )
            }
            composable(Routes.SALES) {
                TransactionScreen()
            }
            composable(Routes.PAYMENT) {
                PaymentSettlementScreen()
            }
            composable(Routes.RESELLERS) {
                ResellerListScreen()
            }
            composable(Routes.STOCK_OPNAME) {
                StockOpnameScreen()
            }
        }
    }
}
