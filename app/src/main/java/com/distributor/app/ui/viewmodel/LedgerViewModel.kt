package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.dao.PaymentLedgerEntry
import com.distributor.app.data.dao.ProductResellerSales
import com.distributor.app.data.dao.ReturnLedgerEntry
import com.distributor.app.data.dao.SaleLedgerEntry
import com.distributor.app.data.dao.TopResellerEntry
import com.distributor.app.data.entity.TransactionEntity
import com.distributor.app.utils.LedgerPdfGenerator
import com.distributor.app.utils.LedgerReportData
import com.distributor.app.utils.LedgerReportEntry
import com.distributor.app.utils.PaymentAllocationLine
import com.distributor.app.utils.PaymentReceiptData
import com.distributor.app.utils.ReceiptData
import com.distributor.app.utils.ReceiptLineItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class LedgerPeriod { TODAY, THIS_WEEK, THIS_MONTH, ALL_TIME }

enum class ReportPeriod { TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL_TIME, CUSTOM }

sealed class LedgerPdfState {
    object Idle            : LedgerPdfState()
    object PeriodPicker    : LedgerPdfState()
    object DateRangePicker : LedgerPdfState()
    object Generating      : LedgerPdfState()
    data class ReadyToShare(val file: File) : LedgerPdfState()
}

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
        val id: Long,
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
    val totalPurchased: Double = 0.0,
    val topResellerOverall: TopResellerEntry? = null,
    val topPerProduct: List<ProductResellerSales> = emptyList(),
    val entries: List<LedgerEntry> = emptyList(),
    val isLoading: Boolean = true,
    val pdfState: LedgerPdfState = LedgerPdfState.Idle,
    val pendingReshareReceipt: ReceiptData? = null,
    val pendingResharePayment: PaymentReceiptData? = null
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
                    val grossProfit    = ledgerDao.getGrossProfit(from, to)
                    val returnMargin  = returnDao.getReturnGrossMargin(from, to)
                    val profit        = grossProfit - returnMargin
                    val totalPurchased = ledgerDao.getTotalPurchased(from, to)

                    val topResellerOverall = ledgerDao.getTopResellerOverall(from, to)
                    // Raw rows are ordered by product_id ASC, totalPurchase DESC —
                    // distinctBy keeps the first (highest) per product.
                    val topPerProduct = ledgerDao.getTopResellersPerProductRaw(from, to)
                        .distinctBy { it.productId }

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
                        totalPurchased     = totalPurchased,
                        topResellerOverall = topResellerOverall,
                        topPerProduct      = topPerProduct,
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
        id             = id,
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

    // ── PDF export ────────────────────────────────────────────────────────────

    fun onPrintFabClicked() {
        _uiState.update { it.copy(pdfState = LedgerPdfState.PeriodPicker) }
    }

    fun onPdfPeriodSelected(period: ReportPeriod) {
        if (period == ReportPeriod.CUSTOM) {
            _uiState.update { it.copy(pdfState = LedgerPdfState.DateRangePicker) }
            return
        }
        val (from, to) = period.toDateRange()
        startPdfGeneration(from, to, period.toPeriodLabel(from, to))
    }

    fun onPdfCustomRangeConfirmed(pickerStartUtcMillis: Long, pickerEndUtcMillis: Long) {
        val from  = pickerStartUtcMillis.toLocalStartOfDay()
        val to    = pickerEndUtcMillis.toLocalEndOfDay()
        val label = "${from.formatReportDate()} – ${to.formatReportDate()}"
        startPdfGeneration(from, to, label)
    }

    fun onPdfDialogDismissed() {
        _uiState.update { it.copy(pdfState = LedgerPdfState.Idle) }
    }

    fun onPdfShareDismissed() {
        _uiState.update { it.copy(pdfState = LedgerPdfState.Idle) }
    }

    private fun startPdfGeneration(from: Long, to: Long, periodLabel: String) {
        _uiState.update { it.copy(pdfState = LedgerPdfState.Generating) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ctx      = getApplication<Application>()
                val sales    = ledgerDao.getSaleEntriesFlow(from, to).first()
                val payments = ledgerDao.getPaymentEntriesFlow(from, to).first()
                val returns  = returnDao.getReturnEntriesFlow(from, to).first()

                val revenue      = ledgerDao.getTotalRevenue(from, to)
                val totalReturns = returnDao.getTotalReturns(from, to)
                val collected    = ledgerDao.getTotalCollected(from, to)
                val outstanding  = ledgerDao.getOutstandingBalance()
                val profit       = ledgerDao.getGrossProfit(from, to) - returnDao.getReturnGrossMargin(from, to)

                val reportEntries: List<LedgerReportEntry> = buildList {
                    sales.mapTo(this) { s ->
                        LedgerReportEntry(
                            timestampMillis = s.timestampMillis,
                            description     = "${s.invoiceNumber} – ${s.resellerName}",
                            typeLabel       = "JUAL",
                            amount          = s.totalAmount,
                            isCredit        = true
                        )
                    }
                    payments.mapTo(this) { p ->
                        val method = when (p.paymentMethod) {
                            "CASH"     -> "Tunai"
                            "TRANSFER" -> "Transfer"
                            else       -> "Lainnya"
                        }
                        LedgerReportEntry(
                            timestampMillis = p.timestampMillis,
                            description     = "${p.resellerName} ($method)",
                            typeLabel       = "BAYAR",
                            amount          = p.amount,
                            isCredit        = true
                        )
                    }
                    returns.mapTo(this) { r ->
                        LedgerReportEntry(
                            timestampMillis = r.timestampMillis,
                            description     = "${r.returnNumber} – ${r.resellerName}",
                            typeLabel       = "RETUR",
                            amount          = r.totalAmount,
                            isCredit        = false
                        )
                    }
                }.sortedByDescending { it.timestampMillis }

                val file = LedgerPdfGenerator(ctx).generate(
                    LedgerReportData(
                        periodLabel = periodLabel,
                        revenue     = revenue - totalReturns,
                        collected   = collected,
                        outstanding = outstanding,
                        grossProfit = profit,
                        entries     = reportEntries
                    )
                )
                _uiState.update { it.copy(pdfState = LedgerPdfState.ReadyToShare(file)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfState = LedgerPdfState.Idle) }
            }
        }
    }

    private fun ReportPeriod.toDateRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        return when (this) {
            ReportPeriod.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0);       set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.THIS_WEEK -> {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0);       set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.THIS_MONTH -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0);       set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.THIS_YEAR -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0);       set(Calendar.MILLISECOND, 0)
                }
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
            ReportPeriod.CUSTOM   -> Pair(0L, now) // handled separately
        }
    }

    private fun ReportPeriod.toPeriodLabel(from: Long, to: Long): String = when (this) {
        ReportPeriod.TODAY      -> "Hari Ini (${from.formatReportDate()})"
        ReportPeriod.THIS_WEEK  -> "Minggu Ini (${from.formatReportDate()} – ${to.formatReportDate()})"
        ReportPeriod.THIS_MONTH -> "Bulan Ini (${from.formatReportDate()} – ${to.formatReportDate()})"
        ReportPeriod.THIS_YEAR  -> "Tahun Ini (${from.formatReportDate()} – ${to.formatReportDate()})"
        ReportPeriod.ALL_TIME   -> "Semua Waktu"
        ReportPeriod.CUSTOM     -> "${from.formatReportDate()} – ${to.formatReportDate()}"
    }

    private fun Long.formatReportDate(): String =
        SimpleDateFormat("d MMM yyyy", Locale("in", "ID")).format(Date(this))

    private fun Long.toLocalStartOfDay(): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = this@toLocalStartOfDay }
        return Calendar.getInstance().apply {
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun Long.toLocalEndOfDay(): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = this@toLocalEndOfDay }
        return Calendar.getInstance().apply {
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    // ── Reshare invoice / payment receipt ────────────────────────────────────

    fun reshareInvoice(invoiceNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val header = ledgerDao.getInvoiceReceiptHeader(invoiceNumber) ?: return@launch
                val details = ledgerDao.getInvoiceDetailLines(header.transactionId)
                val receiptData = ReceiptData(
                    transaction = TransactionEntity(
                        id            = header.transactionId,
                        resellerId    = header.resellerId,
                        invoiceNumber = header.invoiceNumber,
                        status        = header.status,
                        totalAmount   = header.totalAmount,
                        amountPaid    = header.amountPaid,
                        notes         = header.notes,
                        createdAt     = header.createdAt,
                        updatedAt     = header.updatedAt
                    ),
                    resellerName    = header.resellerName,
                    resellerPhone   = header.resellerPhone,
                    resellerAddress = header.resellerAddress,
                    resellerEmail   = header.resellerEmail,
                    lineItems       = details.map { d ->
                        ReceiptLineItem(
                            productName = d.productName,
                            unit        = d.unit,
                            quantity    = d.quantity,
                            unitPrice   = d.unitPrice,
                            subtotal    = d.subtotal
                        )
                    }
                )
                _uiState.update { it.copy(pendingReshareReceipt = receiptData) }
            } catch (_: Exception) { /* silently ignore */ }
        }
    }

    fun resharePayment(paymentLogId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rows = ledgerDao.getPaymentGroupRows(paymentLogId)
                if (rows.isEmpty()) return@launch
                val first = rows.first()
                val paymentData = PaymentReceiptData(
                    resellerName    = first.resellerName,
                    resellerPhone   = first.resellerPhone,
                    resellerAddress = first.resellerAddress,
                    resellerEmail   = first.resellerEmail,
                    paymentAmount   = rows.sumOf { it.amount },
                    paymentMethod   = first.paymentMethod,
                    notes           = first.notes,
                    paymentDate     = first.createdAt,
                    allocations     = rows.mapNotNull { row ->
                        val invNumber = row.invoiceNumber ?: return@mapNotNull null
                        val invTotal  = row.invoiceTotal  ?: return@mapNotNull null
                        val invPaid   = row.invoiceAmountPaid ?: 0.0
                        PaymentAllocationLine(
                            invoiceNumber = invNumber,
                            invoiceTotal  = invTotal,
                            amountApplied = row.amount,
                            balanceAfter  = maxOf(0.0, invTotal - invPaid)
                        )
                    }
                )
                _uiState.update { it.copy(pendingResharePayment = paymentData) }
            } catch (_: Exception) { /* silently ignore */ }
        }
    }

    fun clearReshare() {
        _uiState.update { it.copy(pendingReshareReceipt = null, pendingResharePayment = null) }
    }
}
