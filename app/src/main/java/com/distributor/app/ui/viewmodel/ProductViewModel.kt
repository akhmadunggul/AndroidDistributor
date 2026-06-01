package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.dao.LowStockAlert
import com.distributor.app.data.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductListUiState(
    val products: List<ProductEntity> = emptyList(),
    val lowStockAlerts: List<LowStockAlert> = emptyList(),
    val isFormOpen: Boolean = false,
    val nameInput: String = "",
    val skuInput: String = "",
    val descriptionInput: String = "",
    val basePriceInput: String = "",
    val sellingPriceInput: String = "",
    val unitInput: String = "",
    val thresholdInput: String = "",
    val nameError: String? = null,
    val skuError: String? = null,
    val priceError: String? = null,
    val thresholdError: String? = null,
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null
)

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val productDao     = AppDatabase.getInstance(application).productDao()
    private val stockLedgerDao = AppDatabase.getInstance(application).stockLedgerDao()

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productDao.getAllProducts().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
        viewModelScope.launch {
            stockLedgerDao.getLowStockAlertsFlow().collect { alerts ->
                _uiState.update { it.copy(lowStockAlerts = alerts) }
            }
        }
    }

    fun openForm() {
        _uiState.update { it.copy(isFormOpen = true) }
    }

    fun closeForm() {
        _uiState.update {
            it.copy(
                isFormOpen = false,
                nameInput = "", skuInput = "", descriptionInput = "",
                basePriceInput = "", sellingPriceInput = "", unitInput = "",
                thresholdInput = "",
                nameError = null, skuError = null, priceError = null, thresholdError = null
            )
        }
    }

    fun onNameChanged(v: String)        = _uiState.update { it.copy(nameInput = v,         nameError      = null) }
    fun onSkuChanged(v: String)         = _uiState.update { it.copy(skuInput = v,          skuError       = null) }
    fun onDescriptionChanged(v: String) = _uiState.update { it.copy(descriptionInput = v) }
    fun onBasePriceChanged(v: String)   = _uiState.update { it.copy(basePriceInput = v,    priceError     = null) }
    fun onSellingPriceChanged(v: String)= _uiState.update { it.copy(sellingPriceInput = v, priceError     = null) }
    fun onUnitChanged(v: String)        = _uiState.update { it.copy(unitInput = v) }
    fun onThresholdChanged(v: String)   = _uiState.update { it.copy(thresholdInput = v,    thresholdError = null) }
    fun clearSnackbar()                 = _uiState.update { it.copy(snackbarMessage = null) }

    fun saveProduct() {
        val state = _uiState.value

        if (state.nameInput.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required") }
            return
        }
        if (state.skuInput.isBlank()) {
            _uiState.update { it.copy(skuError = "SKU is required") }
            return
        }

        val basePrice: Double = try {
            val p = state.basePriceInput.toDouble()
            if (p < 0.0) throw NumberFormatException("negative")
            p
        } catch (e: NumberFormatException) {
            _uiState.update { it.copy(priceError = "Enter a valid base price") }
            return
        }

        val sellingPrice: Double = try {
            val p = state.sellingPriceInput.toDouble()
            if (p < 0.0) throw NumberFormatException("negative")
            p
        } catch (e: NumberFormatException) {
            _uiState.update { it.copy(priceError = "Enter a valid selling price") }
            return
        }

        val unit: String = state.unitInput.trim().ifBlank { "pcs" }

        val threshold: Double? = if (state.thresholdInput.isBlank()) {
            null
        } else {
            try {
                val t = state.thresholdInput.toDouble()
                if (t < 0.0) throw NumberFormatException("negative")
                t
            } catch (e: NumberFormatException) {
                _uiState.update { it.copy(thresholdError = "Enter a valid number or leave blank") }
                return
            }
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                productDao.insertProduct(
                    ProductEntity(
                        name           = state.nameInput.trim(),
                        sku            = state.skuInput.trim().uppercase(),
                        description    = state.descriptionInput.trim(),
                        basePrice      = basePrice,
                        sellingPrice   = sellingPrice,
                        unit           = unit,
                        stockThreshold = threshold
                    )
                )
                val savedName = state.nameInput.trim()
                _uiState.update { it.copy(isSubmitting = false) }
                closeForm()
                _uiState.update { it.copy(snackbarMessage = "\"$savedName\" added") }
            } catch (e: Exception) {
                val msg = if (e.message?.contains("UNIQUE", ignoreCase = true) == true)
                    "SKU \"${state.skuInput.trim().uppercase()}\" already exists"
                else
                    "Failed to save: ${e.message}"
                _uiState.update { it.copy(isSubmitting = false, snackbarMessage = msg) }
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                productDao.deleteProduct(product)
                _uiState.update { it.copy(snackbarMessage = "\"${product.name}\" deleted") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Cannot delete: product has existing ledger or transaction entries") }
            }
        }
    }
}
