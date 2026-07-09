package com.example.data.database

import androidx.room.*
import com.example.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getTrashedTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Int): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransactionPermanently(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE isDeleted = 1 AND deletedTimestamp < :threshold")
    suspend fun autoPurgeTrash(threshold: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
