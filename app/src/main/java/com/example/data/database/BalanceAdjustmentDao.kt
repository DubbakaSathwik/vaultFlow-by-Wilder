package com.example.data.database

import androidx.room.*
import com.example.domain.model.BalanceAdjustment
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceAdjustmentDao {
    @Query("SELECT * FROM balance_adjustments ORDER BY timestamp DESC")
    fun getAllAdjustments(): Flow<List<BalanceAdjustment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: BalanceAdjustment): Long

    @Delete
    suspend fun deleteAdjustment(adjustment: BalanceAdjustment)

    @Query("DELETE FROM balance_adjustments")
    suspend fun clearAll()
}
