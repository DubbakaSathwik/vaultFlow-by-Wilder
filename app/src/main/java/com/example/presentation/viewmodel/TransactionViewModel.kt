package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.BankAccount
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption {
    NEWEST, OLDEST, HIGHEST_AMOUNT, LOWEST_AMOUNT, CATEGORY, MERCHANT
}

data class TransactionFilter(
    val selectedType: String? = null, // "Expense", "Income", "Transfer", etc.
    val selectedCategoryId: Int? = null,
    val selectedBankAccountId: Int? = null,
    val selectedPaymentMethod: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val onlyFavorites: Boolean = false,
    val selectedTag: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

class TransactionViewModel(private val repository: VaultRepository) : ViewModel() {

    // Database Flows
    val allTransactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedTransactions: StateFlow<List<Transaction>> = repository.getTrashedTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<BankAccount>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state flows
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    // Last deleted transaction for Undo action
    private var lastDeletedTransaction: Transaction? = null

    // Filtered & Sorted Transactions
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _searchQuery,
        _filter,
        _sortOption
    ) { transactions, query, filterState, sort ->
        var list = transactions

        // Apply Search
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.merchantName.lowercase().contains(q) ||
                it.merchantAlias.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q) ||
                it.notes.lowercase().contains(q) ||
                it.tags.lowercase().contains(q) ||
                it.paymentMethod.lowercase().contains(q) ||
                it.amount.toString().contains(q) ||
                it.id.toString().contains(q)
            }
        }

        // Apply Type Filter
        if (filterState.selectedType != null) {
            list = list.filter { it.type.equals(filterState.selectedType, ignoreCase = true) }
        }

        // Apply Category Filter
        if (filterState.selectedCategoryId != null) {
            list = list.filter { it.categoryId == filterState.selectedCategoryId }
        }

        // Apply Account/Bank Filter
        if (filterState.selectedBankAccountId != null) {
            list = list.filter { it.bankAccountId == filterState.selectedBankAccountId }
        }

        // Apply Payment Method Filter
        if (filterState.selectedPaymentMethod != null) {
            list = list.filter { it.paymentMethod.equals(filterState.selectedPaymentMethod, ignoreCase = true) }
        }

        // Apply Amount range
        if (filterState.minAmount != null) {
            list = list.filter { it.amount >= filterState.minAmount }
        }
        if (filterState.maxAmount != null) {
            list = list.filter { it.amount <= filterState.maxAmount }
        }

        // Apply Favorites
        if (filterState.onlyFavorites) {
            list = list.filter { it.isFavorite }
        }

        // Apply Tag
        if (filterState.selectedTag != null) {
            list = list.filter { it.tagList.contains(filterState.selectedTag) }
        }

        // Apply Date range
        if (filterState.startDate != null) {
            list = list.filter { it.timestamp >= filterState.startDate }
        }
        if (filterState.endDate != null) {
            list = list.filter { it.timestamp <= filterState.endDate }
        }

        // Apply Sorting
        when (sort) {
            SortOption.NEWEST -> list.sortedByDescending { it.timestamp }
            SortOption.OLDEST -> list.sortedBy { it.timestamp }
            SortOption.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
            SortOption.LOWEST_AMOUNT -> list.sortedBy { it.amount }
            SortOption.CATEGORY -> list.sortedBy { it.categoryName }
            SortOption.MERCHANT -> list.sortedBy { it.merchantName }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedInitialDataIfNeeded()
        autoPurgeOldTrash()
    }

    private fun seedInitialDataIfNeeded() {
        viewModelScope.launch {
            // Seed Categories if empty
            val currentCategories = repository.getAllCategories().first()
            if (currentCategories.isEmpty()) {
                val defaultCategories = listOf(
                    Category(name = "Food", colorHex = "#EF4444", iconName = "restaurant", isFavorite = true),
                    Category(name = "Shopping", colorHex = "#EC4899", iconName = "shopping_bag", isFavorite = true),
                    Category(name = "College", colorHex = "#3B82F6", iconName = "school", isFavorite = true),
                    Category(name = "Stationery", colorHex = "#06B6D4", iconName = "edit", isFavorite = false),
                    Category(name = "Medical", colorHex = "#10B981", iconName = "medical_services", isFavorite = true),
                    Category(name = "Fuel", colorHex = "#F97316", iconName = "local_gas_station", isFavorite = false),
                    Category(name = "Entertainment", colorHex = "#8B5CF6", iconName = "movie", isFavorite = true),
                    Category(name = "Family", colorHex = "#78350F", iconName = "people", isFavorite = false),
                    Category(name = "Travel", colorHex = "#6366F1", iconName = "flight", isFavorite = false),
                    Category(name = "Recharge", colorHex = "#0284C7", iconName = "phone_android", isFavorite = false),
                    Category(name = "Bills", colorHex = "#F59E0B", iconName = "receipt_long", isFavorite = true),
                    Category(name = "Others", colorHex = "#6B7280", iconName = "category", isFavorite = false)
                )
                defaultCategories.forEach { repository.insertCategory(it) }
            }

            // Seed Bank Accounts if empty
            val currentAccounts = repository.getAllAccounts().first()
            if (currentAccounts.isEmpty()) {
                val defaultAccounts = listOf(
                    BankAccount(bankName = "Cash", nickname = "Cash Wallet", last4Digits = "0000", balance = 4200.0, colorHex = "#10B981", logoName = "wallet"),
                    BankAccount(bankName = "SBI Savings", nickname = "Primary SBI", last4Digits = "1234", balance = 45000.0, colorHex = "#0284C7", logoName = "account_balance"),
                    BankAccount(bankName = "HDFC Bank", nickname = "HDFC Salary", last4Digits = "5678", balance = 62000.0, colorHex = "#4F46E5", logoName = "account_balance"),
                    BankAccount(bankName = "PhonePe Wallet", nickname = "PhonePe Wallet", last4Digits = "9999", balance = 3500.0, colorHex = "#8B5CF6", logoName = "account_balance_wallet"),
                    BankAccount(bankName = "Paytm Wallet", nickname = "Paytm Wallet", last4Digits = "8888", balance = 2150.0, colorHex = "#0EA5E9", logoName = "account_balance_wallet"),
                    BankAccount(bankName = "FamPay Pocket", nickname = "FamPay Student", last4Digits = "7777", balance = 8000.0, colorHex = "#34D399", logoName = "account_balance_wallet")
                )
                defaultAccounts.forEach { repository.insertAccount(it) }
            }
        }
    }

    private fun autoPurgeOldTrash() {
        viewModelScope.launch {
            // Purge trashed transactions older than 30 days
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            repository.autoPurgeTrash(thirtyDaysAgo)
        }
    }

    // Setters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(newFilter: TransactionFilter) {
        _filter.value = newFilter
    }

    fun clearFilters() {
        _filter.value = TransactionFilter()
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    // Transaction Actions
    fun saveTransaction(transaction: Transaction, originalTransaction: Transaction? = null) {
        viewModelScope.launch {
            // Adjust balances based on transaction save (new vs update)
            if (originalTransaction != null) {
                // Revert original transaction impact
                revertAccountImpact(originalTransaction)
                // Apply new impact
                applyAccountImpact(transaction)
                repository.updateTransaction(transaction)
            } else {
                // Insert and apply impact
                applyAccountImpact(transaction)
                repository.insertTransaction(transaction)
            }
        }
    }

    fun deleteTransactionToTrash(transaction: Transaction) {
        viewModelScope.launch {
            // Update transaction to deleted
            val deletedTransaction = transaction.copy(
                isDeleted = true,
                deletedTimestamp = System.currentTimeMillis()
            )
            repository.updateTransaction(deletedTransaction)
            // Save for undo
            lastDeletedTransaction = transaction
            // Revert impact on balance
            revertAccountImpact(transaction)
        }
    }

    fun undoLastDelete() {
        val lastDeleted = lastDeletedTransaction ?: return
        viewModelScope.launch {
            // Restore transaction
            val restored = lastDeleted.copy(isDeleted = false, deletedTimestamp = null)
            repository.updateTransaction(restored)
            // Re-apply balance impact
            applyAccountImpact(restored)
            lastDeletedTransaction = null
        }
    }

    fun duplicateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val duplicated = transaction.copy(
                id = 0, // Generate new ID
                timestamp = System.currentTimeMillis(),
                isFavorite = false
            )
            repository.insertTransaction(duplicated)
            applyAccountImpact(duplicated)
        }
    }

    fun toggleTransactionFavorite(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(isFavorite = !transaction.isFavorite))
        }
    }

    fun restoreFromTrash(transaction: Transaction) {
        viewModelScope.launch {
            val restored = transaction.copy(isDeleted = false, deletedTimestamp = null)
            repository.updateTransaction(restored)
            applyAccountImpact(restored)
        }
    }

    fun deletePermanently(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransactionPermanently(transaction)
        }
    }

    // Balance impact calculators
    private suspend fun applyAccountImpact(transaction: Transaction) {
        val account = repository.getAccountById(transaction.bankAccountId) ?: return
        val newBalance = when (transaction.type) {
            "Income", "Refund", "Borrow" -> account.balance + transaction.amount
            "Expense", "Lend" -> account.balance - transaction.amount
            "Adjustment" -> transaction.amount // Adjustment sets balance directly or acts as expense/income. Let's make adjustment acts as direct balance override or addition? Let's make adjustment set the balance directly or acts as a delta. Let's assume adjustment is a set or delta. Let's assume adjustment adds/subtracts if we want, or sets directly. The prompt says "Adjustment". Let's treat Adjustment as setting the balance directly to the amount for this account if it's an adjustment type! Yes! "Adjustment sets the balance directly". That's awesome.
            "Transfer" -> account.balance - transaction.amount // Transfer out of this account
            else -> account.balance
        }
        repository.updateAccount(account.copy(balance = newBalance))

        // If transfer, we also need to handle transfer-in account!
        // We can parse transfer-to account nickname/name from merchantName or custom logic.
        // Let's check if the transfer has a destination account.
        if (transaction.type == "Transfer") {
            // Find account with name/nickname matching transaction.merchantName
            val destAccount = repository.getAllAccounts().first().find {
                it.nickname.equals(transaction.merchantName, ignoreCase = true) ||
                it.bankName.equals(transaction.merchantName, ignoreCase = true)
            }
            if (destAccount != null) {
                repository.updateAccount(destAccount.copy(balance = destAccount.balance + transaction.amount))
            }
        }
    }

    private suspend fun revertAccountImpact(transaction: Transaction) {
        val account = repository.getAccountById(transaction.bankAccountId) ?: return
        val revertedBalance = when (transaction.type) {
            "Income", "Refund", "Borrow" -> account.balance - transaction.amount
            "Expense", "Lend" -> account.balance + transaction.amount
            "Transfer" -> account.balance + transaction.amount // Revert transfer out
            else -> account.balance
        }
        repository.updateAccount(account.copy(balance = revertedBalance))

        // If transfer, revert destination account impact too
        if (transaction.type == "Transfer") {
            val destAccount = repository.getAllAccounts().first().find {
                it.nickname.equals(transaction.merchantName, ignoreCase = true) ||
                it.bankName.equals(transaction.merchantName, ignoreCase = true)
            }
            if (destAccount != null) {
                repository.updateAccount(destAccount.copy(balance = destAccount.balance - transaction.amount))
            }
        }
    }

    // Category CRUD
    fun createCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, colorHex = colorHex, iconName = iconName, isCustom = true))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Bank Account CRUD
    fun createBankAccount(bankName: String, nickname: String, last4: String, balance: Double, colorHex: String) {
        viewModelScope.launch {
            repository.insertAccount(
                BankAccount(
                    bankName = bankName,
                    nickname = nickname,
                    last4Digits = last4,
                    balance = balance,
                    colorHex = colorHex
                )
            )
        }
    }
}
