package com.example.data.database

import androidx.room.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OcrDao {

    // Merchant Alias
    @Query("SELECT * FROM merchant_aliases")
    fun getAllAliases(): Flow<List<MerchantAlias>>

    @Query("SELECT * FROM merchant_aliases WHERE originalMerchant = :originalMerchant LIMIT 1")
    suspend fun getAliasForMerchant(originalMerchant: String): MerchantAlias?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: MerchantAlias)

    @Delete
    suspend fun deleteAlias(alias: MerchantAlias)


    // Merchant Corrections (Learning count)
    @Query("SELECT * FROM merchant_corrections WHERE originalMerchant = :originalMerchant AND correctedMerchant = :correctedMerchant LIMIT 1")
    suspend fun getCorrection(originalMerchant: String, correctedMerchant: String): MerchantCorrection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCorrection(correction: MerchantCorrection)

    @Query("DELETE FROM merchant_corrections WHERE originalMerchant = :originalMerchant")
    suspend fun deleteCorrectionsForMerchant(originalMerchant: String)


    // Category Learning
    @Query("SELECT * FROM category_learnings WHERE merchantName = :merchantName LIMIT 1")
    suspend fun getCategoryForMerchant(merchantName: String): CategoryLearning?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryLearning(learning: CategoryLearning)


    // Payment Learning
    @Query("SELECT * FROM payment_learnings WHERE merchantName = :merchantName LIMIT 1")
    suspend fun getPaymentForMerchant(merchantName: String): PaymentLearning?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentLearning(learning: PaymentLearning)


    // Bank Learning
    @Query("SELECT * FROM bank_learnings WHERE merchantName = :merchantName LIMIT 1")
    suspend fun getBankForMerchant(merchantName: String): BankLearning?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankLearning(learning: BankLearning)


    // OCR History
    @Query("SELECT * FROM ocr_histories ORDER BY importDate DESC")
    fun getAllOcrHistories(): Flow<List<OcrHistory>>

    @Query("SELECT * FROM ocr_histories WHERE id = :id LIMIT 1")
    suspend fun getOcrHistoryById(id: Int): OcrHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOcrHistory(history: OcrHistory): Long

    @Update
    suspend fun updateOcrHistory(history: OcrHistory)

    @Delete
    suspend fun deleteOcrHistory(history: OcrHistory)

    @Query("""
        SELECT * FROM ocr_histories 
        WHERE merchantName LIKE :query 
        OR bankName LIKE :query 
        OR transactionId LIKE :query 
        OR dateStr LIKE :query 
        OR CAST(amount AS TEXT) LIKE :query
        ORDER BY importDate DESC
    """)
    fun searchOcrHistory(query: String): Flow<List<OcrHistory>>
}
