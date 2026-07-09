package com.example.data.database

import androidx.room.*
import com.example.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY isFavorite DESC, nickname ASC")
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>

    @Query("SELECT * FROM payment_methods WHERE id = :id LIMIT 1")
    suspend fun getPaymentMethodById(id: Int): PaymentMethod?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethod(method: PaymentMethod): Long

    @Update
    suspend fun updatePaymentMethod(method: PaymentMethod)

    @Delete
    suspend fun deletePaymentMethod(method: PaymentMethod)

    @Query("DELETE FROM payment_methods")
    suspend fun clearAll()
}
