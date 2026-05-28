package com.distributor.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.distributor.app.data.entity.ReturnDetailEntity
import com.distributor.app.data.entity.ReturnEntity
import kotlinx.coroutines.flow.Flow

data class ReturnLedgerEntry(
    val returnNumber: String,
    val resellerName: String,
    val totalAmount: Double,
    val reason: String,
    val timestampMillis: Long
)

@Dao
interface ReturnDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturn(returnEntity: ReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturnDetails(details: List<ReturnDetailEntity>)

    @Query("SELECT COALESCE(SUM(total_amount), 0.0) FROM returns WHERE created_at BETWEEN :from AND :to")
    suspend fun getTotalReturns(from: Long, to: Long): Double

    @Query("""
        SELECT COALESCE(SUM(rd.quantity * (rd.unit_price - p.base_price)), 0.0)
        FROM return_details rd
        JOIN products p ON rd.product_id = p.id
        JOIN returns r ON rd.return_id = r.id
        WHERE r.created_at BETWEEN :from AND :to
    """)
    suspend fun getReturnGrossMargin(from: Long, to: Long): Double

    @Query("""
        SELECT r.return_number AS returnNumber,
               res.name        AS resellerName,
               r.total_amount  AS totalAmount,
               r.reason        AS reason,
               r.created_at    AS timestampMillis
        FROM returns r
        JOIN resellers res ON r.reseller_id = res.id
        WHERE r.created_at BETWEEN :from AND :to
        ORDER BY r.created_at DESC
        LIMIT 200
    """)
    fun getReturnEntriesFlow(from: Long, to: Long): Flow<List<ReturnLedgerEntry>>
}
