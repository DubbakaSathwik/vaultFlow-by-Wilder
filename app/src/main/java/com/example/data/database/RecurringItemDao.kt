package com.example.data.database

import androidx.room.*
import com.example.domain.model.RecurringItem
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringItemDao {
    @Query("SELECT * FROM recurring_items ORDER BY isActive DESC, name ASC")
    fun getAllRecurringItems(): Flow<List<RecurringItem>>

    @Query("SELECT * FROM recurring_items WHERE type = :type ORDER BY isActive DESC, name ASC")
    fun getRecurringItemsByType(type: String): Flow<List<RecurringItem>>

    @Query("SELECT * FROM recurring_items WHERE id = :id LIMIT 1")
    suspend fun getRecurringItemById(id: Int): RecurringItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringItem(item: RecurringItem): Long

    @Update
    suspend fun updateRecurringItem(item: RecurringItem)

    @Delete
    suspend fun deleteRecurringItem(item: RecurringItem)

    @Query("DELETE FROM recurring_items")
    suspend fun clearAll()
}
