package com.distributor.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.distributor.app.data.AppDatabase
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.data.entity.ResellerEntity
import com.distributor.app.data.entity.ReturnDetailEntity
import com.distributor.app.data.entity.ReturnEntity
import com.distributor.app.data.entity.StockLedgerEntity
import com.distributor.app.data.entity.TransactionEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.distributor.app.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReturnCartItem(
    val product: ProductEntity,
    val quantity: Double,
    val unitPrice: Double
) {
    val subtotal: Double get() = quantity * unitPrice
}

data class ReturnUiState(
    val resellers: List<ResellerEntity> = emptyList(),
    val selectedReseller: ResellerEntity? = null,
    val products: List<ProductEntity> = emptyList(),
    val selectedProduct: ProductEntity? = null,
    val quantityInput: String = "",
    val unitPriceInput: String = "",
    val cartItems: List<ReturnCartItem> = emptyList(),
    val cartTotal: Double = 0.0,
    val reason: String = ReturnEntity.REASON_UNSOLD,
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val snackbarMessage: String? = null
)

class ReturnViewModel(application: Application) : AndroidViewModel(application) {

    private val database      = AppDatabase.getInstance(application)
    private val resellerDao   = database.resellerDao()
    private val productDao    = database.productDao()
    private val returnDao     = database.returnDao()
    private val stockLedgerDao = database.stockLedgerDao()
    private val transactionDao = database.transactionDao()

    private val _uiState = MutableStateFlow(ReturnUiState())
    val uiState: StateFlow<ReturnUiState> = _uiState.asStateFlow()

    init {
        observeResellers()
        observeProducts()
    }

    private fun observeResellers() {
        viewModelScope.launch {
            try {
                resellerDao.getAllResellers().collect { resellers ->
                    _uiState.update { it.copy(resellers = resellers) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Gagal memuat reseller: ${e.message}") }
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            try {
                productDao.getAllProducts().collect { products ->
                    _uiState.update { it.copy(products = products) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Gagal memuat produk: ${e.message}") }
            }
        }
    }

    fun onResellerSelected(reseller: ResellerEntity) {
        _uiState.update { it.copy(selectedReseller = reseller) }
    }

    fun onProductSelected(product: ProductEntity) {
        _uiState.update { it.copy(
            selectedProduct = product,
            unitPriceInput  = product.sellingPrice.toLong().toString(),
            quantityInput   = ""
        )}
    }

    fun onQuantityChanged(input: String) {
        _uiState.update { it.copy(quantityInput = input) }
    }

    fun onUnitPriceChanged(input: String) {
        _uiState.update { it.copy(unitPriceInput = input) }
    }

    fun onReasonChanged(reason: String) {
        _uiState.update { it.copy(reason = reason) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun addToCart() {
        val state = _uiState.value
        val product = state.selectedProduct ?: run {
            _uiState.update { it.copy(snackbarMessage = "Pilih produk terlebih dahulu") }
            return
        }
        val qty = state.quantityInput.toDoubleOrNull()
        if (qty == null || qty <= 0.0) {
            _uiState.update { it.copy(snackbarMessage = "Masukkan jumlah yang valid") }
            return
        }
        val price = state.unitPriceInput.toDoubleOrNull()
        if (price == null || price < 0.0) {
            _uiState.update { it.copy(snackbarMessage = "Masukkan harga yang valid") }
            return
        }
        val newItem = ReturnCartItem(product = product, quantity = qty, unitPrice = price)
        // Always add as a new line — merging silently changes the unit price of existing
        // quantity, producing a wrong subtotal. Multiple lines per product are fine.
        val updatedCart = state.cartItems + newItem
        val total = updatedCart.sumOf { it.subtotal }
        _uiState.update { it.copy(
            cartItems       = updatedCart,
            cartTotal       = total,
            selectedProduct = null,
            quantityInput   = "",
            unitPriceInput  = ""
        )}
    }

    fun removeFromCart(item: ReturnCartItem) {
        val updatedCart = _uiState.value.cartItems - item
        _uiState.update { it.copy(
            cartItems = updatedCart,
            cartTotal = updatedCart.sumOf { it.subtotal }
        )}
    }

    fun submitReturn() {
        val state = _uiState.value

        val reseller = state.selectedReseller ?: run {
            _uiState.update { it.copy(snackbarMessage = "Pilih reseller terlebih dahulu") }
            return
        }
        if (state.cartItems.isEmpty()) {
            _uiState.update { it.copy(snackbarMessage = "Keranjang retur kosong") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val datePart = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))
                val suffix = (now % 10000).toString().padStart(4, '0')
                val returnNumber = "RTN-$datePart-$suffix"

                val totalAmount = state.cartItems.sumOf { it.subtotal }

                val returnEntity = ReturnEntity(
                    resellerId   = reseller.id,
                    returnNumber = returnNumber,
                    totalAmount  = totalAmount,
                    reason       = state.reason,
                    notes        = state.notes,
                    createdAt    = now
                )

                val outstandingInvoices: List<TransactionEntity> =
                    transactionDao.getOutstandingTransactions(reseller.id)

                // Declare outside the transaction so the remaining credit is readable after commit
                var remainingCredit = totalAmount

                database.withTransaction {
                    val returnId = returnDao.insertReturn(returnEntity)

                    returnDao.insertReturnDetails(state.cartItems.map { item ->
                        ReturnDetailEntity(
                            returnId  = returnId,
                            productId = item.product.id,
                            quantity  = item.quantity,
                            unitPrice = item.unitPrice,
                            subtotal  = item.subtotal
                        )
                    })

                    stockLedgerDao.insertEntries(state.cartItems.map { item ->
                        StockLedgerEntity(
                            productId  = item.product.id,
                            type       = StockLedgerEntity.TYPE_RETURN_IN,
                            quantity   = item.quantity,
                            unitCost   = item.unitPrice,
                            notes      = "Retur $returnNumber",
                            createdAt  = now
                        )
                    })

                    // Apply return credit to oldest outstanding invoices (FIFO)
                    for (invoice in outstandingInvoices) {
                        if (remainingCredit <= 0.0) break
                        val balanceDue = invoice.totalAmount - invoice.amountPaid
                        val applied = minOf(remainingCredit, balanceDue)
                        val newPaid = invoice.amountPaid + applied
                        val newStatus = if (newPaid >= invoice.totalAmount - 0.001)
                            TransactionEntity.STATUS_PAID
                        else
                            TransactionEntity.STATUS_PARTIAL
                        transactionDao.updateTransaction(invoice.copy(
                            amountPaid = newPaid,
                            status     = newStatus,
                            updatedAt  = now
                        ))
                        remainingCredit -= applied
                    }
                }

                val successMsg = buildString {
                    append("Retur $returnNumber berhasil dicatat")
                    if (remainingCredit > 0.001) {
                        append(" — sisa ${formatRupiah(remainingCredit)} tidak dapat dialokasikan (tidak ada faktur tertunggak)")
                    }
                }

                _uiState.update { it.copy(
                    isSubmitting    = false,
                    selectedReseller = null,
                    selectedProduct = null,
                    cartItems       = emptyList(),
                    cartTotal       = 0.0,
                    quantityInput   = "",
                    unitPriceInput  = "",
                    reason          = ReturnEntity.REASON_UNSOLD,
                    notes           = "",
                    snackbarMessage = successMsg
                )}
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSubmitting    = false,
                    snackbarMessage = "Gagal menyimpan retur: ${e.message}"
                )}
            }
        }
    }
}
