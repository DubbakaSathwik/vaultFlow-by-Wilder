package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.BankAccount
import com.example.domain.model.PaymentMethod
import com.example.domain.model.RecurringItem
import com.example.domain.model.Transaction
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(private val repository: VaultRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val bankAccounts: StateFlow<List<BankAccount>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentMethods: StateFlow<List<PaymentMethod>> = repository.getAllPaymentMethods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringItems: StateFlow<List<RecurringItem>> = repository.getAllRecurringItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined results flow
    val searchResults: StateFlow<SearchResultsState> = combine(
        _searchQuery,
        bankAccounts,
        paymentMethods,
        recurringItems,
        transactions
    ) { query, accounts, methods, recurrings, txs ->
        if (query.isBlank()) {
            return@combine SearchResultsState()
        }

        val q = query.lowercase().trim()

        val filteredAccounts = accounts.filter {
            it.bankName.lowercase().contains(q) ||
            it.nickname.lowercase().contains(q) ||
            it.accountHolder.lowercase().contains(q) ||
            it.upiId.lowercase().contains(q)
        }

        val filteredMethods = methods.filter {
            it.nickname.lowercase().contains(q) ||
            it.type.lowercase().contains(q) ||
            it.upiId.lowercase().contains(q) ||
            it.linkedBankName.lowercase().contains(q)
        }

        val filteredRecurrings = recurrings.filter {
            it.name.lowercase().contains(q) ||
            it.categoryName.lowercase().contains(q) ||
            it.bankAccountName.lowercase().contains(q) ||
            it.notes.lowercase().contains(q)
        }

        val filteredTransactions = txs.filter {
            it.merchantName.lowercase().contains(q) ||
            it.categoryName.lowercase().contains(q) ||
            it.notes.lowercase().contains(q) ||
            it.paymentMethod.lowercase().contains(q) ||
            it.tags.lowercase().contains(q)
        }

        SearchResultsState(
            accounts = filteredAccounts,
            methods = filteredMethods,
            recurrings = filteredRecurrings,
            transactions = filteredTransactions,
            hasResults = filteredAccounts.isNotEmpty() || filteredMethods.isNotEmpty() || filteredRecurrings.isNotEmpty() || filteredTransactions.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResultsState())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

data class SearchResultsState(
    val accounts: List<BankAccount> = emptyList(),
    val methods: List<PaymentMethod> = emptyList(),
    val recurrings: List<RecurringItem> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val hasResults: Boolean = false
)
