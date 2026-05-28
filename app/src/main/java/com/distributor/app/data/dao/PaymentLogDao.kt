package com.distributor.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.distributor.app.data.entity.PaymentLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPaymentLog(log: PaymentLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPaymentLogs(logs: List<PaymentLogEntity>)

    @Query("SELECT * FROM payment_logs WHERE reseller_id = :resellerId ORDER BY created_at DESC")
    fun getPaymentsByResellerFlow(resellerId: Long): Flow<List<PaymentLogEntity>>
}
