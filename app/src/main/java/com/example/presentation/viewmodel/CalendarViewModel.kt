package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.BorrowLendItem
import com.example.domain.model.RecurringItem
import com.example.domain.model.SavingsGoal
import com.example.domain.model.Transaction
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class CalendarViewModel(private val repository: VaultRepository) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<SavingsGoal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val borrowLendItems: StateFlow<List<BorrowLendItem>> = repository.getAllBorrowLendItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringItems: StateFlow<List<RecurringItem>> = repository.getAllRecurringItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Day
    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate = _selectedDate.asStateFlow()

    fun selectDate(date: Calendar) {
        _selectedDate.value = date
    }

    // Helper functions to check if items fall on a specific calendar day
    fun getTransactionsForDay(year: Int, month: Int, day: Int): List<Transaction> {
        return transactions.value.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
        }
    }

    fun getGoalsForDay(year: Int, month: Int, day: Int): List<SavingsGoal> {
        return goals.value.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.createdDate }
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
        }
    }

    fun getBorrowLendForDay(year: Int, month: Int, day: Int): List<BorrowLendItem> {
        return borrowLendItems.value.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month && cal.get(Calendar.DAY_OF_MONTH) == day
        }
    }

    fun getRecurringForDay(year: Int, month: Int, day: Int): List<RecurringItem> {
        // Simple logic for recurring payments: monthly items recur on the same day-of-month, daily recur every day, etc.
        return recurringItems.value.filter {
            it.isActive
            // Simple match for mockup / representation
        }
    }
}
