package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.BorrowLendItem
import com.example.domain.model.Transaction
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BorrowLendViewModel(private val repository: VaultRepository) : ViewModel() {

    val allItems: StateFlow<List<BorrowLendItem>> = repository.getAllBorrowLendItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankAccounts = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filter States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("All") // "All", "Borrowed", "Lended"
    val filterType = _filterType.asStateFlow()

    private val _filterStatus = MutableStateFlow("All") // "All", "Pending", "Partially Paid", "Completed", "Cancelled"
    val filterStatus = _filterStatus.asStateFlow()

    private val _sortBy = MutableStateFlow("Date (Newest)") // "Date (Newest)", "Date (Oldest)", "Amount (High-Low)", "Amount (Low-High)", "Due Date"
    val sortBy = _sortBy.asStateFlow()

    val filteredItems: StateFlow<List<BorrowLendItem>> = combine(
        allItems, searchQuery, filterType, filterStatus, sortBy
    ) { items, query, type, status, sort ->
        var list = items

        // 1. Search
        if (query.isNotBlank()) {
            list = list.filter {
                it.personName.contains(query, ignoreCase = true) ||
                        (it.notes?.contains(query, ignoreCase = true) ?: false) ||
                        (it.bank?.contains(query, ignoreCase = true) ?: false)
            }
        }

        // 2. Type Filter
        if (type != "All") {
            list = list.filter { it.type == type }
        }

        // 3. Status Filter
        if (status != "All") {
            list = list.filter { it.status == status }
        }

        // 4. Sort
        when (sort) {
            "Date (Newest)" -> list.sortedByDescending { it.date }
            "Date (Oldest)" -> list.sortedBy { it.date }
            "Amount (High-Low)" -> list.sortedByDescending { it.amount }
            "Amount (Low-High)" -> list.sortedBy { it.amount }
            "Due Date" -> list.sortedBy { it.dueDate }
            else -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String) {
        _filterType.value = type
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    // CREATE
    fun createBorrowLendItem(item: BorrowLendItem) {
        viewModelScope.launch {
            val itemId = repository.insertBorrowLendItem(item)
            
            // Automatically generate transaction
            val defaultBank = bankAccounts.value.firstOrNull()
            val bankId = defaultBank?.id ?: 1
            val bankName = item.bank ?: defaultBank?.bankName ?: "Primary Bank"

            // Borrow: Money received (+), Lend: Money given out (-)
            val isIncome = item.type == "Borrowed"
            val transactionType = if (isIncome) "Borrow" else "Lend"
            
            val transaction = Transaction(
                amount = item.amount,
                type = transactionType,
                categoryName = if (isIncome) "Borrowing" else "Lending",
                merchantName = item.personName,
                paymentMethod = item.paymentMethod ?: "Cash",
                bankAccountId = bankId,
                bankAccountName = bankName,
                timestamp = item.date,
                notes = "Auto-generated for Borrow/Lend Record #${itemId}: ${item.notes ?: ""}"
            )
            repository.insertTransaction(transaction)
        }
    }

    // EDIT
    fun updateBorrowLendItem(item: BorrowLendItem) {
        viewModelScope.launch {
            repository.updateBorrowLendItem(item)
        }
    }

    // DELETE
    fun deleteBorrowLendItem(item: BorrowLendItem) {
        viewModelScope.launch {
            repository.deleteBorrowLendItem(item)
        }
    }

    // PARTIAL PAYMENT / REPAYMENT
    fun recordPayment(item: BorrowLendItem, paidAmount: Double, notes: String) {
        viewModelScope.launch {
            val newPaidAmount = item.paidAmount + paidAmount
            val newStatus = if (newPaidAmount >= item.amount) {
                "Completed"
            } else {
                "Partially Paid"
            }

            val updatedItem = item.copy(
                paidAmount = newPaidAmount,
                status = newStatus,
                notes = (item.notes ?: "") + "\n[Logged payment of ₹$paidAmount: $notes]"
            )
            repository.updateBorrowLendItem(updatedItem)

            // Automatically generate transaction for payment
            val defaultBank = bankAccounts.value.firstOrNull()
            val bankId = defaultBank?.id ?: 1
            val bankName = item.bank ?: defaultBank?.bankName ?: "Primary Bank"

            // Repaying borrowed money: Expense (-), Collecting lended money: Income (+)
            val isRepaymentOfBorrowed = item.type == "Borrowed"
            val transactionType = if (isRepaymentOfBorrowed) "Expense" else "Income"
            val category = if (isRepaymentOfBorrowed) "Debt Repayment" else "Debt Collection"

            val transaction = Transaction(
                amount = paidAmount,
                type = transactionType,
                categoryName = category,
                merchantName = item.personName,
                paymentMethod = item.paymentMethod ?: "Cash",
                bankAccountId = bankId,
                bankAccountName = bankName,
                timestamp = System.currentTimeMillis(),
                notes = "Repayment log for Borrow/Lend Record #${item.id}: $notes"
            )
            repository.insertTransaction(transaction)
        }
    }

    // MARK COMPLETED
    fun markCompleted(item: BorrowLendItem) {
        val remaining = item.amount - item.paidAmount
        if (remaining > 0) {
            recordPayment(item, remaining, "Marked as completed (Full payout logged)")
        } else {
            viewModelScope.launch {
                repository.updateBorrowLendItem(item.copy(status = "Completed"))
            }
        }
    }
}
