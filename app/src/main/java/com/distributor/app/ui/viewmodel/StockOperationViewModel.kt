package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.data.entity.StockLedgerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StockOperationUiState(
    val products: List<ProductEntity> = emptyList(),
    val selectedProduct: ProductEntity? = null,
    val currentStock: Double = 0.0,
    val typeSelection: String = StockLedgerEntity.TYPE_IN,
    val quantityInput: String = "",
    val unitCostInput: String = "",
    val notesInput: String = "",
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val isDatePickerOpen: Boolean = false,
    val quantityError: String? = null,
    val unitCostError: String? = null,
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null
)

class StockOperationViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val productDao = database.productDao()
    private val stockLedgerDao = database.stockLedgerDao()

    private val _uiState = MutableStateFlow(StockOperationUiState())
    val uiState: StateFlow<StockOperationUiState> = _uiState.asStateFlow()

    private val _selectedProductId = MutableStateFlow<Long?>(null)

    init {
        observeProducts()
        observeSelectedProductStock()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productDao.getAllProducts().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSelectedProductStock() {
        viewModelScope.launch {
            _selectedProductId
                .flatMapLatest { productId ->
                    if (productId == null) flowOf(0.0)
                    else stockLedgerDao.getCurrentStockFlow(productId)
                }
                .collect { stock ->
                    _uiState.update { it.copy(currentStock = stock) }
                }
        }
    }

    fun onProductSelected(product: ProductEntity) {
        _selectedProductId.value = product.id
        _uiState.update { it.copy(selectedProduct = product, quantityError = null) }
    }

    fun onTypeSelected(type: String) {
        _uiState.update { it.copy(typeSelection = type, unitCostError = null) }
    }

    fun onQuantityChanged(input: String) {
        _uiState.update { it.copy(quantityInput = input, quantityError = null) }
    }

    fun onUnitCostChanged(input: String) {
        _uiState.update { it.copy(unitCostInput = input, unitCostError = null) }
    }

    fun onNotesChanged(input: String) {
        _uiState.update { it.copy(notesInput = input) }
    }

    fun onDateSelected(millis: Long?) {
        _uiState.update { it.copy(
            selectedDateMillis = millis ?: System.currentTimeMillis(),
            isDatePickerOpen = false
        )}
    }

    fun openDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = true) }
    }

    fun closeDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = false) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun submitEntry() {
        val state = _uiState.value

        val product = state.selectedProduct ?: run {
            _uiState.update { it.copy(snackbarMessage = "Please select a product") }
            return
        }

        val quantity: Double = try {
            val parsed = state.quantityInput.toDouble()
            if (parsed <= 0.0) throw NumberFormatException("non-positive value")
            parsed
        } catch (e: NumberFormatException) {
            _uiState.update { it.copy(quantityError = "Enter a valid positive number") }
            return
        }

        val isInbound: Boolean = state.typeSelection in StockLedgerEntity.INBOUND_TYPES

        val unitCost: Double? = if (isInbound) {
            try {
                val parsed = state.unitCostInput.toDouble()
                if (parsed < 0.0) throw NumberFormatException("negative value")
                parsed
            } catch (e: NumberFormatException) {
                _uiState.update { it.copy(unitCostError = "Enter a valid unit cost") }
                return
            }
        } else {
            null
        }

        if (!isInbound && quantity > state.currentStock) {
            _uiState.update { it.copy(
                quantityError = "Exceeds available stock (${state.currentStock} ${product.unit})",
                snackbarMessage = "Insufficient stock for this operation"
            )}
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                stockLedgerDao.insertEntry(
                    StockLedgerEntity(
                        productId = product.id,
                        type = state.typeSelection,
                        quantity = quantity,
                        unitCost = unitCost,
                        notes = state.notesInput,
                        createdAt = state.selectedDateMillis
                    )
                )
                _uiState.update { it.copy(
                    isSubmitting = false,
                    quantityInput = "",
                    unitCostInput = "",
                    notesInput = "",
                    snackbarMessage = "Stock entry saved successfully"
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSubmitting = false,
                    snackbarMessage = "Failed to save entry: ${e.message}"
                )}
            }
        }
    }
}
