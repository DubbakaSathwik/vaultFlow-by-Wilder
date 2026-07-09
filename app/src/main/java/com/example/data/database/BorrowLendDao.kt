package com.example.data.database

import androidx.room.*
import com.example.domain.model.BorrowLendItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowLendDao {
    @Query("SELECT * FROM borrow_lend_items ORDER BY date DESC")
    fun getAllBorrowLendItems(): Flow<List<BorrowLendItem>>

    @Query("SELECT * FROM borrow_lend_items WHERE id = :id")
    suspend fun getBorrowLendItemById(id: Int): BorrowLendItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrowLendItem(item: BorrowLendItem): Long

    @Update
    suspend fun updateBorrowLendItem(item: BorrowLendItem)

    @Delete
    suspend fun deleteBorrowLendItem(item: BorrowLendItem)
}
