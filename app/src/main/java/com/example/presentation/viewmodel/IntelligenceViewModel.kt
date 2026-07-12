package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.VaultRepository
import com.example.data.ocr.GeminiService
import com.example.data.ocr.ChatMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat

class IntelligenceViewModel(private val repository: VaultRepository) : ViewModel() {

    // --- State Flows from Repository ---
    val allTransactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMerchants: StateFlow<List<Merchant>> = repository.getAllMerchants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAiMemories: StateFlow<List<AiMemory>> = repository.getAllAiMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<BankAccount>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPaymentMethods: StateFlow<List<PaymentMethod>> = repository.getAllPaymentMethods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOcrHistories: StateFlow<List<OcrHistory>> = repository.getAllOcrHistories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<SavingsGoal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBorrowLendItems: StateFlow<List<BorrowLendItem>> = repository.getAllBorrowLendItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
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

    // --- Global Search & Multi-Filter State ---
    val searchQuery = MutableStateFlow("")
    
    // Multi-Filter criteria (Section 9)
    val filterType = MutableStateFlow<String?>(null) // "Expense", "Income", "Transfer"
    val filterCategoryId = MutableStateFlow<Int?>(null)
    val filterMerchantId = MutableStateFlow<Int?>(null)
    val filterBankId = MutableStateFlow<Int?>(null)
    val filterPaymentMethod = MutableStateFlow<String?>(null)
    val filterMinAmount = MutableStateFlow<Double?>(null)
    val filterMaxAmount = MutableStateFlow<Double?>(null)
    val filterTag = MutableStateFlow<String?>(null)
    val filterHasReceipt = MutableStateFlow<Boolean?>(null)
    val filterOnlyFavorites = MutableStateFlow(false)
    val filterStartDate = MutableStateFlow<Long?>(null)
    val filterEndDate = MutableStateFlow<Long?>(null)

    // Combined filtered transaction list
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        searchQuery,
        filterType,
        filterCategoryId,
        filterMerchantId,
        filterBankId,
        filterPaymentMethod,
        filterMinAmount,
        filterMaxAmount,
        filterTag,
        filterHasReceipt,
        filterOnlyFavorites,
        filterStartDate,
        filterEndDate
    ) { params ->
        val txs = params[0] as List<Transaction>
        val query = (params[1] as String).trim().lowercase()
        val type = params[2] as String?
        val catId = params[3] as Int?
        val merchId = params[4] as Int?
        val bankId = params[5] as Int?
        val payMethod = params[6] as String?
        val minAmt = params[7] as Double?
        val maxAmt = params[8] as Double?
        val tag = params[9] as String?
        val hasReceipt = params[10] as Boolean?
        val onlyFavs = params[11] as Boolean
        val startD = params[12] as Long?
        val endD = params[13] as Long?

        txs.filter { tx ->
            // Search criteria: support fuzzy matching on various transaction properties
            val matchesQuery = if (query.isEmpty()) true else {
                tx.merchantName.lowercase().contains(query) ||
                tx.merchantAlias.lowercase().contains(query) ||
                tx.categoryName.lowercase().contains(query) ||
                tx.bankAccountName.lowercase().contains(query) ||
                tx.paymentMethod.lowercase().contains(query) ||
                tx.notes.lowercase().contains(query) ||
                tx.tags.lowercase().contains(query) ||
                tx.amount.toString().contains(query) ||
                tx.id.toString().contains(query)
            }

            val matchesType = if (type == null) true else tx.type.equals(type, ignoreCase = true)
            val matchesCategory = if (catId == null) true else tx.categoryId == catId
            val matchesBank = if (bankId == null) true else tx.bankAccountId == bankId
            val matchesPayment = if (payMethod == null) true else tx.paymentMethod.equals(payMethod, ignoreCase = true)
            val matchesMinAmt = if (minAmt == null) true else tx.amount >= minAmt
            val matchesMaxAmt = if (maxAmt == null) true else tx.amount <= maxAmt
            val matchesTag = if (tag == null) true else tx.tagList.any { t -> t.equals(tag, ignoreCase = true) || t.replace("#", "").equals(tag.replace("#", ""), ignoreCase = true) }
            val matchesReceipt = if (hasReceipt == null) true else if (hasReceipt) tx.receiptPath != null else tx.receiptPath == null
            val matchesFav = if (onlyFavs) tx.isFavorite else true
            val matchesStart = if (startD == null) true else tx.timestamp >= startD
            val matchesEnd = if (endD == null) true else tx.timestamp <= endD

            matchesQuery && matchesType && matchesCategory && matchesBank && matchesPayment &&
                    matchesMinAmt && matchesMaxAmt && matchesTag && matchesReceipt && matchesFav &&
                    matchesStart && matchesEnd
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Section 11: FREQUENTLY USED STATISTICS ---
    val frequentlyUsedStats: StateFlow<FrequentlyUsedStats> = allTransactions
        .map { txList ->
            if (txList.isEmpty()) return@map FrequentlyUsedStats()

            val activeTxs = txList.filter { !it.isDeleted }
            
            val merchantCounts = activeTxs.groupingBy { it.merchantAlias.ifBlank { it.merchantName } }.eachCount()
            val categoryCounts = activeTxs.groupingBy { it.categoryName }.eachCount()
            val bankCounts = activeTxs.groupingBy { it.bankAccountName }.eachCount()
            val paymentCounts = activeTxs.groupingBy { it.paymentMethod }.eachCount()
            
            val allTags = activeTxs.flatMap { it.tagList }
            val tagCounts = allTags.groupingBy { it }.eachCount()

            FrequentlyUsedStats(
                mostUsedMerchant = merchantCounts.maxByOrNull { it.value }?.key ?: "N/A",
                mostUsedCategory = categoryCounts.maxByOrNull { it.value }?.key ?: "N/A",
                mostUsedBank = bankCounts.maxByOrNull { it.value }?.key ?: "N/A",
                mostUsedPaymentMethod = paymentCounts.maxByOrNull { it.value }?.key ?: "N/A",
                mostUsedTag = tagCounts.maxByOrNull { it.value }?.key ?: "N/A"
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FrequentlyUsedStats())

    // --- Section 7: SMART TAGS ---
    val recentTags: StateFlow<List<String>> = allTransactions.map { txs ->
        txs.flatMap { it.tagList }
            .distinct()
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTags = MutableStateFlow(setOf("#College", "#Trip", "#Emergency", "#Birthday"))

    // --- Section 6: AUTO-SUGGESTIONS WHILE ENTERING ---
    fun getSuggestionsForMerchant(merchantInput: String): Flow<AiMemorySuggestion?> = flow {
        if (merchantInput.isBlank()) {
            emit(null)
            return@flow
        }
        val rawInput = merchantInput.trim()
        
        // 1. Try to find alias mapping (Section 4)
        val aliasRow = repository.getAliasForMerchant(rawInput)
        val targetName = aliasRow?.aliasMerchant ?: rawInput

        // 2. Lookup AI local memory table
        val memory = repository.getAiMemoryForMerchant(targetName)
        if (memory != null) {
            emit(AiMemorySuggestion(
                merchantName = targetName,
                isAliasMapped = aliasRow != null,
                originalMerchant = rawInput,
                category = memory.preferredCategory,
                bankName = memory.preferredBank,
                paymentMethod = memory.preferredPaymentMethod,
                tags = memory.preferredTags,
                notes = memory.preferredNotes,
                transactionType = memory.preferredTransactionType,
                confidence = memory.confidence
            ))
        } else {
            // Fallback suggestions based on transaction history
            val txs = allTransactions.value
            val match = txs.find { it.merchantName.equals(targetName, ignoreCase = true) || it.merchantAlias.equals(targetName, ignoreCase = true) }
            if (match != null) {
                emit(AiMemorySuggestion(
                    merchantName = targetName,
                    isAliasMapped = aliasRow != null,
                    originalMerchant = rawInput,
                    category = match.categoryName,
                    bankName = match.bankAccountName,
                    paymentMethod = match.paymentMethod,
                    tags = match.tags,
                    notes = match.notes,
                    transactionType = match.type,
                    confidence = 0.5
                ))
            } else {
                emit(null)
            }
        }
    }

    // --- Section 1: CATEGORY CRUD ---
    fun addCategory(
        name: String,
        colorHex: String,
        iconName: String,
        description: String = "",
        monthlyBudget: Double = 0.0,
        weeklyBudget: Double = 0.0,
        dailyBudget: Double = 0.0,
        defaultType: String = "Expense"
    ) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                colorHex = colorHex,
                iconName = iconName,
                isCustom = true,
                description = description,
                monthlyBudget = monthlyBudget,
                weeklyBudget = weeklyBudget,
                dailyBudget = dailyBudget,
                defaultTransactionType = defaultType,
                displayOrder = allCategories.value.size
            )
            repository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            // Soft delete or hard delete. Let's make it soft-delete archived or deleted
            repository.updateCategory(category.copy(status = "Deleted"))
        }
    }

    fun toggleCategoryFavorite(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category.copy(isFavorite = !category.isFavorite))
        }
    }

    // --- Section 3 & 4: MERCHANT DATABASE & ALIAS CRUD ---
    fun addMerchant(
        name: String,
        alias: String = "",
        category: String = "",
        address: String = "",
        website: String = "",
        phone: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val formattedName = alias.ifBlank { name }
            val merchant = Merchant(
                merchantName = name,
                alias = alias,
                storeName = formattedName,
                category = category,
                address = address,
                website = website,
                phone = phone,
                notes = notes,
                firstTransaction = System.currentTimeMillis(),
                lastTransaction = System.currentTimeMillis()
            )
            repository.insertMerchant(merchant)

            // If an alias was provided, insert to MerchantAlias table (Section 4)
            if (alias.isNotBlank()) {
                repository.insertAlias(MerchantAlias(originalMerchant = name, aliasMerchant = alias))
            }
        }
    }

    fun updateMerchant(merchant: Merchant) {
        viewModelScope.launch {
            repository.updateMerchant(merchant)
            // Synchronize alias table if custom alias changed
            if (merchant.alias.isNotBlank()) {
                repository.insertAlias(MerchantAlias(originalMerchant = merchant.merchantName, aliasMerchant = merchant.alias))
            }
        }
    }

    fun toggleMerchantFavorite(merchant: Merchant) {
        viewModelScope.launch {
            repository.updateMerchant(merchant.copy(isFavorite = !merchant.isFavorite))
        }
    }

    // --- Section 14: LOCAL AI LEARNING ENGINE ENGINE ---
    // Trigger offline learning loop on each manual or OCR transaction save
    fun learnFromTransaction(tx: Transaction) {
        viewModelScope.launch {
            val rawName = tx.merchantName.trim()
            val normalizedName = tx.merchantAlias.ifBlank { rawName }

            // 1. Process Alias learning if raw differs from alias
            if (rawName.isNotEmpty() && normalizedName.isNotEmpty() && rawName != normalizedName) {
                val existingAlias = repository.getAliasForMerchant(rawName)
                if (existingAlias == null) {
                    val correction = repository.getCorrection(rawName, normalizedName)
                    val newCount = (correction?.count ?: 0) + 1
                    repository.insertCorrection(MerchantCorrection(
                        id = correction?.id ?: 0,
                        originalMerchant = rawName,
                        correctedMerchant = normalizedName,
                        count = newCount
                    ))

                    // If corrected 3 times, save as permanent alias
                    if (newCount >= 3) {
                        repository.insertAlias(MerchantAlias(rawName, normalizedName))
                    }
                }
            }

            // 2. Incremental preferred category, bank, payment method, tags, and notes to AiMemory
            val existingMemory = repository.getAiMemoryForMerchant(normalizedName)
            val newCount = (existingMemory?.learningCount ?: 0) + 1
            
            // Calculate offline confidence based on counts (up to 1.0 max)
            val calculatedConfidence = (0.5 + (newCount * 0.1)).coerceAtMost(1.0)

            val updatedMemory = AiMemory(
                merchantName = normalizedName,
                preferredCategory = tx.categoryName.ifBlank { existingMemory?.preferredCategory ?: "" },
                preferredBank = tx.bankAccountName.ifBlank { existingMemory?.preferredBank ?: "" },
                preferredPaymentMethod = tx.paymentMethod.ifBlank { existingMemory?.preferredPaymentMethod ?: "" },
                preferredTags = tx.tags.ifBlank { existingMemory?.preferredTags ?: "" },
                preferredNotes = tx.notes.ifBlank { existingMemory?.preferredNotes ?: "" },
                preferredTransactionType = tx.type,
                learningCount = newCount,
                confidence = calculatedConfidence
            )
            repository.insertAiMemory(updatedMemory)

            // 3. Keep Merchant Database table synced
            val existingMerchant = repository.getMerchantByName(normalizedName)
            if (existingMerchant != null) {
                // Update stats
                val updatedVisits = existingMerchant.visitCount + 1
                val updatedTotal = existingMerchant.totalSpending + tx.amount
                val updatedAvg = updatedTotal / updatedVisits
                
                val currentMethods = existingMerchant.paymentMethodsUsed.split(",").toMutableSet()
                currentMethods.add(tx.paymentMethod)
                
                val currentBanks = existingMerchant.banksUsed.split(",").toMutableSet()
                currentBanks.add(tx.bankAccountName)

                repository.updateMerchant(existingMerchant.copy(
                    visitCount = updatedVisits,
                    totalSpending = updatedTotal,
                    averageSpending = updatedAvg,
                    lastTransaction = tx.timestamp,
                    paymentMethodsUsed = currentMethods.filter { it.isNotBlank() }.joinToString(","),
                    banksUsed = currentBanks.filter { it.isNotBlank() }.joinToString(",")
                ))
            } else {
                // Insert new merchant
                repository.insertMerchant(Merchant(
                    merchantName = rawName,
                    alias = tx.merchantAlias,
                    storeName = normalizedName,
                    category = tx.categoryName,
                    paymentMethodsUsed = tx.paymentMethod,
                    banksUsed = tx.bankAccountName,
                    tags = tx.tags,
                    visitCount = 1,
                    totalSpending = tx.amount,
                    averageSpending = tx.amount,
                    firstTransaction = tx.timestamp,
                    lastTransaction = tx.timestamp
                ))
            }

            // 4. Update usage count and last used date in Category
            val cats = allCategories.value
            val matchCat = cats.find { it.name.equals(tx.categoryName, ignoreCase = true) }
            if (matchCat != null) {
                repository.updateCategory(matchCat.copy(
                    usageCount = matchCat.usageCount + 1,
                    lastUsed = tx.timestamp
                ))
            }
        }
    }

    fun clearAllFilters() {
        searchQuery.value = ""
        filterType.value = null
        filterCategoryId.value = null
        filterMerchantId.value = null
        filterBankId.value = null
        filterPaymentMethod.value = null
        filterMinAmount.value = null
        filterMaxAmount.value = null
        filterTag.value = null
        filterHasReceipt.value = null
        filterOnlyFavorites.value = false
        filterStartDate.value = null
        filterEndDate.value = null
    }

    // --- AI FINANCE ASSISTANT ENGINE ---
    val aiInsights: StateFlow<IntelligentInsights> = combine(
        allTransactions,
        allCategories,
        allAccounts,
        allGoals,
        allBorrowLendItems
    ) { params ->
        val txs = (params[0] as List<Transaction>).filter { !it.isDeleted }
        val cats = params[1] as List<Category>
        val accs = params[2] as List<BankAccount>
        val goals = params[3] as List<SavingsGoal>
        val borrowLend = params[4] as List<BorrowLendItem>

        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val oneWeekMs = 7 * oneDayMs
        val oneMonthMs = 30 * oneDayMs
        val oneYearMs = 365 * oneDayMs

        // Daily Reviews
        val todayTxs = txs.filter { now - it.timestamp <= oneDayMs }
        val todaySpent = todayTxs.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
        val todayIncome = todayTxs.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
        val todayTopCat = todayTxs.filter { it.type.equals("Expense", ignoreCase = true) }
            .groupBy { it.categoryName }
            .maxByOrNull { it.value.sumOf { t -> t.amount } }?.key ?: "None"
        val dailyReview = FinancialReview(
            totalSpent = todaySpent,
            totalIncome = todayIncome,
            topCategory = todayTopCat,
            reviewText = if (todaySpent > 0) "Today you spent ₹${todaySpent.toInt()} primarily on $todayTopCat. Keep an eye on your budget!" else "Amazing! You recorded no expenses today. Excellent budget control!"
        )

        // Weekly Reviews
        val weekTxs = txs.filter { now - it.timestamp <= oneWeekMs }
        val weekSpent = weekTxs.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
        val weekIncome = weekTxs.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
        val weekTopCat = weekTxs.filter { it.type.equals("Expense", ignoreCase = true) }
            .groupBy { it.categoryName }
            .maxByOrNull { it.value.sumOf { t -> t.amount } }?.key ?: "None"
        val weeklyReview = FinancialReview(
            totalSpent = weekSpent,
            totalIncome = weekIncome,
            topCategory = weekTopCat,
            reviewText = "This week you spent ₹${weekSpent.toInt()} and earned ₹${weekIncome.toInt()}. Your top spending category was $weekTopCat."
        )

        // Monthly Reviews
        val monthTxs = txs.filter { now - it.timestamp <= oneMonthMs }
        val monthSpent = monthTxs.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
        val monthIncome = monthTxs.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
        val monthTopCat = monthTxs.filter { it.type.equals("Expense", ignoreCase = true) }
            .groupBy { it.categoryName }
            .maxByOrNull { it.value.sumOf { t -> t.amount } }?.key ?: "None"
        val monthlyReview = FinancialReview(
            totalSpent = monthSpent,
            totalIncome = monthIncome,
            topCategory = monthTopCat,
            reviewText = "This month you spent ₹${monthSpent.toInt()} against ₹${monthIncome.toInt()} income. Top Category: $monthTopCat."
        )

        // Yearly Reviews
        val yearTxs = txs.filter { now - it.timestamp <= oneYearMs }
        val yearSpent = yearTxs.filter { it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
        val yearIncome = yearTxs.filter { it.type.equals("Income", ignoreCase = true) }.sumOf { it.amount }
        val yearTopCat = yearTxs.filter { it.type.equals("Expense", ignoreCase = true) }
            .groupBy { it.categoryName }
            .maxByOrNull { it.value.sumOf { t -> t.amount } }?.key ?: "None"
        val yearlyReview = FinancialReview(
            totalSpent = yearSpent,
            totalIncome = yearIncome,
            topCategory = yearTopCat,
            reviewText = "Over the past 365 days, your cumulative spending is ₹${yearSpent.toInt()} with ₹${yearIncome.toInt()} income. Top category: $yearTopCat."
        )

        // Spending Pattern Analysis
        val pattern = if (weekSpent > monthSpent / 4) {
            "Your weekly spending of ₹${weekSpent.toInt()} is higher than your average monthly run rate. Recommended to slow down."
        } else {
            "Healthy spending trend. Your spending this week is well below the safe weekly threshold."
        }

        // Budget Suggestions
        val budgetSugs = mutableListOf<String>()
        cats.forEach { cat ->
            val catLimit = cat.monthlyBudget
            val catSpent = monthTxs.filter { it.categoryName.equals(cat.name, ignoreCase = true) && it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
            if (catLimit > 0 && catSpent > catLimit) {
                budgetSugs.add("You overspent on ${cat.name} by ₹${(catSpent - catLimit).toInt()} (Budget: ₹${catLimit.toInt()}). Consider increasing its limit or reducing visits.")
            } else if (catLimit > 0 && catSpent > catLimit * 0.8) {
                budgetSugs.add("Your spending on ${cat.name} (₹${catSpent.toInt()}) has reached 80% of its ₹${catLimit.toInt()} budget limit.")
            }
        }
        if (budgetSugs.isEmpty()) {
            budgetSugs.add("All category budgets are perfectly on track! Maintain this baseline.")
        }

        // Overspending Detection
        val overspendingAlerts = mutableListOf<String>()
        val recentLargeTxs = txs.filter { now - it.timestamp <= 3 * oneDayMs && it.amount > 1000 && it.type.equals("Expense", ignoreCase = true) }
        recentLargeTxs.forEach { tx ->
            overspendingAlerts.add("Detected high expense of ₹${tx.amount.toInt()} at ${tx.merchantName} in ${tx.categoryName}.")
        }
        if (overspendingAlerts.isEmpty()) {
            overspendingAlerts.add("No recent overspending alerts. Your spending velocity is steady.")
        }

        // Goal Progress Suggestions
        val goalSugs = mutableListOf<String>()
        goals.forEach { goal ->
            val progress = if (goal.targetAmount > 0) goal.currentSavedAmount / goal.targetAmount else 0.0
            if (progress >= 0.9) {
                goalSugs.add("You are 90%+ close to completing your '${goal.name}' goal! Try adding a final contribution of ₹${(goal.targetAmount - goal.currentSavedAmount).toInt()} today.")
            } else if (progress > 0.5) {
                goalSugs.add("You're more than halfway to '${goal.name}'! To speed up, consider skipping one meal out this week.")
            } else {
                goalSugs.add("Your '${goal.name}' goal is at ${(progress * 100).toInt()}%. Regular small additions will build compounding momentum.")
            }
        }
        if (goalSugs.isEmpty()) {
            goalSugs.add("No active savings goals found. Create a goal to save specifically for your laptops, kettles or gadgets!")
        }

        // Saving Recommendations
        val saveRecs = mutableListOf<String>()
        val remainingIncome = monthIncome - monthSpent
        if (remainingIncome > 0) {
            saveRecs.add("You have ₹${remainingIncome.toInt()} of unspent income this month. We recommend placing ₹${(remainingIncome * 0.5).toInt()} in high-yield savings.")
        } else {
            saveRecs.add("Your spending matches or exceeds your income this month. Try to cut non-essential category expenses by 15% next week.")
        }

        // Subscription Analysis
        val subTxs = txs.filter { 
            val label = it.merchantName.lowercase()
            label.contains("netflix") || label.contains("spotify") || label.contains("youtube") || label.contains("prime") || label.contains("disney") || label.contains("apple") || label.contains("membership") || label.contains("sub")
        }
        val subCost = subTxs.sumOf { it.amount }
        val subAnalysis = mutableListOf<String>()
        if (subCost > 0) {
            subAnalysis.add("You spent ₹${subCost.toInt()} on subscriptions this month. That is ₹${(subCost * 12).toInt()} annually.")
            subTxs.groupBy { it.merchantName }.forEach { (merchant, tList) ->
                if (tList.size > 1) {
                    subAnalysis.add("Potential duplicate alert: Multi-payments detected for subscription at $merchant.")
                }
            }
        } else {
            subAnalysis.add("No active digital subscription spending detected. Excellent work keeping fixed overheads zero!")
        }

        // Borrow/Lend Risk Analysis
        val overdueBorrow = borrowLend.filter { it.status.lowercase() != "completed" && it.dueDate < now }
        val borrowRisk = mutableListOf<String>()
        overdueBorrow.forEach { item ->
            val debtType = if (item.type.equals("Borrowed", ignoreCase = true)) "overdue payment to" else "unpaid dues from"
            borrowRisk.add("Risk alert: ₹${(item.amount - item.paidAmount).toInt()} is past due ($debtType ${item.personName}).")
        }
        if (borrowRisk.isEmpty()) {
            borrowRisk.add("Lending & borrowing ledger is clean and secure. Risk profile: Excellent.")
        }

        // Upcoming Financial Events
        val upcomingEvents = mutableListOf<String>()
        val upcomingBorrow = borrowLend.filter { it.status.lowercase() != "completed" && it.dueDate >= now && it.dueDate - now <= 5 * oneDayMs }
        upcomingBorrow.forEach { item ->
            val direction = if (item.type.equals("Borrowed", ignoreCase = true)) "Pay ₹${(item.amount - item.paidAmount).toInt()} to" else "Receive ₹${(item.amount - item.paidAmount).toInt()} from"
            upcomingEvents.add("$direction ${item.personName} on ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(item.dueDate))}")
        }
        if (upcomingEvents.isEmpty()) {
            upcomingEvents.add("No critical payments or due loans registered in the next 5 days.")
        }

        // Timeline events
        val timeline = mutableListOf<TimelineEvent>()
        // 1. Biggest Purchase
        val biggestPurchase = txs.filter { it.type.equals("Expense", ignoreCase = true) }.maxByOrNull { it.amount }
        if (biggestPurchase != null) {
            timeline.add(TimelineEvent(
                title = "Biggest Single Purchase",
                description = "Spent ₹${biggestPurchase.amount.toInt()} at ${biggestPurchase.merchantName} in category ${biggestPurchase.categoryName}.",
                timestamp = biggestPurchase.timestamp,
                type = "Biggest Purchase",
                iconName = "shopping_bag"
            ))
        }
        // 2. Highest Spending Day
        val spendingByDay = txs.filter { it.type.equals("Expense", ignoreCase = true) }
            .groupBy { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
            }
        val highestDayGroup = spendingByDay.maxByOrNull { it.value.sumOf { t -> t.amount } }
        if (highestDayGroup != null) {
            val dayTxs = highestDayGroup.value
            val totalDayAmt = dayTxs.sumOf { it.amount }
            timeline.add(TimelineEvent(
                title = "Highest Spending Peak Day",
                description = "Spent ₹${totalDayAmt.toInt()} across ${dayTxs.size} transactions.",
                timestamp = dayTxs.first().timestamp,
                type = "Highest Spending",
                iconName = "trending_up"
            ))
        }
        // 3. Goal Completed
        goals.forEach { goal ->
            if (goal.currentSavedAmount >= goal.targetAmount) {
                timeline.add(TimelineEvent(
                    title = "Savings Goal Completed! 🏆",
                    description = "Successfully saved ₹${goal.targetAmount.toInt()} for '${goal.name}'.",
                    timestamp = now,
                    type = "Goal Completed",
                    iconName = "stars"
                ))
            }
        }
        // 4. Overspent budgets
        cats.forEach { cat ->
            val limit = cat.monthlyBudget
            val spent = monthTxs.filter { it.categoryName.equals(cat.name, ignoreCase = true) && it.type.equals("Expense", ignoreCase = true) }.sumOf { it.amount }
            if (limit > 0 && spent > limit) {
                timeline.add(TimelineEvent(
                    title = "Category Budget Exceeded",
                    description = "Spent ₹${spent.toInt()} on ${cat.name}, exceeding the ₹${limit.toInt()} budget.",
                    timestamp = now - 2 * oneDayMs,
                    type = "Budget Exceeded",
                    iconName = "warning"
                ))
            }
        }
        if (timeline.isEmpty()) {
            timeline.add(TimelineEvent(
                title = "System Initialized",
                description = "VaultFlow Intelligent agent is ready. Log your first expense to begin!",
                timestamp = now,
                type = "Milestone",
                iconName = "lock"
            ))
        }
        timeline.sortByDescending { it.timestamp }

        // Expense predictions
        val monthlyAverages = monthSpent
        val expensePredictionNextMonth = if (monthlyAverages > 0) monthlyAverages * 1.05 else 12500.0

        // Goal Completion predictions
        val goalCompletionPredictions = mutableListOf<String>()
        goals.forEach { goal ->
            if (goal.currentSavedAmount < goal.targetAmount) {
                val progress = goal.currentSavedAmount
                val rate = if (progress > 0) progress / 30.0 else 10.0
                val remaining = goal.targetAmount - goal.currentSavedAmount
                val predictedDays = (remaining / rate).coerceAtMost(365.0).toInt().coerceAtLeast(1)
                goalCompletionPredictions.add("'${goal.name}': Estimated completion in $predictedDays days based on savings rate.")
            }
        }
        if (goalCompletionPredictions.isEmpty()) {
            goalCompletionPredictions.add("No pending savings goals. Create a savings target to see predicted completions.")
        }

        // Recommended monthly / daily
        val recommendedMonthlyBudget = if (monthIncome > 0) monthIncome * 0.7 else 15000.0
        val recommendedDailySafeSpending = if (recommendedMonthlyBudget > 0) recommendedMonthlyBudget / 30.0 else 500.0

        // Merchant Spending Analysis
        val merchSpending = txs.filter { it.type.equals("Expense", ignoreCase = true) }
            .groupBy { it.merchantAlias.ifBlank { it.merchantName } }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
        val merchantSpendingAnalysis = merchSpending.map { "₹${it.second.toInt()} spent at ${it.first}." }
            .ifEmpty { listOf("No merchant interactions recorded.") }

        // Category Spending Analysis
        val catSpending = txs.filter { it.type.equals("Expense", ignoreCase = true) }
            .groupBy { it.categoryName }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
        val categorySpendingAnalysis = catSpending.map { "₹${it.second.toInt()} spent in ${it.first}." }
            .ifEmpty { listOf("No category distributions mapped yet.") }

        // Bank Usage Analysis
        val bankUsage = txs.groupBy { it.bankAccountName }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
        val bankUsageAnalysis = bankUsage.map { "${it.first} used for ${it.second} transactions." }
            .ifEmpty { listOf("No bank transaction volumes detected.") }

        // Payment Method Analysis
        val payMethodUsage = txs.groupBy { it.paymentMethod }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
        val paymentMethodAnalysis = payMethodUsage.map { "${it.first} used for ${it.second} payments." }
            .ifEmpty { listOf("No payment methods indexed.") }

        // Duplicate Transaction Detection Improvements
        val duplicates = mutableListOf<Pair<Transaction, Transaction>>()
        val sortedTxs = txs.sortedBy { it.timestamp }
        for (i in 0 until sortedTxs.size - 1) {
            val t1 = sortedTxs[i]
            val t2 = sortedTxs[i + 1]
            if (t1.amount == t2.amount &&
                t1.categoryName.equals(t2.categoryName, ignoreCase = true) &&
                t1.merchantName.equals(t2.merchantName, ignoreCase = true) &&
                t2.timestamp - t1.timestamp <= 10 * 60 * 1000L
            ) {
                duplicates.add(Pair(t1, t2))
            }
        }

        // Missing Transaction Suggestions
        val missingTransactionSuggestions = mutableListOf<String>()
        val todayHasTx = todayTxs.isNotEmpty()
        if (!todayHasTx) {
            missingTransactionSuggestions.add("No transactions recorded today. Log your transport, grocery or food spends to maintain your streaks!")
        }
        val morningCoffeeTxs = txs.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            (it.merchantName.lowercase().contains("coffee") || it.merchantName.lowercase().contains("starbucks")) && hour in 7..11
        }
        if (morningCoffeeTxs.isNotEmpty() && !todayTxs.any { it.merchantName.lowercase().contains("coffee") }) {
            missingTransactionSuggestions.add("We noticed you skipped logging your standard morning coffee today. Record it to keep stats accurate!")
        }
        if (missingTransactionSuggestions.isEmpty()) {
            missingTransactionSuggestions.add("Your logging matches standard user routines perfectly. No missing transactions identified.")
        }

        // Streaks & Achievements
        var streakDays = 0
        val dayGroupedTxs = txs.filter { it.type.equals("Expense", ignoreCase = true) }
            .groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
            }
        val calendarCursor = Calendar.getInstance()
        for (d in 0..14) {
            val dateStr = "${calendarCursor.get(Calendar.YEAR)}-${calendarCursor.get(Calendar.MONTH)}-${calendarCursor.get(Calendar.DAY_OF_MONTH)}"
            val daySpend = dayGroupedTxs[dateStr]?.sumOf { it.amount } ?: 0.0
            if (daySpend <= recommendedDailySafeSpending) {
                streakDays++
            } else {
                break
            }
            calendarCursor.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Financial health score
        val totalBudgetSpent = monthSpent
        val totalBudgetLimit = recommendedMonthlyBudget
        val budgetScoreVal = if (totalBudgetLimit > 0) ((1.0 - (totalBudgetSpent / totalBudgetLimit).coerceAtMost(1.0)) * 100).toInt().coerceAtLeast(30) else 90
        val budgetExplVal = "You spent ₹${totalBudgetSpent.toInt()} out of your recommended budget limit of ₹${totalBudgetLimit.toInt()}."

        val savingsRate = if (monthIncome > 0) (monthIncome - monthSpent) / monthIncome else 0.15
        val savingsScoreVal = (savingsRate.coerceIn(0.0, 0.4) / 0.4 * 100).toInt().coerceAtLeast(40)
        val savingsExplVal = "Saving ${(savingsRate * 100).toInt()}% of your income. High-performers target 20%+"

        val debtOverdue = overdueBorrow.isNotEmpty()
        val debtScoreVal = if (debtOverdue) 55 else 95
        val debtExplVal = if (debtOverdue) "Risk present: You have overdue borrowing items in your ledger." else "No overdue loans or debts detected. Risk: Extremely Safe."

        val consistencyDays = txs.filter { now - it.timestamp <= oneWeekMs }
            .map { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.DAY_OF_WEEK)
            }.distinct().size
        val consistencyScoreVal = (consistencyDays.toFloat() / 7f * 100).toInt().coerceAtLeast(30)
        val consistencyExplVal = "You recorded transactions on $consistencyDays out of 7 days this week."

        val subscriptionScoreVal = if (subCost > 0) ((1.0 - (subCost / (monthIncome.coerceAtLeast(1.0))).coerceAtMost(0.3)) * 100).toInt() else 95
        val subscriptionsExplVal = "Subscriptions consume ${(subCost / monthIncome.coerceAtLeast(1.0) * 100).toInt()}% of overall income."

        val activeGoalsNotDone = goals.filter { it.currentSavedAmount < it.targetAmount }
        val goalsScoreVal = if (activeGoalsNotDone.isEmpty() && goals.isNotEmpty()) 100 else if (goals.isEmpty()) 70 else 85
        val goalsExplVal = "Actively contributing to ${activeGoalsNotDone.size} goals with compounding momentum."

        val totalScoreVal = ((budgetScoreVal + savingsScoreVal + debtScoreVal + consistencyScoreVal + subscriptionScoreVal + goalsScoreVal) / 6)

        val healthScoreObj = IntelligenceHealthScore(
            totalScore = totalScoreVal,
            budgetScore = budgetScoreVal,
            budgetExpl = budgetExplVal,
            savingsScore = savingsScoreVal,
            savingsExpl = savingsExplVal,
            debtScore = debtScoreVal,
            debtExpl = debtExplVal,
            consistencyScore = consistencyScoreVal,
            consistencyExpl = consistencyExplVal,
            subscriptionsScore = subscriptionScoreVal,
            subscriptionsExpl = subscriptionsExplVal,
            goalsScore = goalsScoreVal,
            goalsExpl = goalsExplVal
        )

        IntelligentInsights(
            dailyReview = dailyReview,
            weeklyReview = weeklyReview,
            monthlyReview = monthlyReview,
            yearlyReview = yearlyReview,
            spendingPattern = pattern,
            budgetSuggestions = budgetSugs,
            overspendingAlerts = overspendingAlerts,
            goalSuggestions = goalSugs,
            savingRecommendations = saveRecs,
            subscriptionAnalysis = subAnalysis,
            borrowLendRiskAnalysis = borrowRisk,
            upcomingFinancialEvents = upcomingEvents,
            healthTimeline = timeline,
            expensePredictionNextMonth = expensePredictionNextMonth,
            goalCompletionPredictions = goalCompletionPredictions,
            recommendedMonthlyBudget = recommendedMonthlyBudget,
            recommendedDailySafeSpending = recommendedDailySafeSpending,
            merchantSpendingAnalysis = merchantSpendingAnalysis,
            categorySpendingAnalysis = categorySpendingAnalysis,
            bankUsageAnalysis = bankUsageAnalysis,
            paymentMethodAnalysis = paymentMethodAnalysis,
            duplicateTransactions = duplicates,
            missingTransactionSuggestions = missingTransactionSuggestions,
            spendingStreak = streakDays,
            savingsStreak = if (savingsRate > 0) 3 else 0,
            achievements = listOf("Budget General", "Thrifty Saver", "Punctual Payer"),
            healthScore = healthScoreObj
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IntelligentInsights(
        dailyReview = FinancialReview(0.0, 0.0, "None", "Analyzing spins..."),
        weeklyReview = FinancialReview(0.0, 0.0, "None", "Analyzing spins..."),
        monthlyReview = FinancialReview(0.0, 0.0, "None", "Analyzing spins..."),
        yearlyReview = FinancialReview(0.0, 0.0, "None", "Analyzing spins..."),
        spendingPattern = "Steady flow",
        budgetSuggestions = listOf("Perfect spending baseline."),
        overspendingAlerts = listOf("Steady velocity."),
        goalSuggestions = listOf("Maintain goals contribution."),
        savingRecommendations = listOf("Set aside 15% minimum."),
        subscriptionAnalysis = listOf("No high overheads detected."),
        borrowLendRiskAnalysis = listOf("ledger is fully clean."),
        upcomingFinancialEvents = listOf("No bills due soon."),
        healthTimeline = emptyList(),
        expensePredictionNextMonth = 12000.0,
        goalCompletionPredictions = listOf("Estimating soon..."),
        recommendedMonthlyBudget = 10000.0,
        recommendedDailySafeSpending = 300.0,
        merchantSpendingAnalysis = listOf("Processing..."),
        categorySpendingAnalysis = listOf("Processing..."),
        bankUsageAnalysis = listOf("Processing..."),
        paymentMethodAnalysis = listOf("Processing..."),
        duplicateTransactions = emptyList(),
        missingTransactionSuggestions = listOf("Logged regularly."),
        spendingStreak = 3,
        savingsStreak = 1,
        achievements = listOf("Budget General", "Thrifty Saver", "Punctual Payer"),
        healthScore = IntelligenceHealthScore()
    ))

    // --- Conversational Financial AI Assistant ---
    val chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val isChatLoading = MutableStateFlow(false)

    fun compileLedgerContext(): String {
        val sb = StringBuilder()
        
        // 1. Bank Accounts & Balances
        val accounts = allAccounts.value
        sb.append("ACCOUNTS & BALANCES:\n")
        if (accounts.isEmpty()) {
            sb.append("- No accounts set up.\n")
        } else {
            accounts.forEach { acct ->
                sb.append("- ${acct.bankName} (${acct.nickname}): Balance ₹${acct.balance} (Last 4: ${acct.last4Digits})\n")
            }
        }
        sb.append("\n")
        
        // 2. Savings Goals
        val goals = allGoals.value
        sb.append("ACTIVE SAVINGS GOALS:\n")
        if (goals.isEmpty()) {
            sb.append("- No active savings goals.\n")
        } else {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            goals.forEach { g ->
                val dateStr = dateFormat.format(Date(g.targetDate))
                sb.append("- Goal: ${g.name}, Target: ₹${g.targetAmount}, Current Saved: ₹${g.currentSavedAmount}, Target Date: $dateStr, Status: ${g.status}\n")
            }
        }
        sb.append("\n")

        // 3. Borrow & Lend (Ledger)
        val borrowLend = allBorrowLendItems.value
        sb.append("BORROW & LEND LEDGER ITEMS:\n")
        if (borrowLend.isEmpty()) {
            sb.append("- No active borrow/lend records.\n")
        } else {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            borrowLend.forEach { bl ->
                val typeWord = if (bl.type == "Borrowed") "Borrowed from" else "Lent to"
                val dateStr = dateFormat.format(Date(bl.date))
                val dueDateStr = dateFormat.format(Date(bl.dueDate))
                sb.append("- $typeWord ${bl.personName}: ₹${bl.amount} (Paid: ₹${bl.paidAmount}, Date: $dateStr, Due Date: $dueDateStr, Status: ${bl.status})\n")
            }
        }
        sb.append("\n")

        // 4. Transactions Ledger
        val transactions = allTransactions.value
        sb.append("TRANSACTION ENTRIES (Total: ${transactions.size}):\n")
        if (transactions.isEmpty()) {
            sb.append("- No transaction entries logged yet.\n")
        } else {
            val latestTxs = transactions.sortedByDescending { it.timestamp }.take(200)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            latestTxs.forEach { tx ->
                val typeWord = if (tx.type == "Income") "Income (+)" else "Expense (-)"
                val dateStr = dateFormat.format(Date(tx.timestamp))
                val aliasText = if (tx.merchantAlias.isNotEmpty()) " (Alias: ${tx.merchantAlias})" else ""
                sb.append("- [$dateStr] $typeWord: ₹${tx.amount} at ${tx.merchantName}$aliasText. Category: ${tx.categoryName}. Payment: ${tx.paymentMethod} (${tx.bankAccountName}). Tags: ${tx.tags}. Notes: ${tx.notes}\n")
            }
        }
        
        return sb.toString()
    }

    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return
        
        val userMsg = ChatMessage(role = "user", content = messageText)
        chatMessages.value = chatMessages.value + userMsg
        
        isChatLoading.value = true
        
        viewModelScope.launch {
            try {
                val contextLedger = compileLedgerContext()
                val currentHistory = chatMessages.value.dropLast(1)
                
                val response = GeminiService.askGemini(
                    question = messageText,
                    contextLedger = contextLedger,
                    history = currentHistory
                )
                
                chatMessages.value = chatMessages.value + ChatMessage(role = "model", content = response)
            } catch (e: Exception) {
                chatMessages.value = chatMessages.value + ChatMessage(
                    role = "model",
                    content = "Error processing response: ${e.localizedMessage ?: e.message}"
                )
            } finally {
                isChatLoading.value = false
            }
        }
    }

    fun clearChatHistory() {
        chatMessages.value = emptyList()
    }
}

// Support Models for AI Intelligence
data class FinancialReview(
    val totalSpent: Double,
    val totalIncome: Double,
    val topCategory: String,
    val reviewText: String
)

data class TimelineEvent(
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: String,
    val iconName: String
)

data class IntelligenceHealthScore(
    val totalScore: Int = 85,
    val budgetScore: Int = 90,
    val budgetExpl: String = "You are staying well within your ₹10,000 budget.",
    val savingsScore: Int = 85,
    val savingsExpl: String = "Good savings rate. You saved 15% of your income this month.",
    val debtScore: Int = 95,
    val debtExpl: String = "Low debt-to-income ratio. No overdue loans.",
    val consistencyScore: Int = 80,
    val consistencyExpl: String = "Logged transactions 5 out of 7 days this week.",
    val subscriptionsScore: Int = 90,
    val subscriptionsExpl: String = "No high-risk or inactive subscriptions found.",
    val goalsScore: Int = 75,
    val goalsExpl: String = "On track for savings goal, but could increase monthly savings."
)

data class IntelligentInsights(
    val dailyReview: FinancialReview,
    val weeklyReview: FinancialReview,
    val monthlyReview: FinancialReview,
    val yearlyReview: FinancialReview,
    val spendingPattern: String,
    val budgetSuggestions: List<String>,
    val overspendingAlerts: List<String>,
    val goalSuggestions: List<String>,
    val savingRecommendations: List<String>,
    val subscriptionAnalysis: List<String>,
    val borrowLendRiskAnalysis: List<String>,
    val upcomingFinancialEvents: List<String>,
    val healthTimeline: List<TimelineEvent>,
    val expensePredictionNextMonth: Double,
    val goalCompletionPredictions: List<String>,
    val recommendedMonthlyBudget: Double,
    val recommendedDailySafeSpending: Double,
    val merchantSpendingAnalysis: List<String>,
    val categorySpendingAnalysis: List<String>,
    val bankUsageAnalysis: List<String>,
    val paymentMethodAnalysis: List<String>,
    val duplicateTransactions: List<Pair<Transaction, Transaction>>,
    val missingTransactionSuggestions: List<String>,
    val spendingStreak: Int,
    val savingsStreak: Int,
    val achievements: List<String>,
    val healthScore: IntelligenceHealthScore
)

data class FrequentlyUsedStats(
    val mostUsedMerchant: String = "N/A",
    val mostUsedCategory: String = "N/A",
    val mostUsedBank: String = "N/A",
    val mostUsedPaymentMethod: String = "N/A",
    val mostUsedTag: String = "N/A"
)

data class AiMemorySuggestion(
    val merchantName: String,
    val isAliasMapped: Boolean,
    val originalMerchant: String,
    val category: String,
    val bankName: String,
    val paymentMethod: String,
    val tags: String,
    val notes: String,
    val transactionType: String,
    val confidence: Double
)
