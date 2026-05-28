package com.distributor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.distributor.app.ui.screens.PaymentSettlementScreen
import com.distributor.app.ui.screens.StockOpnameScreen
import com.distributor.app.ui.screens.StockOperationScreen
import com.distributor.app.ui.screens.TransactionScreen

// ── Navigation destinations ────────────────────────────────────────────────

private sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Stock   : Destination("stock_operation",    "Stock",    Icons.Default.Inventory2)
    data object Sales   : Destination("transaction",         "Sales",    Icons.Default.ShoppingCart)
    data object Payment : Destination("payment_settlement",  "Payment",  Icons.Default.Payments)
    data object Opname  : Destination("stock_opname",        "Opname",   Icons.Default.Assignment)
}

private val ALL_DESTINATIONS: List<Destination> = listOf(
    Destination.Stock,
    Destination.Sales,
    Destination.Payment,
    Destination.Opname
)

// ── Activity ───────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
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
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                ALL_DESTINATIONS.forEach { dest ->
                    val selected: Boolean =
                        currentDestination?.hierarchy?.any { it.route == dest.route } == true

                    NavigationBarItem(
                        icon    = { Icon(dest.icon, contentDescription = dest.label) },
                        label   = { Text(dest.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                // Pop up to the start destination to avoid building a large back-stack
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
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Destination.Stock.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Stock.route)   { StockOperationScreen() }
            composable(Destination.Sales.route)   { TransactionScreen() }
            composable(Destination.Payment.route) { PaymentSettlementScreen() }
            composable(Destination.Opname.route)  { StockOpnameScreen() }
        }
    }
}
