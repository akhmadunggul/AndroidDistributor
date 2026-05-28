package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.entity.PaymentLogEntity
import com.distributor.app.data.entity.ResellerEntity
import com.distributor.app.data.entity.TransactionEntity
import com.distributor.app.ui.model.AllocationPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class PaymentSettlementUiState(
    val resellers: List<ResellerEntity> = emptyList(),
    val selectedReseller: ResellerEntity? = null,
    val outstandingInvoices: List<TransactionEntity> = emptyList(),
    val outstandingBalance: Double = 0.0,
    val paymentAmountInput: String = "",
    val paymentMethod: String = PaymentLogEntity.METHOD_CASH,
    val notesInput: String = "",
    val allocationPreview: List<AllocationPreview> = emptyList(),
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null
)

class PaymentSettlementViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val resellerDao = database.resellerDao()
    private val transactionDao = database.transactionDao()
    private val paymentLogDao = database.paymentLogDao()

    private val _uiState = MutableStateFlow(PaymentSettlementUiState())
    val uiState: StateFlow<PaymentSettlementUiState> = _uiState.asStateFlow()

    private val _selectedResellerId = MutableStateFlow<Long?>(null)

    init {
        observeResellers()
        observeOutstandingInvoices()
    }

    private fun observeResellers() {
        viewModelScope.launch {
            resellerDao.getAllResellers().collect { resellers ->
                _uiState.update { it.copy(resellers = resellers) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeOutstandingInvoices() {
        viewModelScope.launch {
            _selectedResellerId
                .flatMapLatest { resellerId ->
                    if (resellerId == null) flowOf(emptyList())
                    else transactionDao.getOutstandingTransactionsFlow(resellerId)
                }
                .collect { invoices ->
                    val balance: Double = invoices.sumOf { it.totalAmount - it.amountPaid }
                    _uiState.update { it.copy(
                        outstandingInvoices = invoices,
                        outstandingBalance = balance
                    )}
                    refreshAllocationPreview()
                }
        }
    }

    fun onResellerSelected(reseller: ResellerEntity) {
        _selectedResellerId.value = reseller.id
        _uiState.update { it.copy(
            selectedReseller = reseller,
            paymentAmountInput = "",
            allocationPreview = emptyList()
        )}
    }

    fun onPaymentAmountChanged(input: String) {
        _uiState.update { it.copy(paymentAmountInput = input) }
        refreshAllocationPreview()
    }

    fun onPaymentMethodChanged(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onNotesChanged(input: String) {
        _uiState.update { it.copy(notesInput = input) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun refreshAllocationPreview() {
        val state = _uiState.value
        val paymentAmount: Double = state.paymentAmountInput.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(allocationPreview = emptyList()) }
            return
        }
        if (paymentAmount <= 0.0) {
            _uiState.update { it.copy(allocationPreview = emptyList()) }
            return
        }
        _uiState.update { it.copy(allocationPreview = buildAllocations(state.outstandingInvoices, paymentAmount)) }
    }

    // Pure function — no side effects. Used for both preview and the commit step.
    private fun buildAllocations(
        invoices: List<TransactionEntity>,
        paymentAmount: Double
    ): List<AllocationPreview> {
        val result = mutableListOf<AllocationPreview>()
        var remaining: Double = paymentAmount

        for (invoice in invoices) {
            if (remaining <= 0.0) break
            val balanceDue: Double = invoice.totalAmount - invoice.amountPaid
            val amountToApply: Double = minOf(remaining, balanceDue)
            result.add(AllocationPreview(
                invoice = invoice,
                amountApplied = amountToApply,
                balanceAfter = balanceDue - amountToApply
            ))
            remaining -= amountToApply
        }

        return result
    }

    fun settlePayment() {
        val state = _uiState.value

        val reseller: ResellerEntity = state.selectedReseller ?: run {
            _uiState.update { it.copy(snackbarMessage = "Please select a reseller") }
            return
        }

        val paymentAmount: Double = try {
            val parsed = state.paymentAmountInput.toDouble()
            if (parsed <= 0.0) throw NumberFormatException("non-positive value")
            parsed
        } catch (e: NumberFormatException) {
            _uiState.update { it.copy(snackbarMessage = "Enter a valid payment amount greater than zero") }
            return
        }

        if (state.outstandingInvoices.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "No outstanding invoices for this reseller") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Re-read inside the transaction for a consistent snapshot under concurrent writes
                val invoices: List<TransactionEntity> = transactionDao.getOutstandingTransactions(reseller.id)
                val allocations: List<AllocationPreview> = buildAllocations(invoices, paymentAmount)

                val paymentLogs = mutableListOf<PaymentLogEntity>()
                val updatedTransactions = mutableListOf<TransactionEntity>()

                for (alloc in allocations) {
                    val invoice: TransactionEntity = alloc.invoice
                    val newAmountPaid: Double = invoice.amountPaid + alloc.amountApplied
                    val newStatus: String = if (newAmountPaid >= invoice.totalAmount - 0.001) {
                        TransactionEntity.STATUS_PAID
                    } else {
                        TransactionEntity.STATUS_PARTIAL
                    }

                    paymentLogs.add(PaymentLogEntity(
                        resellerId = reseller.id,
                        transactionId = invoice.id,
                        amount = alloc.amountApplied,
                        paymentMethod = state.paymentMethod,
                        notes = state.notesInput
                    ))

                    updatedTransactions.add(invoice.copy(
                        amountPaid = newAmountPaid,
                        status = newStatus,
                        updatedAt = System.currentTimeMillis()
                    ))
                }

                database.withTransaction {
                    paymentLogDao.insertPaymentLogs(paymentLogs)
                    updatedTransactions.forEach { transactionDao.updateTransaction(it) }
                }

                val invoiceWord: String = if (paymentLogs.size == 1) "invoice" else "invoices"
                _uiState.update { it.copy(
                    isSubmitting = false,
                    paymentAmountInput = "",
                    notesInput = "",
                    allocationPreview = emptyList(),
                    snackbarMessage = "${formatAmount(paymentAmount)} settled across ${paymentLogs.size} $invoiceWord"
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSubmitting = false,
                    snackbarMessage = "Settlement failed: ${e.message}"
                )}
            }
        }
    }

    private fun formatAmount(amount: Double): String =
        String.format(Locale.getDefault(), "%.2f", amount)
}
