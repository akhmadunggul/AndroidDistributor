package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.dao.PaymentLedgerEntry
import com.distributor.app.data.dao.ReturnLedgerEntry
import com.distributor.app.data.dao.SaleLedgerEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class LedgerPeriod { TODAY, THIS_WEEK, THIS_MONTH, ALL_TIME }

sealed class LedgerEntry {
    abstract val timestampMillis: Long

    data class Sale(
        val invoiceNumber: String,
        val resellerName: String,
        val totalAmount: Double,
        val amountPaid: Double,
        val status: String,
        override val timestampMillis: Long
    ) : LedgerEntry()

    data class Payment(
        val resellerName: String,
        val amount: Double,
        val paymentMethod: String,
        override val timestampMillis: Long
    ) : LedgerEntry()

    data class Return(
        val returnNumber: String,
        val resellerName: String,
        val totalAmount: Double,
        val reason: String,
        override val timestampMillis: Long
    ) : LedgerEntry()
}

data class LedgerUiState(
    val period: LedgerPeriod = LedgerPeriod.THIS_MONTH,
    val totalRevenue: Double = 0.0,
    val totalCollected: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val grossProfit: Double = 0.0,
    val entries: List<LedgerEntry> = emptyList(),
    val isLoading: Boolean = true
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val ledgerDao  = AppDatabase.getInstance(application).ledgerDao()
    private val returnDao  = AppDatabase.getInstance(application).returnDao()

    private val _period = MutableStateFlow(LedgerPeriod.THIS_MONTH)
    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        observeLedger()
    }

    fun onPeriodSelected(period: LedgerPeriod) {
        _period.value = period
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLedger() {
        viewModelScope.launch {
            try {
                _period.flatMapLatest { period ->
                    val (from, to) = period.toDateRange()
                    combine(
                        ledgerDao.getSaleEntriesFlow(from, to),
                        ledgerDao.getPaymentEntriesFlow(from, to),
                        returnDao.getReturnEntriesFlow(from, to)
                    ) { sales, payments, returns -> Triple(period, Triple(sales, payments, returns), Unit) }
                }.collect { (period, dataTriple, _) ->
                    val (sales, payments, returns) = dataTriple
                    val (from, to) = period.toDateRange()

                    val grossRevenue = ledgerDao.getTotalRevenue(from, to)
                    val totalReturns = returnDao.getTotalReturns(from, to)
                    val revenue      = grossRevenue - totalReturns
                    val collected    = ledgerDao.getTotalCollected(from, to)
                    val outstanding  = ledgerDao.getOutstandingBalance()
                    val grossProfit  = ledgerDao.getGrossProfit(from, to)
                    val returnMargin = returnDao.getReturnGrossMargin(from, to)
                    val profit       = grossProfit - returnMargin

                    val entries: List<LedgerEntry> = buildList {
                        sales.mapTo(this)    { s -> s.toLedgerEntry() }
                        payments.mapTo(this) { p -> p.toLedgerEntry() }
                        returns.mapTo(this)  { r -> r.toLedgerEntry() }
                    }.sortedByDescending { it.timestampMillis }

                    _uiState.update { it.copy(
                        period             = period,
                        totalRevenue       = revenue,
                        totalCollected     = collected,
                        outstandingBalance = outstanding,
                        grossProfit        = profit,
                        entries            = entries,
                        isLoading          = false
                    )}
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun LedgerPeriod.toDateRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (this) {
            LedgerPeriod.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            LedgerPeriod.THIS_WEEK -> {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            LedgerPeriod.THIS_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            LedgerPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }
    }

    private fun SaleLedgerEntry.toLedgerEntry() = LedgerEntry.Sale(
        invoiceNumber  = invoiceNumber,
        resellerName   = resellerName,
        totalAmount    = totalAmount,
        amountPaid     = amountPaid,
        status         = status,
        timestampMillis = timestampMillis
    )

    private fun PaymentLedgerEntry.toLedgerEntry() = LedgerEntry.Payment(
        resellerName   = resellerName,
        amount         = amount,
        paymentMethod  = paymentMethod,
        timestampMillis = timestampMillis
    )

    private fun ReturnLedgerEntry.toLedgerEntry() = LedgerEntry.Return(
        returnNumber   = returnNumber,
        resellerName   = resellerName,
        totalAmount    = totalAmount,
        reason         = reason,
        timestampMillis = timestampMillis
    )
}
