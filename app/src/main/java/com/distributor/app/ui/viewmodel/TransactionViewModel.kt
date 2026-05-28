package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.data.entity.ResellerEntity
import com.distributor.app.data.entity.StockLedgerEntity
import com.distributor.app.data.entity.TransactionDetailEntity
import com.distributor.app.data.entity.TransactionEntity
import com.distributor.app.ui.model.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransactionUiState(
    val resellers: List<ResellerEntity> = emptyList(),
    val selectedReseller: ResellerEntity? = null,
    val products: List<ProductEntity> = emptyList(),
    val stockMap: Map<Long, Double> = emptyMap(),
    val cartItems: List<CartItem> = emptyList(),
    val cartTotal: Double = 0.0,
    val selectedProduct: ProductEntity? = null,
    val quantityInput: String = "",
    val unitPriceInput: String = "",
    val overStockProductId: Long? = null,
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null,
    val lastCreatedInvoiceNumber: String? = null
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application)
    private val productDao = database.productDao()
    private val resellerDao = database.resellerDao()
    private val stockLedgerDao = database.stockLedgerDao()
    private val transactionDao = database.transactionDao()

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        observeResellers()
        observeProducts()
        observeAllStocks()
    }

    private fun observeResellers() {
        viewModelScope.launch {
            resellerDao.getAllResellers().collect { resellers ->
                _uiState.update { it.copy(resellers = resellers) }
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productDao.getAllProducts().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    private fun observeAllStocks() {
        viewModelScope.launch {
            stockLedgerDao.getAllCurrentStocksFlow().collect { summaries ->
                val map: Map<Long, Double> = summaries.associate { it.productId to it.stock }
                _uiState.update { it.copy(stockMap = map) }
            }
        }
    }

    fun onResellerSelected(reseller: ResellerEntity) {
        _uiState.update { it.copy(selectedReseller = reseller) }
    }

    fun onProductSelected(product: ProductEntity) {
        _uiState.update { it.copy(
            selectedProduct = product,
            unitPriceInput = product.sellingPrice.toString(),
            overStockProductId = null,
            quantityInput = ""
        )}
    }

    fun onQuantityChanged(input: String) {
        _uiState.update { it.copy(quantityInput = input, overStockProductId = null) }
    }

    fun onUnitPriceChanged(input: String) {
        _uiState.update { it.copy(unitPriceInput = input) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun acknowledgeInvoice() {
        _uiState.update { it.copy(lastCreatedInvoiceNumber = null) }
    }

    fun addToCart() {
        val state = _uiState.value

        val product: ProductEntity = state.selectedProduct ?: run {
            _uiState.update { it.copy(snackbarMessage = "Please select a product") }
            return
        }

        val quantity: Double = try {
            val parsed = state.quantityInput.toDouble()
            if (parsed <= 0.0) throw NumberFormatException("non-positive value")
            parsed
        } catch (e: NumberFormatException) {
            _uiState.update { it.copy(snackbarMessage = "Enter a valid quantity") }
            return
        }

        val unitPrice: Double = try {
            val parsed = state.unitPriceInput.toDouble()
            if (parsed < 0.0) throw NumberFormatException("negative value")
            parsed
        } catch (e: NumberFormatException) {
            _uiState.update { it.copy(snackbarMessage = "Enter a valid unit price") }
            return
        }

        val availableStock: Double = state.stockMap[product.id] ?: 0.0
        val alreadyInCart: Double = state.cartItems
            .filter { it.product.id == product.id }
            .sumOf { it.quantity }
        val totalRequested: Double = alreadyInCart + quantity

        if (totalRequested > availableStock) {
            val remaining = availableStock - alreadyInCart
            _uiState.update { it.copy(
                overStockProductId = product.id,
                snackbarMessage = "Insufficient stock: ${remaining.coerceAtLeast(0.0)} ${product.unit} available"
            )}
            return
        }

        val newItem = CartItem(
            product = product,
            quantity = quantity,
            unitPrice = unitPrice,
            subtotal = quantity * unitPrice
        )
        val updatedCart: List<CartItem> = state.cartItems + newItem

        _uiState.update { it.copy(
            cartItems = updatedCart,
            cartTotal = updatedCart.sumOf { it.subtotal },
            selectedProduct = null,
            quantityInput = "",
            unitPriceInput = "",
            overStockProductId = null
        )}
    }

    fun removeFromCart(item: CartItem) {
        val updatedCart: List<CartItem> = _uiState.value.cartItems - item
        _uiState.update { it.copy(
            cartItems = updatedCart,
            cartTotal = updatedCart.sumOf { it.subtotal }
        )}
    }

    fun submitTransaction() {
        val state = _uiState.value

        if (state.selectedReseller == null) {
            _uiState.update { it.copy(snackbarMessage = "Please select a reseller") }
            return
        }
        if (state.cartItems.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Cart is empty") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now: Long = System.currentTimeMillis()
                val invoiceNumber: String = buildInvoiceNumber(now)

                database.withTransaction {
                    val transactionId: Long = transactionDao.insertTransaction(
                        TransactionEntity(
                            resellerId = state.selectedReseller.id,
                            invoiceNumber = invoiceNumber,
                            status = TransactionEntity.STATUS_UNPAID,
                            totalAmount = state.cartTotal,
                            amountPaid = 0.0,
                            createdAt = now,
                            updatedAt = now
                        )
                    )

                    transactionDao.insertDetails(
                        state.cartItems.map { item ->
                            TransactionDetailEntity(
                                transactionId = transactionId,
                                productId = item.product.id,
                                quantity = item.quantity,
                                unitPrice = item.unitPrice,
                                subtotal = item.subtotal
                            )
                        }
                    )

                    stockLedgerDao.insertEntries(
                        state.cartItems.map { item ->
                            StockLedgerEntity(
                                productId = item.product.id,
                                type = StockLedgerEntity.TYPE_OUT,
                                quantity = item.quantity,
                                unitCost = null,
                                referenceTransactionId = transactionId,
                                notes = "Sale — $invoiceNumber",
                                createdAt = now
                            )
                        }
                    )
                }

                _uiState.update { it.copy(
                    isSubmitting = false,
                    cartItems = emptyList(),
                    cartTotal = 0.0,
                    selectedReseller = null,
                    lastCreatedInvoiceNumber = invoiceNumber,
                    snackbarMessage = "Invoice $invoiceNumber created"
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSubmitting = false,
                    snackbarMessage = "Failed to submit: ${e.message}"
                )}
            }
        }
    }

    private fun buildInvoiceNumber(millis: Long): String {
        val datePart: String = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(millis))
        val suffix: String = (millis % 10_000L).toString().padStart(4, '0')
        return "INV-$datePart-$suffix"
    }
}
