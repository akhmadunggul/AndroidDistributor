package com.distributor.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.distributor.app.data.entity.StockLedgerEntity
import com.distributor.app.data.model.ProductStockSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface StockLedgerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: StockLedgerEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntries(entries: List<StockLedgerEntity>)

    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN type IN ('IN', 'ADJUST_IN', 'RETURN_IN') THEN quantity ELSE -quantity END),
            0.0
        )
        FROM stock_ledger
        WHERE product_id = :productId
    """)
    fun getCurrentStockFlow(productId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN type IN ('IN', 'ADJUST_IN', 'RETURN_IN') THEN quantity ELSE -quantity END),
            0.0
        )
        FROM stock_ledger
        WHERE product_id = :productId
    """)
    suspend fun getCurrentStock(productId: Long): Double

    @Query("""
        SELECT p.id AS product_id,
            COALESCE(
                SUM(
                    CASE
                        WHEN sl.type IN ('IN', 'ADJUST_IN', 'RETURN_IN') THEN  sl.quantity
                        WHEN sl.type IN ('OUT', 'ADJUST_OUT')             THEN -sl.quantity
                        ELSE 0.0
                    END
                ),
                0.0
            ) AS stock
        FROM products p
        LEFT JOIN stock_ledger sl ON p.id = sl.product_id
        GROUP BY p.id
    """)
    fun getAllCurrentStocksFlow(): Flow<List<ProductStockSummary>>

    @Query("SELECT * FROM stock_ledger WHERE product_id = :productId ORDER BY created_at DESC")
    fun getEntriesForProduct(productId: Long): Flow<List<StockLedgerEntity>>
}
