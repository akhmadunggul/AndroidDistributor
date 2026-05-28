package com.distributor.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.distributor.app.data.entity.ResellerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResellerDao {

    @Query("SELECT * FROM resellers ORDER BY name ASC")
    fun getAllResellers(): Flow<List<ResellerEntity>>

    @Query("SELECT * FROM resellers WHERE id = :id")
    suspend fun getResellerById(id: Long): ResellerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReseller(reseller: ResellerEntity): Long

    @Update
    suspend fun updateReseller(reseller: ResellerEntity)

    @Delete
    suspend fun deleteReseller(reseller: ResellerEntity)
}
