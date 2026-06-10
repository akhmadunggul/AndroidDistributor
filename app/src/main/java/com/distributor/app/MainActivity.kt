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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.distributor.app.ui.screens.AboutScreen
import com.distributor.app.ui.screens.HomeScreen
import com.distributor.app.ui.screens.LedgerScreen
import com.distributor.app.ui.screens.SplashScreen
import com.distributor.app.ui.screens.PaymentSettlementScreen
import com.distributor.app.ui.screens.ProductListScreen
import com.distributor.app.ui.screens.ResellerListScreen
import com.distributor.app.ui.screens.ReturnScreen
import com.distributor.app.ui.screens.SettingsScreen
import com.distributor.app.ui.screens.StockOpnameScreen
import com.distributor.app.ui.screens.StockOperationScreen
import com.distributor.app.ui.screens.TransactionScreen
import com.distributor.app.utils.LocaleHelper
import com.distributor.app.utils.UpdateCheckState
import com.distributor.app.utils.VERSION_CHECK_URL
import com.distributor.app.utils.VersionCheckService
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// ── CompositionLocals ──────────────────────────────────────────────────────

val LocalAppNavController = staticCompositionLocalOf<NavHostController> {
    error("LocalAppNavController not provided")
}

// ── Routes ─────────────────────────────────────────────────────────────────

private object Routes {
    const val PRODUCTS        = "products"
    const val STOCK           = "stock"
    const val SALES           = "sales"
    const val PAYMENT         = "payment"
    const val RESELLERS       = "resellers"
    const val LEDGER          = "ledger"
    const val RETURN          = "return"
    const val STOCK_OPNAME    = "stock_opname"
    const val SETTINGS        = "settings"
    const val ABOUT           = "about"
    const val SPLASH          = "splash"
    const val HOME            = "home"
}

// ── Bottom-nav tab descriptors ──────────────────────────────────────────────

private data class NavTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

private val NAV_TABS: List<NavTab> = listOf(
    NavTab(Routes.HOME,      R.string.tab_home,      Icons.Default.Home),
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
        installSplashScreen()
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

    val showBottomBar: Boolean =
        currentRoute != Routes.STOCK_OPNAME &&
        currentRoute != Routes.SETTINGS &&
        currentRoute != Routes.PRODUCTS &&
        currentRoute != Routes.LEDGER &&
        currentRoute != Routes.ABOUT &&
        currentRoute != Routes.SPLASH &&
        currentRoute != Routes.RETURN

    // ── Version check ──────────────────────────────────────────────────────
    val context = LocalContext.current
    var updateState by remember { mutableStateOf<UpdateCheckState>(UpdateCheckState.Idle) }

    LaunchedEffect(Unit) {
        val info = VersionCheckService.fetch(VERSION_CHECK_URL) ?: return@LaunchedEffect
        val current = BuildConfig.VERSION_CODE
        updateState = when {
            current < info.minimumVersionCode -> UpdateCheckState.MustUpdate(info)
            current < info.latestVersionCode  -> UpdateCheckState.UpdateAvailable(info)
            else                              -> UpdateCheckState.Idle
        }
    }

    // Blokir tombol Back saat wajib update agar pengguna tidak bisa bypass
    BackHandler(enabled = updateState is UpdateCheckState.MustUpdate) {}

    when (val state = updateState) {
        is UpdateCheckState.MustUpdate -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Pembaruan Wajib") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Versi ini tidak lagi didukung. Unduh versi terbaru untuk melanjutkan menggunakan aplikasi.")
                    if (state.info.releaseNotes.isNotBlank())
                        Text(state.info.releaseNotes, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.info.downloadUrl)))
                }) { Text("Unduh Sekarang") }
            }
        )
        is UpdateCheckState.UpdateAvailable -> AlertDialog(
            onDismissRequest = { updateState = UpdateCheckState.Idle },
            title = { Text("Pembaruan Tersedia") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Versi baru aplikasi tersedia.")
                    if (state.info.releaseNotes.isNotBlank())
                        Text(state.info.releaseNotes, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.info.downloadUrl)))
                    updateState = UpdateCheckState.Idle
                }) { Text("Unduh Sekarang") }
            },
            dismissButton = {
                TextButton(onClick = { updateState = UpdateCheckState.Idle }) {
                    Text("Nanti")
                }
            }
        )
        else -> {}
    }

    CompositionLocalProvider(LocalAppNavController provides navController) {
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
                            label    = { Text(stringResource(tab.labelRes), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                            selected = selected,
                            onClick  = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.HOME) {
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
            startDestination = Routes.SPLASH,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToProducts  = { navController.navigate(Routes.PRODUCTS)  { launchSingleTop = true } },
                    onNavigateToStock     = { navController.navigate(Routes.STOCK)     { launchSingleTop = true } },
                    onNavigateToSales     = { navController.navigate(Routes.SALES)     { launchSingleTop = true } },
                    onNavigateToPayment   = { navController.navigate(Routes.PAYMENT)   { launchSingleTop = true } },
                    onNavigateToResellers = { navController.navigate(Routes.RESELLERS) { launchSingleTop = true } },
                    onNavigateToReturn    = { navController.navigate(Routes.RETURN)    { launchSingleTop = true } },
                    onNavigateToLedger    = { navController.navigate(Routes.LEDGER)    { launchSingleTop = true } },
                    onNavigateToSettings  = { navController.navigate(Routes.SETTINGS)  { launchSingleTop = true } }
                )
            }
            composable(Routes.RETURN) {
                ReturnScreen()
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
            composable(Routes.PRODUCTS) {
                ProductListScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.LEDGER) {
                LedgerScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.STOCK_OPNAME) {
                StockOpnameScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack    = { navController.popBackStack() },
                    onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
    } // CompositionLocalProvider
}
