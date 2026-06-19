package com.distributor.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.distributor.app.data.entity.ProductEntity
import com.distributor.app.data.entity.TransactionDetailEntity
import com.distributor.app.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDetails(details: List<TransactionDetailEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    // Flow variant — drives the settlement screen's live invoice list
    @Query("SELECT * FROM transactions WHERE reseller_id = :resellerId AND status != 'PAID' ORDER BY created_at ASC")
    fun getOutstandingTransactionsFlow(resellerId: Long): Flow<List<TransactionEntity>>

    // Suspend variant — used inside the FIFO algorithm for a consistent snapshot
    @Query("SELECT * FROM transactions WHERE reseller_id = :resellerId AND status != 'PAID' ORDER BY created_at ASC")
    suspend fun getOutstandingTransactions(resellerId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE reseller_id = :resellerId ORDER BY created_at DESC")
    fun getAllTransactionsByResellerFlow(resellerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_details WHERE transaction_id = :transactionId")
    fun getDetailsByTransactionFlow(transactionId: Long): Flow<List<TransactionDetailEntity>>

    @Query("""
        SELECT DISTINCT p.*
        FROM products p
        JOIN transaction_details td ON td.product_id = p.id
        JOIN transactions t ON td.transaction_id = t.id
        WHERE t.reseller_id = :resellerId
        ORDER BY p.name ASC
    """)
    fun getProductsByResellerFlow(resellerId: Long): Flow<List<ProductEntity>>
}
