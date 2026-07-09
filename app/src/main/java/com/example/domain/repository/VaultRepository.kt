package com.example.domain.repository

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

interface VaultRepository {
    // Legacy Vault Items
    fun getAllItems(): Flow<List<VaultItem>>
    suspend fun insertItem(item: VaultItem)
    suspend fun deleteItem(item: VaultItem)
    suspend fun clearAll()

    // Transactions
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTrashedTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Int): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransactionPermanently(transaction: Transaction)
    suspend fun autoPurgeTrash(threshold: Long)

    // Bank Accounts
    fun getAllAccounts(): Flow<List<BankAccount>>
    suspend fun getAccountById(id: Int): BankAccount?
    suspend fun insertAccount(account: BankAccount): Long
    suspend fun updateAccount(account: BankAccount)
    suspend fun deleteAccount(account: BankAccount)

    // Categories
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Int): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)

    // User Profile
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun getUserProfileOneShot(): UserProfile?
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    // Payment Methods
    fun getAllPaymentMethods(): Flow<List<PaymentMethod>>
    suspend fun getPaymentMethodById(id: Int): PaymentMethod?
    suspend fun insertPaymentMethod(method: PaymentMethod): Long
    suspend fun updatePaymentMethod(method: PaymentMethod)
    suspend fun deletePaymentMethod(method: PaymentMethod)

    // Balance Adjustments
    fun getAllAdjustments(): Flow<List<BalanceAdjustment>>
    suspend fun insertAdjustment(adjustment: BalanceAdjustment): Long
    suspend fun deleteAdjustment(adjustment: BalanceAdjustment)

    // Recurring Items
    fun getAllRecurringItems(): Flow<List<RecurringItem>>
    fun getRecurringItemsByType(type: String): Flow<List<RecurringItem>>
    suspend fun getRecurringItemById(id: Int): RecurringItem?
    suspend fun insertRecurringItem(item: RecurringItem): Long
    suspend fun updateRecurringItem(item: RecurringItem)
    suspend fun deleteRecurringItem(item: RecurringItem)

    // OCR & Learning Systems
    fun getAllAliases(): Flow<List<MerchantAlias>>
    suspend fun getAliasForMerchant(originalMerchant: String): MerchantAlias?
    suspend fun insertAlias(alias: MerchantAlias)
    suspend fun deleteAlias(alias: MerchantAlias)

    suspend fun getCorrection(originalMerchant: String, correctedMerchant: String): MerchantCorrection?
    suspend fun insertCorrection(correction: MerchantCorrection)
    suspend fun deleteCorrectionsForMerchant(originalMerchant: String)

    suspend fun getCategoryForMerchant(merchantName: String): CategoryLearning?
    suspend fun insertCategoryLearning(learning: CategoryLearning)

    suspend fun getPaymentForMerchant(merchantName: String): PaymentLearning?
    suspend fun insertPaymentLearning(learning: PaymentLearning)

    suspend fun getBankForMerchant(merchantName: String): BankLearning?
    suspend fun insertBankLearning(learning: BankLearning)

    fun getAllOcrHistories(): Flow<List<OcrHistory>>
    suspend fun getOcrHistoryById(id: Int): OcrHistory?
    suspend fun insertOcrHistory(history: OcrHistory): Long
    suspend fun updateOcrHistory(history: OcrHistory)
    suspend fun deleteOcrHistory(history: OcrHistory)
    fun searchOcrHistory(query: String): Flow<List<OcrHistory>>

    // Merchants
    fun getAllMerchants(): Flow<List<Merchant>>
    fun searchMerchants(query: String): Flow<List<Merchant>>
    suspend fun getMerchantById(id: Int): Merchant?
    suspend fun getMerchantByName(name: String): Merchant?
    suspend fun insertMerchant(merchant: Merchant): Long
    suspend fun updateMerchant(merchant: Merchant)
    suspend fun deleteMerchant(merchant: Merchant)

    // AI Memory
    suspend fun getAiMemoryForMerchant(merchantName: String): AiMemory?
    suspend fun insertAiMemory(aiMemory: AiMemory)
    fun getAllAiMemories(): Flow<List<AiMemory>>

    // Savings Goals
    fun getAllGoals(): Flow<List<SavingsGoal>>
    suspend fun getGoalById(id: Int): SavingsGoal?
    suspend fun insertGoal(goal: SavingsGoal): Long
    suspend fun updateGoal(goal: SavingsGoal)
    suspend fun deleteGoal(goal: SavingsGoal)

    // Private Vault
    fun getVaultStateFlow(): Flow<PrivateVaultState?>
    suspend fun getVaultStateOneShot(): PrivateVaultState?
    suspend fun insertOrUpdateVaultState(state: PrivateVaultState)
    fun getAllVaultHistory(): Flow<List<VaultHistoryItem>>
    suspend fun insertVaultHistoryItem(item: VaultHistoryItem): Long
}

