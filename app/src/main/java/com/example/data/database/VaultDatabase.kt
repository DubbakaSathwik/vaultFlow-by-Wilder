package com.example.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.domain.model.VaultItem
import com.example.domain.model.Transaction
import com.example.domain.model.BankAccount
import com.example.domain.model.Category
import com.example.domain.model.UserProfile
import com.example.domain.model.PaymentMethod
import com.example.domain.model.BalanceAdjustment
import com.example.domain.model.RecurringItem
import com.example.domain.model.MerchantAlias
import com.example.domain.model.MerchantCorrection
import com.example.domain.model.CategoryLearning
import com.example.domain.model.PaymentLearning
import com.example.domain.model.BankLearning
import com.example.domain.model.Merchant
import com.example.domain.model.AiMemory
import com.example.domain.model.OcrHistory
import com.example.domain.model.SavingsGoal
import com.example.domain.model.PrivateVaultState
import com.example.domain.model.VaultHistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItem)

    @Delete
    suspend fun deleteItem(item: VaultItem)
    
    @Query("DELETE FROM vault_items")
    suspend fun clearAll()
}

@Database(
    entities = [
        VaultItem::class,
        Transaction::class,
        BankAccount::class,
        Category::class,
        UserProfile::class,
        PaymentMethod::class,
        BalanceAdjustment::class,
        RecurringItem::class,
        MerchantAlias::class,
        MerchantCorrection::class,
        CategoryLearning::class,
        PaymentLearning::class,
        BankLearning::class,
        OcrHistory::class,
        Merchant::class,
        AiMemory::class,
        SavingsGoal::class,
        PrivateVaultState::class,
        VaultHistoryItem::class
    ],
    version = 6,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultDao(): VaultDao
    abstract fun transactionDao(): TransactionDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun balanceAdjustmentDao(): BalanceAdjustmentDao
    abstract fun recurringItemDao(): RecurringItemDao
    abstract fun ocrDao(): OcrDao
    abstract fun merchantDao(): MerchantDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun privateVaultDao(): PrivateVaultDao
}

