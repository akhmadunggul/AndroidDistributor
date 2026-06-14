package com.distributor.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import com.distributor.app.ui.screens.FaqScreen
import com.distributor.app.ui.screens.LicensePaymentScreen
import com.distributor.app.ui.screens.DashboardScreen
import com.distributor.app.ui.screens.HomeScreen
import com.distributor.app.ui.screens.LedgerScreen
import com.distributor.app.ui.screens.SplashScreen
import com.distributor.app.ui.screens.PaymentSettlementScreen
import com.distributor.app.ui.screens.ProductListScreen
import com.distributor.app.ui.screens.ResellerListScreen
import com.distributor.app.ui.screens.ReturnScreen
import com.distributor.app.ui.screens.SettingsScreen
import com.distributor.app.ui.screens.StockOpnameScreen
import com.distributor.app.ui.screens.StokingScreen
import com.distributor.app.ui.screens.TransactionScreen
import com.distributor.app.utils.LocaleHelper
import com.distributor.app.ui.screens.ActivationScreen
import com.distributor.app.ui.screens.PinLockScreen
import com.distributor.app.ui.screens.PinSetupScreen
import com.distributor.app.utils.PinStore
import com.distributor.app.utils.LicenseService
import com.distributor.app.utils.LicenseState
import com.distributor.app.utils.LicenseStore
import com.distributor.app.utils.OFFLINE_GRACE_DAYS
import com.distributor.app.utils.BusinessConfigStore
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
    const val DASHBOARD       = "dashboard"
    const val SETTINGS        = "settings"
    const val ABOUT           = "about"
    const val SPLASH          = "splash"
    const val HOME            = "home"
    const val ACTIVATION      = "activation"
    const val PIN_LOCK        = "pin_lock"
    const val PIN_SETUP       = "pin_setup"
    const val FAQ             = "faq"
    const val LICENSE_PAYMENT = "license_payment"
}

// ── Bottom-nav tab descriptors ──────────────────────────────────────────────

private data class NavTab(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

private val NAV_TABS: List<NavTab> = listOf(
    NavTab(Routes.HOME,      R.string.tab_home,      Icons.Default.Home),
    NavTab(Routes.STOCK,     R.string.tab_stoking,   Icons.Default.Warehouse),
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
        currentRoute != Routes.SETTINGS &&
        currentRoute != Routes.PRODUCTS &&
        currentRoute != Routes.DASHBOARD &&
        currentRoute != Routes.LEDGER &&
        currentRoute != Routes.ABOUT &&
        currentRoute != Routes.SPLASH &&
        currentRoute != Routes.RETURN &&
        currentRoute != Routes.ACTIVATION &&
        currentRoute != Routes.PIN_LOCK &&
        currentRoute != Routes.PIN_SETUP &&
        currentRoute != Routes.FAQ &&
        currentRoute != Routes.LICENSE_PAYMENT

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

    // ── License check ──────────────────────────────────────────────────────
    // Inisialisasi dari cache agar block langsung aktif sebelum network check selesai
    var licenseState by remember {
        val initial: LicenseState = if (!LicenseStore.isActivated(context)) {
            LicenseState.Idle
        } else {
            val expiryMs = LicenseStore.getExpiryMs(context)
            val daysLeft = if (expiryMs > 0)
                ((expiryMs - System.currentTimeMillis()) / 86_400_000L).toInt()
            else
                LicenseStore.getDaysRemaining(context)
            when {
                daysLeft <= 0       -> LicenseState.Expired
                daysLeft in 1..7    -> LicenseState.TrialWarning(daysLeft)
                else                -> LicenseState.Idle
            }
        }
        mutableStateOf(initial)
    }

    LaunchedEffect(Unit) {
        if (!LicenseStore.isActivated(context)) return@LaunchedEffect
        val response = LicenseService.check(
            LicenseStore.getLicenseKey(context),
            LicenseStore.getDeviceId(context)
        )
        if (response == null) {
            val elapsedMs  = System.currentTimeMillis() - LicenseStore.getLastCheckMs(context)
            val graceMs    = OFFLINE_GRACE_DAYS.toLong() * 24 * 60 * 60 * 1000
            if (elapsedMs > graceMs) licenseState = LicenseState.OfflineWarning
            return@LaunchedEffect
        }
        if (!response.success) {
            licenseState = when (response.error) {
                "EXPIRED" -> LicenseState.Expired
                "REVOKED" -> LicenseState.Revoked
                else      -> LicenseState.Idle
            }
            return@LaunchedEffect
        }
        LicenseStore.updateCheck(
            context, response.status,
            LicenseService.parseExpiryDate(response.expiryDate),
            response.daysRemaining
        )
        licenseState = when {
            response.daysRemaining <= 0    -> LicenseState.Expired
            response.daysRemaining in 1..7 -> LicenseState.TrialWarning(response.daysRemaining)
            else                           -> LicenseState.Idle
        }
    }

    // ── WA contact fetch (cached; used in license dialogs) ─────────────────
    var contactWa by remember { mutableStateOf(LicenseStore.getContactWa(context)) }
    var isLoadingWa by remember { mutableStateOf(contactWa == null) }

    LaunchedEffect(Unit) {
        if (!LicenseStore.isActivated(context)) { isLoadingWa = false; return@LaunchedEffect }
        val wa = LicenseService.fetchContactWa()
        if (wa != null) {
            LicenseStore.saveContactWa(context, wa)
            contactWa = wa
        }
        isLoadingWa = false
    }

    val waUnavailableMsg = stringResource(R.string.wa_contact_unavailable)
    var showRenewalPeriodDialog by remember { mutableStateOf(false) }

    fun openContactWa(period: String) {
        val number = contactWa ?: run {
            android.widget.Toast.makeText(context, waUnavailableMsg, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val businessName = BusinessConfigStore.get(context).businessName.ifBlank { "-" }
        val licenseKey   = LicenseStore.getLicenseKey(context).ifBlank { "-" }
        val message = "Halo Admin Distro-Ku,\n\n" +
            "Saya ingin mengajukan perpanjangan lisensi aplikasi Distro-Ku.\n\n" +
            "Nama UMKM: $businessName\n" +
            "ID Lisensi: $licenseKey\n" +
            "Periode Perpanjangan: $period\n\n" +
            "Mohon informasi proses selanjutnya.\n" +
            "Terima kasih."
        val cleaned = number.replace("[^0-9]".toRegex(), "")
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleaned?text=${Uri.encode(message)}")))
    }

    if (showRenewalPeriodDialog) {
        AlertDialog(
            onDismissRequest = { showRenewalPeriodDialog = false },
            title = { Text("Pilih Periode Perpanjangan") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick  = { showRenewalPeriodDialog = false; openContactWa("1 Bulan") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("1 Bulan") }
                    Button(
                        onClick  = { showRenewalPeriodDialog = false; openContactWa("1 Tahun") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("1 Tahun") }
                }
            },
            confirmButton  = {},
            dismissButton  = {
                TextButton(onClick = { showRenewalPeriodDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    BackHandler(enabled = licenseState is LicenseState.Expired || licenseState is LicenseState.Revoked) {}

    val suppressLicenseDialog = currentRoute == Routes.LICENSE_PAYMENT

    if (!suppressLicenseDialog) when (val ls = licenseState) {
        is LicenseState.Expired -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.license_expired_title)) },
            text  = { Text(stringResource(R.string.license_expired_body)) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick  = { navController.navigate(Routes.LICENSE_PAYMENT) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.license_buy_button)) }
                    ContactWaButton(isLoadingWa = isLoadingWa, onClick = { showRenewalPeriodDialog = true })
                }
            }
        )
        is LicenseState.Revoked -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.license_revoked_title)) },
            text  = { Text(stringResource(R.string.license_revoked_body)) },
            confirmButton = {
                ContactWaButton(isLoadingWa = isLoadingWa, onClick = { showRenewalPeriodDialog = true })
            }
        )
        is LicenseState.TrialWarning -> AlertDialog(
            onDismissRequest = { licenseState = LicenseState.Idle },
            title = { Text(stringResource(R.string.license_trial_warning_title)) },
            text  = { Text(stringResource(R.string.license_trial_warning_body, ls.days)) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick  = { navController.navigate(Routes.LICENSE_PAYMENT) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.license_buy_button)) }
                    ContactWaButton(isLoadingWa = isLoadingWa, onClick = { showRenewalPeriodDialog = true })
                }
            },
            dismissButton = {
                TextButton(onClick = { licenseState = LicenseState.Idle }) {
                    Text(stringResource(R.string.license_dismiss))
                }
            }
        )
        is LicenseState.OfflineWarning -> AlertDialog(
            onDismissRequest = { licenseState = LicenseState.Idle },
            title = { Text(stringResource(R.string.license_offline_title)) },
            text  = { Text(stringResource(R.string.license_offline_body)) },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { licenseState = LicenseState.Idle }) {
                    Text(stringResource(R.string.license_dismiss))
                }
            }
        )
        else -> {}
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
                    val dest = when {
                        !LicenseStore.isActivated(context) -> Routes.ACTIVATION
                        PinStore.isPinEnabled(context)     -> Routes.PIN_LOCK
                        else                               -> Routes.HOME
                    }
                    navController.navigate(dest) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(Routes.ACTIVATION) {
                ActivationScreen(onActivated = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
            composable(Routes.PIN_LOCK) {
                PinLockScreen(onUnlocked = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }
            composable(Routes.PIN_SETUP) {
                PinSetupScreen(onComplete = { navController.popBackStack() })
            }
            composable(Routes.DASHBOARD) {
                DashboardScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateToDashboard = { navController.navigate(Routes.DASHBOARD)    { launchSingleTop = true } },
                    onNavigateToProducts  = { navController.navigate(Routes.PRODUCTS)     { launchSingleTop = true } },
                    onNavigateToStoking   = { navController.navigate(Routes.STOCK)        { launchSingleTop = true } },
                    onNavigateToOpname    = { navController.navigate(Routes.STOCK_OPNAME) { launchSingleTop = true } },
                    onNavigateToSales     = { navController.navigate(Routes.SALES)        { launchSingleTop = true } },
                    onNavigateToPayment   = { navController.navigate(Routes.PAYMENT)      { launchSingleTop = true } },
                    onNavigateToResellers = { navController.navigate(Routes.RESELLERS)    { launchSingleTop = true } },
                    onNavigateToReturn    = { navController.navigate(Routes.RETURN)       { launchSingleTop = true } },
                    onNavigateToLedger    = { navController.navigate(Routes.LEDGER)       { launchSingleTop = true } },
                    onNavigateToSettings  = { navController.navigate(Routes.SETTINGS)     { launchSingleTop = true } },
                    onNavigateToFaq       = { navController.navigate(Routes.FAQ)          { launchSingleTop = true } }
                )
            }
            composable(Routes.RETURN) {
                ReturnScreen()
            }
            composable(Routes.STOCK) {
                StokingScreen()
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
                    onNavigateBack       = { navController.popBackStack() },
                    onNavigateToAbout    = { navController.navigate(Routes.ABOUT) },
                    onNavigateToPinSetup = { navController.navigate(Routes.PIN_SETUP) }
                )
            }
            composable(Routes.ABOUT) {
                AboutScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.FAQ) {
                FaqScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.LICENSE_PAYMENT) {
                LicensePaymentScreen(
                    onNavigateBack   = { navController.popBackStack() },
                    onPaymentSuccess = {
                        licenseState = LicenseState.Idle
                        navController.popBackStack()
                    }
                )
            }
        }
    }
    } // CompositionLocalProvider
}

@Composable
private fun ContactWaButton(isLoadingWa: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !isLoadingWa) {
        if (isLoadingWa) {
            CircularProgressIndicator(
                modifier    = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color       = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(stringResource(R.string.license_contact_distributor))
        }
    }
}
