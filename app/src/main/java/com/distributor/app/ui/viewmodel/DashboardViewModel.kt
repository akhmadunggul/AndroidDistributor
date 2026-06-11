package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.dao.LowStockAlert
import com.distributor.app.data.dao.TopProductEntry
import com.distributor.app.data.dao.TopResellerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DashboardPeriod { TODAY, WEEK, MONTH, ALL_TIME }

data class DashboardUiState(
    val period: DashboardPeriod = DashboardPeriod.TODAY,
    val revenue: Double = 0.0,
    val collected: Double = 0.0,
    val outstanding: Double = 0.0,
    val grossProfit: Double = 0.0,
    val invoiceCount: Int = 0,
    val topReseller: TopResellerEntry? = null,
    val topProducts: List<TopProductEntry> = emptyList(),
    val lowStockAlerts: List<LowStockAlert> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db        = AppDatabase.getInstance(application)
    private val ledgerDao = db.ledgerDao()
    private val stockDao  = db.stockLedgerDao()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            stockDao.getLowStockAlertsFlow().collect { alerts ->
                _uiState.update { it.copy(lowStockAlerts = alerts) }
            }
        }
        loadMetrics(DashboardPeriod.TODAY)
    }

    fun selectPeriod(period: DashboardPeriod) {
        _uiState.update { it.copy(period = period, isLoading = true) }
        loadMetrics(period)
    }

    private fun loadMetrics(period: DashboardPeriod) {
        val (from, to) = period.toTimeRange()
        viewModelScope.launch(Dispatchers.IO) {
            val revenue      = ledgerDao.getTotalRevenue(from, to)
            val collected    = ledgerDao.getTotalCollected(from, to)
            val outstanding  = ledgerDao.getOutstandingBalance()
            val grossProfit  = ledgerDao.getGrossProfit(from, to)
            val invoiceCount = ledgerDao.getTransactionCount(from, to)
            val topReseller  = ledgerDao.getTopResellerOverall(from, to)
            val topProducts  = ledgerDao.getTopProducts(from, to)
            _uiState.update {
                it.copy(
                    revenue      = revenue,
                    collected    = collected,
                    outstanding  = outstanding,
                    grossProfit  = grossProfit,
                    invoiceCount = invoiceCount,
                    topReseller  = topReseller,
                    topProducts  = topProducts,
                    isLoading    = false
                )
            }
        }
    }
}

private fun DashboardPeriod.toTimeRange(): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    return when (this) {
        DashboardPeriod.TODAY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to now
        }
        DashboardPeriod.WEEK -> {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to now
        }
        DashboardPeriod.MONTH -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to now
        }
        DashboardPeriod.ALL_TIME -> 0L to Long.MAX_VALUE / 2
    }
}
