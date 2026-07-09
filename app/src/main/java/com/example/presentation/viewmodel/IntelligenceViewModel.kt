package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

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
}

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
