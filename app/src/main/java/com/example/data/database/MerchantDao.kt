package com.example.data.database

import androidx.room.*
import com.example.domain.model.Merchant
import com.example.domain.model.AiMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantDao {

    // Merchants
    @Query("SELECT * FROM merchants ORDER BY isFavorite DESC, visitCount DESC, storeName ASC")
    fun getAllMerchants(): Flow<List<Merchant>>

    @Query("SELECT * FROM merchants WHERE id = :id LIMIT 1")
    suspend fun getMerchantById(id: Int): Merchant?

    @Query("SELECT * FROM merchants WHERE merchantName = :name OR alias = :name OR storeName = :name LIMIT 1")
    suspend fun getMerchantByName(name: String): Merchant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: Merchant): Long

    @Update
    suspend fun updateMerchant(merchant: Merchant)

    @Delete
    suspend fun deleteMerchant(merchant: Merchant)

    @Query("""
        SELECT * FROM merchants 
        WHERE merchantName LIKE :query 
        OR alias LIKE :query 
        OR storeName LIKE :query 
        OR category LIKE :query 
        OR tags LIKE :query
        ORDER BY isFavorite DESC, visitCount DESC
    """)
    fun searchMerchants(query: String): Flow<List<Merchant>>

    @Query("DELETE FROM merchants")
    suspend fun clearAll()

    // AI Memories
    @Query("SELECT * FROM ai_memories WHERE merchantName = :merchantName LIMIT 1")
    suspend fun getAiMemoryByMerchant(merchantName: String): AiMemory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiMemory(aiMemory: AiMemory)

    @Query("SELECT * FROM ai_memories")
    fun getAllAiMemories(): Flow<List<AiMemory>>
}
