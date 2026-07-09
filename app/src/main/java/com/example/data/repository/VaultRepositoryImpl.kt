package com.example.data.repository

import com.example.data.database.VaultDao
import com.example.data.database.TransactionDao
import com.example.data.database.BankAccountDao
import com.example.data.database.CategoryDao
import com.example.data.database.UserProfileDao
import com.example.data.database.PaymentMethodDao
import com.example.data.database.BalanceAdjustmentDao
import com.example.data.database.RecurringItemDao
import com.example.data.database.OcrDao
import com.example.data.database.MerchantDao
import com.example.data.database.SavingsGoalDao
import com.example.data.database.PrivateVaultDao
import com.example.data.database.BorrowLendDao
import com.example.data.database.ReminderDao
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
import com.example.domain.model.BorrowLendItem
import com.example.domain.model.Reminder
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow

class VaultRepositoryImpl(
    private val vaultDao: VaultDao,
    private val transactionDao: TransactionDao,
    private val bankAccountDao: BankAccountDao,
    private val categoryDao: CategoryDao,
    private val userProfileDao: UserProfileDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val balanceAdjustmentDao: BalanceAdjustmentDao,
    private val recurringItemDao: RecurringItemDao,
    private val ocrDao: OcrDao,
    private val merchantDao: MerchantDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val privateVaultDao: PrivateVaultDao,
    private val borrowLendDao: BorrowLendDao,
    private val reminderDao: ReminderDao
) : VaultRepository {

    // Legacy Items
    override fun getAllItems(): Flow<List<VaultItem>> = vaultDao.getAllItems()
    override suspend fun insertItem(item: VaultItem) = vaultDao.insertItem(item)
    override suspend fun deleteItem(item: VaultItem) = vaultDao.deleteItem(item)
    override suspend fun clearAll() = vaultDao.clearAll()

    // Transactions
    override fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    override fun getTrashedTransactions(): Flow<List<Transaction>> = transactionDao.getTrashedTransactions()
    override suspend fun getTransactionById(id: Int): Transaction? = transactionDao.getTransactionById(id)
    override suspend fun insertTransaction(transaction: Transaction): Long = transactionDao.insertTransaction(transaction)
    override suspend fun updateTransaction(transaction: Transaction) = transactionDao.updateTransaction(transaction)
    override suspend fun deleteTransactionPermanently(transaction: Transaction) = transactionDao.deleteTransactionPermanently(transaction)
    override suspend fun autoPurgeTrash(threshold: Long) = transactionDao.autoPurgeTrash(threshold)

    // Bank Accounts
    override fun getAllAccounts(): Flow<List<BankAccount>> = bankAccountDao.getAllAccounts()
    override suspend fun getAccountById(id: Int): BankAccount? = bankAccountDao.getAccountById(id)
    override suspend fun insertAccount(account: BankAccount): Long = bankAccountDao.insertAccount(account)
    override suspend fun updateAccount(account: BankAccount) = bankAccountDao.updateAccount(account)
    override suspend fun deleteAccount(account: BankAccount) = bankAccountDao.deleteAccount(account)

    // Categories
    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    override suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)
    override suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    override suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    override suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // User Profile
    override fun getUserProfile(): Flow<UserProfile?> = userProfileDao.getUserProfile()
    override suspend fun getUserProfileOneShot(): UserProfile? = userProfileDao.getUserProfileOneShot()
    override suspend fun insertOrUpdateProfile(profile: UserProfile) = userProfileDao.insertOrUpdateProfile(profile)

    // Payment Methods
    override fun getAllPaymentMethods(): Flow<List<PaymentMethod>> = paymentMethodDao.getAllPaymentMethods()
    override suspend fun getPaymentMethodById(id: Int): PaymentMethod? = paymentMethodDao.getPaymentMethodById(id)
    override suspend fun insertPaymentMethod(method: PaymentMethod): Long = paymentMethodDao.insertPaymentMethod(method)
    override suspend fun updatePaymentMethod(method: PaymentMethod) = paymentMethodDao.updatePaymentMethod(method)
    override suspend fun deletePaymentMethod(method: PaymentMethod) = paymentMethodDao.deletePaymentMethod(method)

    // Balance Adjustments
    override fun getAllAdjustments(): Flow<List<BalanceAdjustment>> = balanceAdjustmentDao.getAllAdjustments()
    override suspend fun insertAdjustment(adjustment: BalanceAdjustment): Long = balanceAdjustmentDao.insertAdjustment(adjustment)
    override suspend fun deleteAdjustment(adjustment: BalanceAdjustment) = balanceAdjustmentDao.deleteAdjustment(adjustment)

    // Recurring Items
    override fun getAllRecurringItems(): Flow<List<RecurringItem>> = recurringItemDao.getAllRecurringItems()
    override fun getRecurringItemsByType(type: String): Flow<List<RecurringItem>> = recurringItemDao.getRecurringItemsByType(type)
    override suspend fun getRecurringItemById(id: Int): RecurringItem? = recurringItemDao.getRecurringItemById(id)
    override suspend fun insertRecurringItem(item: RecurringItem): Long = recurringItemDao.insertRecurringItem(item)
    override suspend fun updateRecurringItem(item: RecurringItem) = recurringItemDao.updateRecurringItem(item)
    override suspend fun deleteRecurringItem(item: RecurringItem) = recurringItemDao.deleteRecurringItem(item)

    // OCR & Learning Systems
    override fun getAllAliases(): Flow<List<MerchantAlias>> = ocrDao.getAllAliases()
    override suspend fun getAliasForMerchant(originalMerchant: String): MerchantAlias? = ocrDao.getAliasForMerchant(originalMerchant)
    override suspend fun insertAlias(alias: MerchantAlias) = ocrDao.insertAlias(alias)
    override suspend fun deleteAlias(alias: MerchantAlias) = ocrDao.deleteAlias(alias)

    override suspend fun getCorrection(originalMerchant: String, correctedMerchant: String): MerchantCorrection? = ocrDao.getCorrection(originalMerchant, correctedMerchant)
    override suspend fun insertCorrection(correction: MerchantCorrection) = ocrDao.insertCorrection(correction)
    override suspend fun deleteCorrectionsForMerchant(originalMerchant: String) = ocrDao.deleteCorrectionsForMerchant(originalMerchant)

    override suspend fun getCategoryForMerchant(merchantName: String): CategoryLearning? = ocrDao.getCategoryForMerchant(merchantName)
    override suspend fun insertCategoryLearning(learning: CategoryLearning) = ocrDao.insertCategoryLearning(learning)

    override suspend fun getPaymentForMerchant(merchantName: String): PaymentLearning? = ocrDao.getPaymentForMerchant(merchantName)
    override suspend fun insertPaymentLearning(learning: PaymentLearning) = ocrDao.insertPaymentLearning(learning)

    override suspend fun getBankForMerchant(merchantName: String): BankLearning? = ocrDao.getBankForMerchant(merchantName)
    override suspend fun insertBankLearning(learning: BankLearning) = ocrDao.insertBankLearning(learning)

    override fun getAllOcrHistories(): Flow<List<OcrHistory>> = ocrDao.getAllOcrHistories()
    override suspend fun getOcrHistoryById(id: Int): OcrHistory? = ocrDao.getOcrHistoryById(id)
    override suspend fun insertOcrHistory(history: OcrHistory): Long = ocrDao.insertOcrHistory(history)
    override suspend fun updateOcrHistory(history: OcrHistory) = ocrDao.updateOcrHistory(history)
    override suspend fun deleteOcrHistory(history: OcrHistory) = ocrDao.deleteOcrHistory(history)
    override fun searchOcrHistory(query: String): Flow<List<OcrHistory>> = ocrDao.searchOcrHistory("%$query%")

    // Merchants
    override fun getAllMerchants(): Flow<List<Merchant>> = merchantDao.getAllMerchants()
    override fun searchMerchants(query: String): Flow<List<Merchant>> = merchantDao.searchMerchants("%$query%")
    override suspend fun getMerchantById(id: Int): Merchant? = merchantDao.getMerchantById(id)
    override suspend fun getMerchantByName(name: String): Merchant? = merchantDao.getMerchantByName(name)
    override suspend fun insertMerchant(merchant: Merchant): Long = merchantDao.insertMerchant(merchant)
    override suspend fun updateMerchant(merchant: Merchant) = merchantDao.updateMerchant(merchant)
    override suspend fun deleteMerchant(merchant: Merchant) = merchantDao.deleteMerchant(merchant)

    // AI Memory
    override suspend fun getAiMemoryForMerchant(merchantName: String): AiMemory? = merchantDao.getAiMemoryByMerchant(merchantName)
    override suspend fun insertAiMemory(aiMemory: AiMemory) = merchantDao.insertAiMemory(aiMemory)
    override fun getAllAiMemories(): Flow<List<AiMemory>> = merchantDao.getAllAiMemories()

    // Savings Goals
    override fun getAllGoals(): Flow<List<SavingsGoal>> = savingsGoalDao.getAllGoals()
    override suspend fun getGoalById(id: Int): SavingsGoal? = savingsGoalDao.getGoalById(id)
    override suspend fun insertGoal(goal: SavingsGoal): Long = savingsGoalDao.insertGoal(goal)
    override suspend fun updateGoal(goal: SavingsGoal) = savingsGoalDao.updateGoal(goal)
    override suspend fun deleteGoal(goal: SavingsGoal) = savingsGoalDao.deleteGoal(goal)

    // Private Vault
    override fun getVaultStateFlow(): Flow<PrivateVaultState?> = privateVaultDao.getVaultStateFlow()
    override suspend fun getVaultStateOneShot(): PrivateVaultState? = privateVaultDao.getVaultStateOneShot()
    override suspend fun insertOrUpdateVaultState(state: PrivateVaultState) = privateVaultDao.insertOrUpdateState(state)
    override fun getAllVaultHistory(): Flow<List<VaultHistoryItem>> = privateVaultDao.getAllHistory()
    override suspend fun insertVaultHistoryItem(item: VaultHistoryItem): Long = privateVaultDao.insertHistoryItem(item)

    // Borrow & Lend
    override fun getAllBorrowLendItems(): Flow<List<BorrowLendItem>> = borrowLendDao.getAllBorrowLendItems()
    override suspend fun getBorrowLendItemById(id: Int): BorrowLendItem? = borrowLendDao.getBorrowLendItemById(id)
    override suspend fun insertBorrowLendItem(item: BorrowLendItem): Long = borrowLendDao.insertBorrowLendItem(item)
    override suspend fun updateBorrowLendItem(item: BorrowLendItem) = borrowLendDao.updateBorrowLendItem(item)
    override suspend fun deleteBorrowLendItem(item: BorrowLendItem) = borrowLendDao.deleteBorrowLendItem(item)

    // Reminders
    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()
    override suspend fun getReminderById(id: Int): Reminder? = reminderDao.getReminderById(id)
    override suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)
    override suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)
    override suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
}

