package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.entity.StockLedgerEntity
import com.distributor.app.ui.model.OpnameRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StockOpnameUiState(
    val rows: List<OpnameRow> = emptyList(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null,
    val saveSuccessful: Boolean = false
)

class StockOpnameViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val productDao = database.productDao()
    private val stockLedgerDao = database.stockLedgerDao()

    private val _uiState = MutableStateFlow(StockOpnameUiState())
    val uiState: StateFlow<StockOpnameUiState> = _uiState.asStateFlow()

    init {
        observeOpnameData()
    }

    private fun observeOpnameData() {
        viewModelScope.launch {
            combine(
                productDao.getAllProducts(),
                stockLedgerDao.getAllCurrentStocksFlow()
            ) { products, stockSummaries ->
                val stockMap: Map<Long, Double> = stockSummaries.associate { it.productId to it.stock }
                products.map { product ->
                    val systemStock: Double = stockMap[product.id] ?: 0.0
                    val existing: OpnameRow? = _uiState.value.rows.find { it.product.id == product.id }
                    val physicalInput: String = existing?.physicalInput ?: ""
                    val physicalQty: Double? = physicalInput.toDoubleOrNull()
                    OpnameRow(
                        product = product,
                        systemStock = systemStock,
                        physicalInput = physicalInput,
                        discrepancy = if (physicalQty != null) physicalQty - systemStock else 0.0
                    )
                }
            }.collect { rows ->
                _uiState.update { it.copy(rows = rows, isLoading = false) }
            }
        }
    }

    fun onPhysicalInputChanged(productId: Long, input: String) {
        val updatedRows: List<OpnameRow> = _uiState.value.rows.map { row ->
            if (row.product.id != productId) return@map row
            val physicalQty: Double? = input.toDoubleOrNull()
            row.copy(
                physicalInput = input,
                discrepancy = if (physicalQty != null) physicalQty - row.systemStock else 0.0
            )
        }
        _uiState.update { it.copy(rows = updatedRows) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun resetSaveSuccessful() {
        _uiState.update { it.copy(saveSuccessful = false) }
    }

    fun saveOpname() {
        val rows: List<OpnameRow> = _uiState.value.rows
        val now: Long = System.currentTimeMillis()
        val adjustmentEntries = mutableListOf<StockLedgerEntity>()

        for (row in rows) {
            if (row.physicalInput.isBlank()) continue

            val physicalQty: Double = try {
                val parsed = row.physicalInput.toDouble()
                if (parsed < 0.0) throw NumberFormatException("negative value")
                parsed
            } catch (e: NumberFormatException) {
                _uiState.update { it.copy(
                    snackbarMessage = "Invalid quantity for \"${row.product.name}\""
                )}
                return
            }

            val discrepancy: Double = physicalQty - row.systemStock

            when {
                discrepancy > 0.001 -> adjustmentEntries.add(
                    StockLedgerEntity(
                        productId = row.product.id,
                        type = StockLedgerEntity.TYPE_ADJUST_IN,
                        quantity = discrepancy,
                        unitCost = null,
                        notes = "Stock opname",
                        createdAt = now
                    )
                )
                discrepancy < -0.001 -> adjustmentEntries.add(
                    StockLedgerEntity(
                        productId = row.product.id,
                        type = StockLedgerEntity.TYPE_ADJUST_OUT,
                        quantity = -discrepancy,
                        unitCost = null,
                        notes = "Stock opname",
                        createdAt = now
                    )
                )
            }
        }

        if (adjustmentEntries.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "No discrepancies found — stock counts match") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                stockLedgerDao.insertEntries(adjustmentEntries)
                _uiState.update { it.copy(
                    isSubmitting = false,
                    saveSuccessful = true,
                    snackbarMessage = "${adjustmentEntries.size} ledger adjustment(s) written"
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSubmitting = false,
                    snackbarMessage = "Failed to save adjustments: ${e.message}"
                )}
            }
        }
    }
}
