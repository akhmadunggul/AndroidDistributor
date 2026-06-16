package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.dao.PurchasePaymentEntry
import com.distributor.app.data.entity.PurchasePaymentEntity
import com.distributor.app.data.entity.SupplierEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PurchaseUiState(
    val suppliers: List<SupplierEntity> = emptyList(),
    val selectedSupplier: SupplierEntity? = null,
    val payments: List<PurchasePaymentEntry> = emptyList(),
    val amountInput: String = "",
    val paymentMethod: String = "CASH",
    val notesInput: String = "",
    val purchaseDateMs: Long = System.currentTimeMillis(),
    val showDatePicker: Boolean = false,
    val amountError: String? = null,
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null
)

class PurchaseViewModel(application: Application) : AndroidViewModel(application) {

    private val db               = AppDatabase.getInstance(application)
    private val supplierDao      = db.supplierDao()
    private val purchaseDao      = db.purchasePaymentDao()

    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            supplierDao.getAllSuppliers().collect { list ->
                _uiState.update { it.copy(suppliers = list) }
            }
        }
        viewModelScope.launch {
            purchaseDao.getPaymentsFlow().collect { list ->
                _uiState.update { it.copy(payments = list) }
            }
        }
    }

    fun onSupplierSelected(supplier: SupplierEntity) =
        _uiState.update { it.copy(selectedSupplier = supplier) }

    fun onAmountChanged(v: String) =
        _uiState.update { it.copy(amountInput = v, amountError = null) }

    fun onPaymentMethodChanged(v: String) =
        _uiState.update { it.copy(paymentMethod = v) }

    fun onNotesChanged(v: String) =
        _uiState.update { it.copy(notesInput = v) }

    fun onShowDatePicker() =
        _uiState.update { it.copy(showDatePicker = true) }

    fun onDateSelected(ms: Long) =
        _uiState.update { it.copy(purchaseDateMs = ms, showDatePicker = false) }

    fun onDatePickerDismissed() =
        _uiState.update { it.copy(showDatePicker = false) }

    fun clearSnackbar() =
        _uiState.update { it.copy(snackbarMessage = null) }

    fun recordPayment() {
        val state = _uiState.value
        val supplier = state.selectedSupplier
        if (supplier == null) {
            _uiState.update { it.copy(snackbarMessage = "Please select a supplier") }
            return
        }
        val amount = state.amountInput.replace(",", ".").toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(amountError = "Enter a valid amount") }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                purchaseDao.insertPayment(
                    PurchasePaymentEntity(
                        supplierId    = supplier.id,
                        amount        = amount,
                        paymentMethod = state.paymentMethod,
                        notes         = state.notesInput.trim(),
                        purchaseDate  = state.purchaseDateMs,
                        createdAt     = System.currentTimeMillis()
                    )
                )
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        amountInput = "",
                        notesInput = "",
                        purchaseDateMs = System.currentTimeMillis(),
                        snackbarMessage = "Payment to ${supplier.name} recorded"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, snackbarMessage = "Failed: ${e.message}") }
            }
        }
    }
}
