package com.distributor.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PurchasePaymentEntry(
    val id: Long,
    val supplierName: String,
    val amount: Double,
    val paymentMethod: String,
    val notes: String,
    val purchaseDateMs: Long
)

@Dao
interface PurchasePaymentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(entity: com.distributor.app.data.entity.PurchasePaymentEntity): Long

    @Query("""
        SELECT pp.id,
               s.name         AS supplierName,
               pp.amount,
               pp.payment_method AS paymentMethod,
               pp.notes,
               pp.purchase_date  AS purchaseDateMs
        FROM purchase_payments pp
        JOIN suppliers s ON pp.supplier_id = s.id
        ORDER BY pp.purchase_date DESC
        LIMIT 200
    """)
    fun getPaymentsFlow(): Flow<List<PurchasePaymentEntry>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM purchase_payments WHERE purchase_date BETWEEN :from AND :to")
    suspend fun getTotalPurchased(from: Long, to: Long): Double
}
