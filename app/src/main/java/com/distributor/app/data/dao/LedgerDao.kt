package com.distributor.app.data.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class SaleLedgerEntry(
    val invoiceNumber: String,
    val resellerName: String,
    val totalAmount: Double,
    val amountPaid: Double,
    val status: String,
    val timestampMillis: Long
)

data class PaymentLedgerEntry(
    val amount: Double,
    val resellerName: String,
    val paymentMethod: String,
    val timestampMillis: Long
)

data class TopResellerEntry(
    val resellerName: String,
    val totalPurchase: Double
)

data class TopProductEntry(
    val productName: String,
    val totalRevenue: Double
)

data class ProductResellerSales(
    val productId: Long,
    val productName: String,
    val resellerName: String,
    val totalPurchase: Double
)

@Dao
interface LedgerDao {

    @Query("SELECT COALESCE(SUM(total_amount), 0.0) FROM transactions WHERE created_at BETWEEN :from AND :to")
    suspend fun getTotalRevenue(from: Long, to: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payment_logs WHERE created_at BETWEEN :from AND :to")
    suspend fun getTotalCollected(from: Long, to: Long): Double

    @Query("SELECT COALESCE(SUM(total_amount - amount_paid), 0.0) FROM transactions WHERE status != 'PAID'")
    suspend fun getOutstandingBalance(): Double

    @Query("""
        SELECT COALESCE(SUM(td.quantity * (td.unit_price - p.base_price)), 0.0)
        FROM transaction_details td
        JOIN products p ON td.product_id = p.id
        JOIN transactions t ON td.transaction_id = t.id
        WHERE t.created_at BETWEEN :from AND :to
    """)
    suspend fun getGrossProfit(from: Long, to: Long): Double

    @Query("""
        SELECT t.invoice_number AS invoiceNumber,
               r.name           AS resellerName,
               t.total_amount   AS totalAmount,
               t.amount_paid    AS amountPaid,
               t.status         AS status,
               t.created_at     AS timestampMillis
        FROM transactions t
        JOIN resellers r ON t.reseller_id = r.id
        WHERE t.created_at BETWEEN :from AND :to
        ORDER BY t.created_at DESC
        LIMIT 200
    """)
    fun getSaleEntriesFlow(from: Long, to: Long): Flow<List<SaleLedgerEntry>>

    @Query("""
        SELECT pl.amount         AS amount,
               r.name            AS resellerName,
               pl.payment_method AS paymentMethod,
               pl.created_at     AS timestampMillis
        FROM payment_logs pl
        JOIN resellers r ON pl.reseller_id = r.id
        WHERE pl.created_at BETWEEN :from AND :to
        ORDER BY pl.created_at DESC
        LIMIT 200
    """)
    fun getPaymentEntriesFlow(from: Long, to: Long): Flow<List<PaymentLedgerEntry>>

    @Query("""
        SELECT r.name AS resellerName,
               COALESCE(SUM(td.quantity * td.unit_price), 0.0) AS totalPurchase
        FROM transaction_details td
        JOIN transactions t ON td.transaction_id = t.id
        JOIN resellers r ON t.reseller_id = r.id
        WHERE t.created_at BETWEEN :from AND :to
        GROUP BY t.reseller_id
        ORDER BY SUM(td.quantity * td.unit_price) DESC
        LIMIT 1
    """)
    suspend fun getTopResellerOverall(from: Long, to: Long): TopResellerEntry?

    @Query("""
        SELECT p.id   AS productId,
               p.name AS productName,
               r.name AS resellerName,
               COALESCE(SUM(td.quantity * td.unit_price), 0.0) AS totalPurchase
        FROM transaction_details td
        JOIN transactions t ON td.transaction_id = t.id
        JOIN resellers r ON t.reseller_id = r.id
        JOIN products p ON td.product_id = p.id
        WHERE t.created_at BETWEEN :from AND :to
        GROUP BY td.product_id, t.reseller_id
        ORDER BY td.product_id ASC, SUM(td.quantity * td.unit_price) DESC
    """)
    suspend fun getTopResellersPerProductRaw(from: Long, to: Long): List<ProductResellerSales>

    @Query("""
        SELECT p.name AS productName,
               COALESCE(SUM(td.quantity * td.unit_price), 0.0) AS totalRevenue
        FROM transaction_details td
        JOIN transactions t ON td.transaction_id = t.id
        JOIN products p ON td.product_id = p.id
        WHERE t.created_at BETWEEN :from AND :to
        GROUP BY td.product_id
        ORDER BY SUM(td.quantity * td.unit_price) DESC
        LIMIT 5
    """)
    suspend fun getTopProducts(from: Long, to: Long): List<TopProductEntry>

    @Query("SELECT COUNT(*) FROM transactions WHERE created_at BETWEEN :from AND :to")
    suspend fun getTransactionCount(from: Long, to: Long): Int

}
