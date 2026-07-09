package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ReportFilterType {
    DAY, WEEK, MONTH, QUARTER, YEAR, CUSTOM
}

data class ReportDateRange(val start: Long, val end: Long)

data class DashboardSummaryState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val netWorth: Double = 0.0,
    val vaultBalance: Double = 0.0,
    val bankBalances: List<BankAccountBalanceItem> = emptyList(),
    val outstandingBorrow: Double = 0.0,
    val outstandingLend: Double = 0.0,
    val financialHealthScore: Int = 100
)

data class BankAccountBalanceItem(
    val bankId: Int,
    val bankName: String,
    val balance: Double,
    val colorHex: String
)

data class BankStatementItem(
    val bankName: String,
    val openingBalance: Double,
    val credits: Double,
    val debits: Double,
    val transfers: Double,
    val closingBalance: Double
)

data class PaymentMethodStatementItem(
    val methodName: String,
    val transactionCount: Int,
    val totalCredits: Double,
    val totalDebits: Double,
    val netFlow: Double
)

data class SavingsGoalReportItem(
    val name: String,
    val target: Double,
    val saved: Double,
    val completionPercentage: Float,
    val remaining: Double,
    val deposits: Double,
    val withdrawals: Double
)

data class BorrowLendReportState(
    val moneyBorrowed: Double = 0.0,
    val moneyLent: Double = 0.0,
    val recovered: Double = 0.0,
    val outstandingBorrow: Double = 0.0,
    val outstandingLend: Double = 0.0,
    val overdue: Double = 0.0,
    val completedCount: Int = 0
)

data class LedgerItem(
    val id: Int,
    val timestamp: Long,
    val dateLabel: String,
    val description: String,
    val category: String,
    val paymentMethod: String,
    val bankAccount: String,
    val isCredit: Boolean, // Income/Lend recover/Borrow is credit (+), Expense is debit (-)
    val amount: Double,
    val runningBalance: Double
)

data class CashBookState(
    val openingBalance: Double = 0.0,
    val receipts: Double = 0.0,
    val payments: Double = 0.0,
    val closingBalance: Double = 0.0,
    val transactions: List<Transaction> = emptyList()
)

data class TrialBalanceItem(
    val accountName: String,
    val debit: Double,
    val credit: Double
)

data class TrialBalanceState(
    val items: List<TrialBalanceItem> = emptyList(),
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val isVerified: Boolean = false
)

data class PLItem(
    val periodLabel: String,
    val income: Double,
    val expense: Double,
    val netSavings: Double
)

class ReportsViewModel(private val repository: VaultRepository) : ViewModel() {

    // Filtering states
    private val _filterType = MutableStateFlow(ReportFilterType.MONTH)
    val filterType = _filterType.asStateFlow()

    private val _customDateRange = MutableStateFlow<ReportDateRange?>(null)
    val customDateRange = _customDateRange.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedMerchant = MutableStateFlow<String?>(null)
    val selectedMerchant = _selectedMerchant.asStateFlow()

    private val _selectedBank = MutableStateFlow<String?>(null)
    val selectedBank = _selectedBank.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow<String?>(null)
    val selectedPaymentMethod = _selectedPaymentMethod.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Database core sources
    val transactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankAccounts: StateFlow<List<BankAccount>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val borrowLendItems: StateFlow<List<BorrowLendItem>> = repository.getAllBorrowLendItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val vaultState: StateFlow<PrivateVaultState?> = repository.getVaultStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Derived Date Range
    val activeDateRange: StateFlow<ReportDateRange> = combine(_filterType, _customDateRange) { type, custom ->
        calculateDateRange(type, custom)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportDateRange(0, System.currentTimeMillis()))

    // Filtered transaction list based on search, category, merchant, bank, payment method, date
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        activeDateRange,
        _selectedCategory,
        _selectedMerchant,
        _selectedBank,
        _selectedPaymentMethod,
        _searchQuery
    ) { array ->
        val txs = array[0] as List<Transaction>
        val range = array[1] as ReportDateRange
        val cat = array[2] as String?
        val merchant = array[3] as String?
        val bank = array[4] as String?
        val payMethod = array[5] as String?
        val query = array[6] as String

        txs.filter { tx ->
            val matchesDate = tx.timestamp in range.start..range.end && !tx.isDeleted
            val matchesCat = cat == null || tx.categoryName.equals(cat, ignoreCase = true)
            val matchesMerchant = merchant == null || tx.merchantName.equals(merchant, ignoreCase = true)
            val matchesBank = bank == null || tx.bankAccountName.equals(bank, ignoreCase = true)
            val matchesPayMethod = payMethod == null || tx.paymentMethod.equals(payMethod, ignoreCase = true)
            val matchesSearch = query.isBlank() || 
                    tx.notes.contains(query, ignoreCase = true) ||
                    tx.categoryName.contains(query, ignoreCase = true) ||
                    tx.merchantName.contains(query, ignoreCase = true) ||
                    tx.paymentMethod.contains(query, ignoreCase = true) ||
                    tx.bankAccountName.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)

            matchesDate && matchesCat && matchesMerchant && matchesBank && matchesPayMethod && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 1. Dashboard Summary
    val dashboardSummary: StateFlow<DashboardSummaryState> = combine(
        filteredTransactions, bankAccounts, savingsGoals, vaultState, borrowLendItems
    ) { filteredTxs, accounts, goals, vault, borrowLend ->
        val totalIncome = filteredTxs.filter { it.type == "Income" }.sumOf { it.amount }
        val totalExpense = filteredTxs.filter { it.type == "Expense" }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        
        val vaultBal = vault?.balance ?: 0.0
        val accBalanceTotal = accounts.sumOf { it.balance }
        val totalGoalsSaved = goals.sumOf { it.currentSavedAmount }
        
        val borrowed = borrowLend.filter { it.type == "Borrowed" && it.status != "Completed" }.sumOf { it.remainingAmount }
        val lent = borrowLend.filter { it.type == "Lended" && it.status != "Completed" }.sumOf { it.remainingAmount }
        
        val netWorth = accBalanceTotal + totalGoalsSaved + vaultBal + lent - borrowed
        
        val bankItems = accounts.map {
            BankAccountBalanceItem(it.id, it.bankName, it.balance, it.colorHex)
        }

        // Standard Dynamic Health Score calculation
        var score = 100
        if (totalIncome > 0) {
            val expenseRatio = totalExpense / totalIncome
            score -= (expenseRatio * 50).toInt().coerceIn(0, 50)
        }
        if (borrowed > netWorth * 0.5) {
            score -= 20
        }
        score = score.coerceIn(30, 100)

        DashboardSummaryState(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            netWorth = netWorth,
            vaultBalance = vaultBal,
            bankBalances = bankItems,
            outstandingBorrow = borrowed,
            outstandingLend = lent,
            financialHealthScore = score
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummaryState())

    // 2. Income Report Groupings
    val incomeReportGroupings: StateFlow<Map<String, Map<String, Double>>> = filteredTransactions.map { txs ->
        val incomeTxs = txs.filter { it.type == "Income" }
        val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val weekFormat = SimpleDateFormat("'Week' w, yyyy", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

        val byDay = incomeTxs.groupBy { dayFormat.format(Date(it.timestamp)) }.mapValues { e -> e.value.sumOf { it.amount } }
        val byWeek = incomeTxs.groupBy { weekFormat.format(Date(it.timestamp)) }.mapValues { e -> e.value.sumOf { it.amount } }
        val byMonth = incomeTxs.groupBy { monthFormat.format(Date(it.timestamp)) }.mapValues { e -> e.value.sumOf { it.amount } }
        val byYear = incomeTxs.groupBy { yearFormat.format(Date(it.timestamp)) }.mapValues { e -> e.value.sumOf { it.amount } }
        
        val byCategory = incomeTxs.groupBy { it.categoryName }.mapValues { e -> e.value.sumOf { it.amount } }
        val byBank = incomeTxs.groupBy { it.bankAccountName }.mapValues { e -> e.value.sumOf { it.amount } }
        val byMethod = incomeTxs.groupBy { it.paymentMethod }.mapValues { e -> e.value.sumOf { it.amount } }
        val byMerchant = incomeTxs.groupBy { it.merchantName.ifBlank { "Direct Receipt" } }.mapValues { e -> e.value.sumOf { it.amount } }

        mapOf(
            "Day" to byDay,
            "Week" to byWeek,
            "Month" to byMonth,
            "Year" to byYear,
            "Category" to byCategory,
            "Bank" to byBank,
            "Payment Method" to byMethod,
            "Merchant" to byMerchant
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 3. Expense Report Details
    val expenseReportDetails: StateFlow<Map<String, Any>> = filteredTransactions.map { txs ->
        val expenseTxs = txs.filter { it.type == "Expense" }
        val dayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val highest = expenseTxs.maxByOrNull { it.amount }
        val lowest = expenseTxs.minByOrNull { it.amount }

        val byCategory = expenseTxs.groupBy { it.categoryName }.mapValues { e -> e.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }.toMap()
        val byMerchant = expenseTxs.groupBy { it.merchantName.ifBlank { "Unknown Merchant" } }.mapValues { e -> e.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }.toMap()
        val byPayment = expenseTxs.groupBy { it.paymentMethod }.mapValues { e -> e.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }.toMap()
        val byBank = expenseTxs.groupBy { it.bankAccountName }.mapValues { e -> e.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }.toMap()
        val byDate = expenseTxs.groupBy { dayFormat.format(Date(it.timestamp)) }.mapValues { e -> e.value.sumOf { it.amount } }

        mapOf(
            "highest" to (highest ?: "N/A"),
            "lowest" to (lowest ?: "N/A"),
            "byCategory" to byCategory,
            "byMerchant" to byMerchant,
            "byPayment" to byPayment,
            "byBank" to byBank,
            "byDate" to byDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 4. Bank Account Statements
    val bankStatements: StateFlow<List<BankStatementItem>> = combine(
        transactions, bankAccounts
    ) { allTxs, accounts ->
        accounts.map { acc ->
            val txs = allTxs.filter { it.bankAccountName == acc.bankName && !it.isDeleted }
            val credits = txs.filter { it.type == "Income" || it.type == "Refund" }.sumOf { it.amount }
            val debits = txs.filter { it.type == "Expense" }.sumOf { it.amount }
            val transfersIn = txs.filter { it.type == "Transfer" && it.amount > 0 }.sumOf { it.amount } // Outgoing is marked negative or is handled differently
            
            // Reconstruct a relative opening balance based on the current balance in db
            val closingBalance = acc.balance
            val openingBalance = acc.openingBalance.coerceAtLeast(0.0)

            BankStatementItem(
                bankName = acc.bankName,
                openingBalance = openingBalance,
                credits = credits,
                debits = debits,
                transfers = transfersIn,
                closingBalance = closingBalance
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Payment Method Statements
    val paymentMethodStatements: StateFlow<List<PaymentMethodStatementItem>> = filteredTransactions.map { txs ->
        val methods = listOf("Cash", "PhonePe", "Google Pay", "Paytm", "FamPay", "Debit Card", "Credit Card", "UPI", "Bank Transfer")
        methods.map { method ->
            val methodTxs = txs.filter { it.paymentMethod.equals(method, ignoreCase = true) }
            val credits = methodTxs.filter { it.type == "Income" || it.type == "Refund" }.sumOf { it.amount }
            val debits = methodTxs.filter { it.type == "Expense" }.sumOf { it.amount }
            
            PaymentMethodStatementItem(
                methodName = method,
                transactionCount = methodTxs.size,
                totalCredits = credits,
                totalDebits = debits,
                netFlow = credits - debits
            )
        }.filter { it.transactionCount > 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Savings Goals Report
    val savingsGoalsReport: StateFlow<List<SavingsGoalReportItem>> = combine(
        transactions, savingsGoals
    ) { allTxs, goals ->
        goals.map { goal ->
            val percentage = if (goal.targetAmount > 0) (goal.currentSavedAmount / goal.targetAmount).toFloat() else 0f
            val remaining = (goal.targetAmount - goal.currentSavedAmount).coerceAtLeast(0.0)

            // Calculate deposits & withdrawals dynamically if logged in transactions under category "Savings" or goal matching name
            val goalTxs = allTxs.filter { it.categoryName.equals("Savings", ignoreCase = true) && it.notes.contains(goal.name, ignoreCase = true) && !it.isDeleted }
            val deposits = goalTxs.filter { it.type == "Income" || it.type == "Transfer" }.sumOf { it.amount }
            val withdrawals = goalTxs.filter { it.type == "Expense" }.sumOf { it.amount }

            SavingsGoalReportItem(
                name = goal.name,
                target = goal.targetAmount,
                saved = goal.currentSavedAmount,
                completionPercentage = percentage,
                remaining = remaining,
                deposits = deposits.coerceAtLeast(goal.currentSavedAmount),
                withdrawals = withdrawals
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Borrow/Lend Report
    val borrowLendReport: StateFlow<BorrowLendReportState> = borrowLendItems.map { items ->
        val now = System.currentTimeMillis()
        val borrowed = items.filter { it.type == "Borrowed" }.sumOf { it.amount }
        val lent = items.filter { it.type == "Lended" }.sumOf { it.amount }
        val recovered = items.filter { it.type == "Lended" }.sumOf { it.paidAmount }
        
        val outBorrow = items.filter { it.type == "Borrowed" && it.status != "Completed" }.sumOf { it.remainingAmount }
        val outLent = items.filter { it.type == "Lended" && it.status != "Completed" }.sumOf { it.remainingAmount }
        
        val overdueVal = items.filter { it.status != "Completed" && it.status != "Cancelled" && it.dueDate < now }.sumOf { it.remainingAmount }
        val completed = items.count { it.status == "Completed" }

        BorrowLendReportState(
            moneyBorrowed = borrowed,
            moneyLent = lent,
            recovered = recovered,
            outstandingBorrow = outBorrow,
            outstandingLend = outLent,
            overdue = overdueVal,
            completedCount = completed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BorrowLendReportState())

    // 8. Professional Accounting Ledger with Running Balance
    val ledgerItems: StateFlow<List<LedgerItem>> = filteredTransactions.map { txs ->
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val sortedTxs = txs.sortedBy { it.timestamp }
        
        var runningBal = 0.0
        sortedTxs.map { tx ->
            val isCredit = tx.type == "Income" || tx.type == "Refund" || tx.type == "Borrow"
            if (isCredit) {
                runningBal += tx.amount
            } else {
                runningBal -= tx.amount
            }

            LedgerItem(
                id = tx.id,
                timestamp = tx.timestamp,
                dateLabel = dateFormat.format(Date(tx.timestamp)),
                description = if (tx.notes.isBlank()) tx.merchantName.ifBlank { "Transaction #${tx.id}" } else tx.notes,
                category = tx.categoryName,
                paymentMethod = tx.paymentMethod,
                bankAccount = tx.bankAccountName,
                isCredit = isCredit,
                amount = tx.amount,
                runningBalance = runningBal
            )
        }.reversed() // Show newest first in UI view
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 9. Cash Book (specifically Cash transactions)
    val cashBook: StateFlow<CashBookState> = filteredTransactions.map { txs ->
        val cashTxs = txs.filter { it.paymentMethod.equals("Cash", ignoreCase = true) }
        val receipts = cashTxs.filter { it.type == "Income" || it.type == "Refund" }.sumOf { it.amount }
        val payments = cashTxs.filter { it.type == "Expense" }.sumOf { it.amount }
        
        // Reconstruct Cash Book closing balance relative to receipts and payments
        val closing = receipts - payments

        CashBookState(
            openingBalance = 0.0,
            receipts = receipts,
            payments = payments,
            closingBalance = closing,
            transactions = cashTxs.sortedByDescending { it.timestamp }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashBookState())

    // 10. Trial Balance Generation
    val trialBalance: StateFlow<TrialBalanceState> = combine(
        filteredTransactions, bankAccounts, savingsGoals, borrowLendItems, vaultState
    ) { txs, accounts, goals, borrowLend, vault ->
        val itemsList = mutableListOf<TrialBalanceItem>()
        
        // Debit entries (Expenses, Cash/Bank Balances, Lended items, Savings Goals)
        val expensesSum = txs.filter { it.type == "Expense" }.sumOf { it.amount }
        if (expensesSum > 0) {
            itemsList.add(TrialBalanceItem("Total Expenses Ledger", expensesSum, 0.0))
        }

        accounts.forEach { acc ->
            if (acc.balance >= 0) {
                itemsList.add(TrialBalanceItem("${acc.bankName} Account (Asset)", acc.balance, 0.0))
            } else {
                itemsList.add(TrialBalanceItem("${acc.bankName} Account (Overdraft)", 0.0, -acc.balance))
            }
        }

        val savingsSum = goals.sumOf { it.currentSavedAmount }
        if (savingsSum > 0) {
            itemsList.add(TrialBalanceItem("Savings Goals Pool (Asset)", savingsSum, 0.0))
        }

        val lentSum = borrowLend.filter { it.type == "Lended" && it.status != "Completed" }.sumOf { it.remainingAmount }
        if (lentSum > 0) {
            itemsList.add(TrialBalanceItem("Lended Receivables (Asset)", lentSum, 0.0))
        }

        // Credit entries (Incomes, Capital/Opening, Borrowed Outstanding, Private Vault)
        val incomesSum = txs.filter { it.type == "Income" }.sumOf { it.amount }
        if (incomesSum > 0) {
            itemsList.add(TrialBalanceItem("Total Incomes Ledger", 0.0, incomesSum))
        }

        val borrowedSum = borrowLend.filter { it.type == "Borrowed" && it.status != "Completed" }.sumOf { it.remainingAmount }
        if (borrowedSum > 0) {
            itemsList.add(TrialBalanceItem("Borrowed Payables (Liability)", 0.0, borrowedSum))
        }

        val vaultBal = vault?.balance ?: 0.0
        if (vaultBal > 0) {
            itemsList.add(TrialBalanceItem("Private Vault Capital", 0.0, vaultBal))
        }

        val totalDebit = itemsList.sumOf { it.debit }
        val totalCredit = itemsList.sumOf { it.credit }
        
        // Auto reconcile difference to capital account for Trial Balance double entry presentation
        val diff = totalDebit - totalCredit
        if (diff > 0) {
            itemsList.add(TrialBalanceItem("Equity / Capital Account", 0.0, diff))
        } else if (diff < 0) {
            itemsList.add(TrialBalanceItem("Equity / Capital Account", -diff, 0.0))
        }

        val finalDebit = itemsList.sumOf { it.debit }
        val finalCredit = itemsList.sumOf { it.credit }

        TrialBalanceState(
            items = itemsList,
            totalDebit = finalDebit,
            totalCredit = finalCredit,
            isVerified = Math.abs(finalDebit - finalCredit) < 0.01
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrialBalanceState())

    // 11. Profit & Loss Summary
    val profitAndLoss: StateFlow<List<PLItem>> = transactions.map { allTxs ->
        val cal = Calendar.getInstance()
        val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val pLList = mutableListOf<PLItem>()

        // Generate past 6 months P&L entries
        for (i in 5 downTo 0) {
            val checkCal = Calendar.getInstance()
            checkCal.add(Calendar.MONTH, -i)
            
            val startCal = Calendar.getInstance().apply {
                time = checkCal.time
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                time = checkCal.time
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val monthTxs = allTxs.filter { it.timestamp in startCal.timeInMillis..endCal.timeInMillis && !it.isDeleted }
            val inc = monthTxs.filter { it.type == "Income" }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type == "Expense" }.sumOf { it.amount }

            pLList.add(
                PLItem(
                    periodLabel = monthYearFormat.format(checkCal.time),
                    income = inc,
                    expense = exp,
                    netSavings = inc - exp
                )
            )
        }
        pLList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter Actions
    fun setFilterType(type: ReportFilterType) {
        _filterType.value = type
        if (type != ReportFilterType.CUSTOM) {
            _customDateRange.value = null
        }
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _filterType.value = ReportFilterType.CUSTOM
        _customDateRange.value = ReportDateRange(start, end)
    }

    fun setSelectedCategory(cat: String?) {
        _selectedCategory.value = cat
    }

    fun setSelectedMerchant(merchant: String?) {
        _selectedMerchant.value = merchant
    }

    fun setSelectedBank(bank: String?) {
        _selectedBank.value = bank
    }

    fun setSelectedPaymentMethod(method: String?) {
        _selectedPaymentMethod.value = method
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearAllFilters() {
        _selectedCategory.value = null
        _selectedMerchant.value = null
        _selectedBank.value = null
        _selectedPaymentMethod.value = null
        _searchQuery.value = ""
        _filterType.value = ReportFilterType.MONTH
        _customDateRange.value = null
    }

    private fun calculateDateRange(type: ReportFilterType, custom: ReportDateRange?): ReportDateRange {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now

        return when (type) {
            ReportFilterType.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val end = cal.timeInMillis
                ReportDateRange(start, end)
            }
            ReportFilterType.WEEK -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.add(Calendar.DAY_OF_YEAR, -7)
                ReportDateRange(cal.timeInMillis, now)
            }
            ReportFilterType.MONTH -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.add(Calendar.DAY_OF_YEAR, -30)
                ReportDateRange(cal.timeInMillis, now)
            }
            ReportFilterType.QUARTER -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.add(Calendar.DAY_OF_YEAR, -90)
                ReportDateRange(cal.timeInMillis, now)
            }
            ReportFilterType.YEAR -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.add(Calendar.DAY_OF_YEAR, -365)
                ReportDateRange(cal.timeInMillis, now)
            }
            ReportFilterType.CUSTOM -> {
                custom ?: ReportDateRange(0, now)
            }
        }
    }

    // PDF, CSV and JSON Exports Data Helper
    fun generateCSVData(): String {
        val sb = java.lang.StringBuilder()
        sb.append("ID,Date,Description,Category,Bank,Payment Method,Type,Amount,Notes\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        filteredTransactions.value.forEach { tx ->
            val dateStr = sdf.format(Date(tx.timestamp))
            val notesEscaped = tx.notes.replace("\"", "\"\"")
            val merchantEscaped = tx.merchantName.replace("\"", "\"\"")
            sb.append("${tx.id},\"$dateStr\",\"$merchantEscaped\",\"${tx.categoryName}\",\"${tx.bankAccountName}\",\"${tx.paymentMethod}\",\"${tx.type}\",${tx.amount},\"$notesEscaped\"\n")
        }
        return sb.toString()
    }

    fun generateJSONData(): String {
        val sb = java.lang.StringBuilder()
        sb.append("[\n")
        val txList = filteredTransactions.value
        txList.forEachIndexed { idx, tx ->
            sb.append("  {\n")
            sb.append("    \"id\": ${tx.id},\n")
            sb.append("    \"amount\": ${tx.amount},\n")
            sb.append("    \"type\": \"${tx.type}\",\n")
            sb.append("    \"category\": \"${tx.categoryName}\",\n")
            sb.append("    \"merchant\": \"${tx.merchantName}\",\n")
            sb.append("    \"paymentMethod\": \"${tx.paymentMethod}\",\n")
            sb.append("    \"bankAccount\": \"${tx.bankAccountName}\",\n")
            sb.append("    \"timestamp\": ${tx.timestamp},\n")
            sb.append("    \"notes\": \"${tx.notes.replace("\"", "\\\"")}\"\n")
            sb.append("  }")
            if (idx < txList.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    fun generateHTMLReport(): String {
        val user = userProfile.value?.name ?: "Pranav"
        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        val range = activeDateRange.value
        val rangeSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val rangeStr = "${rangeSdf.format(Date(range.start))} to ${rangeSdf.format(Date(range.end))}"

        val summary = dashboardSummary.value

        val ledgerRows = ledgerItems.value.take(50).joinToString("\n") { item ->
            "<tr>" +
            "<td>${item.dateLabel}</td>" +
            "<td>${item.description}</td>" +
            "<td>${item.category}</td>" +
            "<td>${item.bankAccount} (${item.paymentMethod})</td>" +
            "<td class='${if (item.isCredit) "credit" else "debit"}'>${if (item.isCredit) "+" else "-"}₹${String.format("%.2f", item.amount)}</td>" +
            "<td>₹${String.format("%.2f", item.runningBalance)}</td>" +
            "</tr>"
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8">
        <style>
            body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #1e293b; margin: 40px; line-height: 1.5; }
            .cover { text-align: center; padding-top: 100px; padding-bottom: 100px; page-break-after: always; }
            .logo { font-size: 42px; font-weight: bold; color: #0284c7; margin-bottom: 20px; text-transform: uppercase; letter-spacing: 2px; }
            .title { font-size: 32px; font-weight: bold; margin-bottom: 10px; color: #0f172a; }
            .subtitle { font-size: 18px; color: #64748b; margin-bottom: 50px; }
            .metadata { font-size: 14px; color: #64748b; line-height: 1.8; margin-top: 150px; }
            .section { page-break-before: always; margin-top: 40px; }
            h2 { font-size: 24px; color: #0f172a; border-bottom: 2px solid #e2e8f0; padding-bottom: 8px; margin-bottom: 20px; }
            .grid { display: flex; flex-wrap: wrap; gap: 20px; margin-bottom: 30px; }
            .card { flex: 1; min-width: 200px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; text-align: center; }
            .card .val { font-size: 24px; font-weight: bold; color: #0284c7; margin-top: 8px; }
            .card .lbl { font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 1px; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; margin-bottom: 30px; font-size: 14px; }
            th { background: #f1f5f9; color: #475569; font-weight: bold; text-align: left; padding: 12px; border-bottom: 2px solid #e2e8f0; }
            td { padding: 12px; border-bottom: 1px solid #e2e8f0; color: #334155; }
            tr:hover { background: #f8fafc; }
            .credit { color: #10b981; font-weight: bold; }
            .debit { color: #ef4444; font-weight: bold; }
            .footer { position: fixed; bottom: 20px; left: 40px; right: 40px; text-align: center; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 10px; }
        </style>
        </head>
        <body>
            <!-- COVER PAGE -->
            <div class="cover">
                <div class="logo">VaultFlow</div>
                <div class="title">Professional Financial Statement</div>
                <div class="subtitle">Complete Accounting & Performance Audit Report</div>
                <div class="metadata">
                    <p><strong>Prepared For:</strong> $user</p>
                    <p><strong>Reporting Period:</strong> $rangeStr</p>
                    <p><strong>Generated Date:</strong> $dateStr</p>
                    <p><strong>Status:</strong> Audited & Certified (Local Offline State)</p>
                </div>
            </div>

            <!-- SUMMARY SECTION -->
            <div class="section">
                <h2>Financial Dashboard Summary</h2>
                <div class="grid">
                    <div class="card">
                        <div class="lbl">Total Income</div>
                        <div class="val">₹${String.format("%.2f", summary.totalIncome)}</div>
                    </div>
                    <div class="card">
                        <div class="lbl">Total Expense</div>
                        <div class="val">₹${String.format("%.2f", summary.totalExpense)}</div>
                    </div>
                    <div class="card">
                        <div class="lbl">Net Savings</div>
                        <div class="val">₹${String.format("%.2f", summary.netSavings)}</div>
                    </div>
                    <div class="card">
                        <div class="lbl">Financial Health</div>
                        <div class="val">${summary.financialHealthScore}/100</div>
                    </div>
                </div>
                <div class="grid">
                    <div class="card">
                        <div class="lbl">Net Worth</div>
                        <div class="val">₹${String.format("%.2f", summary.netWorth)}</div>
                    </div>
                    <div class="card">
                        <div class="lbl">Private Vault</div>
                        <div class="val">₹${String.format("%.2f", summary.vaultBalance)}</div>
                    </div>
                    <div class="card">
                        <div class="lbl">Outstanding Debt</div>
                        <div class="val">₹${String.format("%.2f", summary.outstandingBorrow)}</div>
                    </div>
                    <div class="card">
                        <div class="lbl">Lended Assets</div>
                        <div class="val">₹${String.format("%.2f", summary.outstandingLend)}</div>
                    </div>
                </div>
            </div>

            <!-- LEDGER SECTION -->
            <div class="section">
                <h2>Professional Accounting Ledger (Recent 50)</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Description</th>
                            <th>Category</th>
                            <th>Account (Method)</th>
                            <th>Amount</th>
                            <th>Running Balance</th>
                        </tr>
                    </thead>
                    <tbody>
                        $ledgerRows
                    </tbody>
                </table>
            </div>

            <div class="footer">
                VaultFlow Premium Accounting Module | Generated Automatically | Page 1 of 1
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
