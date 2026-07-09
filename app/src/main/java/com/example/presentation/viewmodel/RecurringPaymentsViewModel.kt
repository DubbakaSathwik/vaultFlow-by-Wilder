package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.RecurringItem
import com.example.domain.model.Transaction
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringPaymentsViewModel(private val repository: VaultRepository) : ViewModel() {

    // All recurring payments
    val allRecurringItems: StateFlow<List<RecurringItem>> = repository.getAllRecurringItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Recurring Incomes
    val recurringIncomes: StateFlow<List<RecurringItem>> = allRecurringItems
        .map { items -> items.filter { it.type == "Income" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Recurring Expenses
    val recurringExpenses: StateFlow<List<RecurringItem>> = allRecurringItems
        .map { items -> items.filter { it.type == "Expense" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedInitialRecurringItemsIfNeeded()
    }

    private fun seedInitialRecurringItemsIfNeeded() {
        viewModelScope.launch {
            val currentItems = repository.getAllRecurringItems().first()
            if (currentItems.isEmpty()) {
                val accounts = repository.getAllAccounts().first()
                val categories = repository.getAllCategories().first()

                val defaultBankId = accounts.firstOrNull()?.id ?: 1
                val defaultBankName = accounts.firstOrNull()?.nickname ?: "Cash"
                val foodCatId = categories.find { it.name == "Food" }?.id ?: 1
                val billsCatId = categories.find { it.name == "Bills" }?.id ?: 2

                val defaults = listOf(
                    RecurringItem(
                        type = "Expense",
                        name = "Netflix Standard Premium",
                        amount = 649.0,
                        frequency = "Monthly",
                        bankAccountId = defaultBankId,
                        bankAccountName = defaultBankName,
                        paymentMethod = "Google Pay",
                        categoryId = billsCatId,
                        categoryName = "Bills",
                        notes = "Auto-renewal sub for family screen",
                        isAutoAdd = true,
                        askBeforeAdd = false,
                        hasReminder = true
                    ),
                    RecurringItem(
                        type = "Expense",
                        name = "Spotify Student Duo",
                        amount = 149.0,
                        frequency = "Monthly",
                        bankAccountId = defaultBankId,
                        bankAccountName = defaultBankName,
                        paymentMethod = "UPI",
                        categoryId = billsCatId,
                        categoryName = "Bills",
                        notes = "Music streaming",
                        isAutoAdd = false,
                        askBeforeAdd = true,
                        hasReminder = true
                    ),
                    RecurringItem(
                        type = "Income",
                        name = "Monthly Allowance",
                        amount = 5000.0,
                        frequency = "Monthly",
                        bankAccountId = defaultBankId,
                        bankAccountName = defaultBankName,
                        categoryId = billsCatId,
                        categoryName = "Others",
                        notes = "From dad",
                        isAutoAdd = true,
                        askBeforeAdd = false,
                        hasReminder = true
                    )
                )

                defaults.forEach { repository.insertRecurringItem(it) }
            }
        }
    }

    // Insert, Update, Delete
    fun addRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            repository.insertRecurringItem(item)
        }
    }

    fun updateRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            repository.updateRecurringItem(item)
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            repository.deleteRecurringItem(item)
        }
    }

    // Trigger & execute a transaction based on the recurring schedule rules
    fun triggerRecurringPayment(item: RecurringItem) {
        viewModelScope.launch {
            // Log a transaction in the transaction engine
            val transaction = Transaction(
                amount = item.amount,
                type = item.type,
                categoryName = item.categoryName,
                categoryId = item.categoryId,
                merchantName = "Recurring: ${item.name}",
                paymentMethod = item.paymentMethod,
                bankAccountId = item.bankAccountId,
                bankAccountName = item.bankAccountName,
                timestamp = System.currentTimeMillis(),
                notes = "Auto-generated from recurring template: ${item.notes}",
                tags = "#recurring"
            )
            repository.insertTransaction(transaction)

            // Adjust bank account balance based on transaction type
            val account = repository.getAccountById(item.bankAccountId)
            if (account != null) {
                val newBalance = if (item.type == "Income") {
                    account.balance + item.amount
                } else {
                    account.balance - item.amount
                }
                repository.updateAccount(account.copy(balance = newBalance))
            }

            // Update recurring item's last run timestamp
            val updatedItem = item.copy(
                lastTriggeredTimestamp = System.currentTimeMillis(),
                nextTriggeredTimestamp = System.currentTimeMillis() + getFrequencyOffset(item.frequency)
            )
            repository.updateRecurringItem(updatedItem)
        }
    }

    private fun getFrequencyOffset(frequency: String): Long {
        val oneDay = 24 * 60 * 60 * 1000L
        return when (frequency) {
            "Daily" -> oneDay
            "Weekly" -> 7 * oneDay
            "Monthly" -> 30 * oneDay
            "Yearly" -> 365 * oneDay
            else -> 30 * oneDay
        }
    }
}
