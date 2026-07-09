package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.BankAccount
import com.example.domain.model.BorrowLendItem
import com.example.domain.model.Category
import com.example.domain.model.Merchant
import com.example.domain.model.PrivateVaultState
import com.example.domain.model.SavingsGoal
import com.example.domain.model.Transaction
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

enum class FilterType {
    DAY, WEEK, MONTH, QUARTER, YEAR, CUSTOM
}

data class DateRange(val start: Long, val end: Long)

data class OverviewStats(
    val totalIncome: Double,
    val totalExpense: Double,
    val currentBalance: Double,
    val savings: Double,
    val vaultBalance: Double,
    val borrowed: Double,
    val lent: Double,
    val netWorth: Double
)

data class ChartSegment(
    val label: String,
    val amount: Double,
    val percentage: Float,
    val colorHex: String
)

data class MonthlyBarData(
    val monthName: String,
    val income: Double,
    val expense: Double
)

data class DailyTrendPoint(
    val dateLabel: String,
    val amount: Double,
    val timestamp: Long
)

data class SavingsTrendPoint(
    val dateLabel: String,
    val accumulatedAmount: Double
)

data class HeatmapDay(
    val dayOfMonth: Int,
    val dateLabel: String,
    val isIncomeDay: Boolean,
    val isExpenseDay: Boolean,
    val totalExpense: Double,
    val totalIncome: Double,
    val intensity: Float // 0.0f (no spending) to 1.0f (highest spending day)
)

data class CategoryAnalyticItem(
    val categoryName: String,
    val colorHex: String,
    val transactionCount: Int,
    val totalSpent: Double,
    val budget: Double,
    val budgetUsagePercentage: Float,
    val averageSpent: Double,
    val changeVsLastMonth: Double // Positive means spent more, negative means spent less
)

data class MerchantAnalyticItem(
    val merchantName: String,
    val visitCount: Int,
    val totalSpending: Double,
    val averagePurchase: Double,
    val visitsLast30Days: Int
)

data class BankAnalyticItem(
    val bankName: String,
    val balance: Double,
    val transactionCount: Int,
    val transferCount: Int,
    val isHighestBalance: Boolean,
    val isMostUsed: Boolean
)

data class PaymentMethodAnalyticItem(
    val methodName: String,
    val transactionCount: Int,
    val totalAmount: Double,
    val usagePercentage: Float
)

data class SavingsAnalyticSummary(
    val overallProgress: Float, // 0.0 to 1.0
    val completionRate: Float,  // 0.0 to 1.0
    val averageSavings: Double,
    val projectedCompletionDays: Int,
    val activeGoalsCount: Int,
    val completedGoalsCount: Int
)

data class BorrowLendAnalyticSummary(
    val outstandingAmount: Double,
    val collectedAmount: Double,
    val pendingAmount: Double,
    val overdueAmount: Double,
    val recoveryPercentage: Float
)

data class InsightCard(
    val title: String,
    val value: String,
    val description: String,
    val type: String // "alert", "success", "info"
)

data class FinancialHealthScore(
    val score: Int, // 0 to 100
    val categoryScores: Map<String, Int>,
    val explanation: String,
    val recommendation: String,
    val rating: String // "Excellent", "Good", "Fair", "Needs Improvement"
)

class AnalyticsViewModel(private val repository: VaultRepository) : ViewModel() {

    // Filter parameters
    private val _filterType = MutableStateFlow(FilterType.MONTH)
    val filterType = _filterType.asStateFlow()

    private val _customDateRange = MutableStateFlow<DateRange?>(null)
    val customDateRange = _customDateRange.asStateFlow()

    // Database source streams
    val transactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankAccounts: StateFlow<List<BankAccount>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultState: StateFlow<PrivateVaultState?> = repository.getVaultStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val borrowLendItems: StateFlow<List<BorrowLendItem>> = repository.getAllBorrowLendItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val merchants: StateFlow<List<Merchant>> = repository.getAllMerchants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated range based on filters
    val activeDateRange: StateFlow<DateRange> = combine(_filterType, _customDateRange) { type, custom ->
        getFilterTimestamps(type, custom)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DateRange(0, System.currentTimeMillis()))

    // Filtered transaction list
    val filteredTransactions: StateFlow<List<Transaction>> = combine(transactions, activeDateRange) { txs, range ->
        txs.filter { it.timestamp in range.start..range.end && !it.isDeleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 1. Overview Dashboard Stats
    val overviewStats: StateFlow<OverviewStats> = combine(
        filteredTransactions, bankAccounts, savingsGoals, vaultState, borrowLendItems
    ) { filteredTxs, accounts, goals, vault, borrowLend ->
        val income = filteredTxs.filter { it.type == "Income" }.sumOf { it.amount }
        val expense = filteredTxs.filter { it.type == "Expense" }.sumOf { it.amount }
        val accBalance = accounts.sumOf { it.balance }
        val totalSavings = goals.sumOf { it.currentSavedAmount }
        val vltBalance = vault?.balance ?: 0.0

        val borrowed = borrowLend.filter { it.type == "Borrowed" && it.status != "Completed" }.sumOf { it.remainingAmount }
        val lent = borrowLend.filter { it.type == "Lended" && it.status != "Completed" }.sumOf { it.remainingAmount }

        val netWorth = accBalance + totalSavings + vltBalance + lent - borrowed

        OverviewStats(
            totalIncome = income,
            totalExpense = expense,
            currentBalance = accBalance,
            savings = totalSavings,
            vaultBalance = vltBalance,
            borrowed = borrowed,
            lent = lent,
            netWorth = netWorth
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

    // 2. Pie Chart: Category Distribution
    val categoryDistribution: StateFlow<List<ChartSegment>> = combine(
        filteredTransactions, categories
    ) { txs, cats ->
        val expenses = txs.filter { it.type == "Expense" }
        val totalExp = expenses.sumOf { it.amount }
        if (totalExp == 0.0) return@combine emptyList()

        val grouped = expenses.groupBy { it.categoryName }
        val catMap = cats.associateBy { it.name }

        grouped.map { (catName, catTxs) ->
            val sum = catTxs.sumOf { it.amount }
            val color = catMap[catName]?.colorHex ?: "#94A3B8"
            ChartSegment(
                label = catName,
                amount = sum,
                percentage = (sum / totalExp).toFloat(),
                colorHex = color
            )
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Bar Chart: Monthly Income vs Expense (Past 6 Months)
    val monthlyIncomeVsExpense: StateFlow<List<MonthlyBarData>> = transactions.map { txs ->
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val dataList = mutableListOf<MonthlyBarData>()

        for (i in 5 downTo 0) {
            val checkCal = Calendar.getInstance()
            checkCal.add(Calendar.MONTH, -i)
            val monthYearStr = format.format(checkCal.time)

            // Filter txs in that specific month and year
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

            val monthTxs = txs.filter { it.timestamp in startCal.timeInMillis..endCal.timeInMillis && !it.isDeleted }
            val inc = monthTxs.filter { it.type == "Income" }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type == "Expense" }.sumOf { it.amount }

            val simpleMonthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(checkCal.time)
            dataList.add(MonthlyBarData(simpleMonthLabel, inc, exp))
        }
        dataList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Line Chart: Daily Spending Trend
    val dailySpendingTrend: StateFlow<List<DailyTrendPoint>> = combine(
        filteredTransactions, activeDateRange
    ) { txs, range ->
        val expenses = txs.filter { it.type == "Expense" }
        val diffDays = ((range.end - range.start) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)

        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val points = mutableListOf<DailyTrendPoint>()

        if (diffDays <= 31) {
            // Group day-by-day
            val dayMillis = 24 * 60 * 60 * 1000L
            for (i in 0..diffDays) {
                val dayStart = range.start + i * dayMillis
                val dayEnd = dayStart + dayMillis - 1
                val label = dateFormat.format(Date(dayStart))
                val sum = expenses.filter { it.timestamp in dayStart..dayEnd }.sumOf { it.amount }
                points.add(DailyTrendPoint(label, sum, dayStart))
            }
        } else {
            // Group by week or month for large ranges
            val chunkCount = 10
            val chunkSize = (range.end - range.start) / chunkCount
            for (i in 0 until chunkCount) {
                val chunkStart = range.start + i * chunkSize
                val chunkEnd = chunkStart + chunkSize
                val label = dateFormat.format(Date(chunkStart))
                val sum = expenses.filter { it.timestamp in chunkStart..chunkEnd }.sumOf { it.amount }
                points.add(DailyTrendPoint(label, sum, chunkStart))
            }
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5. Area Chart: Savings Growth
    val savingsGrowth: StateFlow<List<SavingsTrendPoint>> = savingsGoals.map { goals ->
        val points = mutableListOf<SavingsTrendPoint>()
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("dd MMM", Locale.getDefault())

        val totalCurrentSaved = goals.sumOf { it.currentSavedAmount }
        if (totalCurrentSaved == 0.0) {
            return@map listOf(SavingsTrendPoint("Start", 0.0), SavingsTrendPoint("Now", 0.0))
        }

        // Generate 6 sample timeline progression steps for total savings
        val steps = 6
        for (i in 0 until steps) {
            cal.add(Calendar.DAY_OF_YEAR, -((steps - 1 - i) * 5))
            val label = format.format(cal.time)
            val cumulativeMock = (totalCurrentSaved / (steps - 1)) * i
            points.add(SavingsTrendPoint(label, cumulativeMock))
            cal.timeInMillis = System.currentTimeMillis() // Reset
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 6. Heatmap Data (Days of active month)
    val heatmapData: StateFlow<List<HeatmapDay>> = transactions.map { txs ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val daysList = mutableListOf<HeatmapDay>()
        val dayExpenses = mutableMapOf<Int, Double>()
        var maxExpenseSum = 1.0

        for (day in 1..maxDays) {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                time = startCal.time
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val dayTxs = txs.filter { it.timestamp in startCal.timeInMillis..endCal.timeInMillis && !it.isDeleted }
            val hasIncome = dayTxs.any { it.type == "Income" }
            val hasExpense = dayTxs.any { it.type == "Expense" }
            val expenseSum = dayTxs.filter { it.type == "Expense" }.sumOf { it.amount }
            val incomeSum = dayTxs.filter { it.type == "Income" }.sumOf { it.amount }

            dayExpenses[day] = expenseSum
            if (expenseSum > maxExpenseSum) {
                maxExpenseSum = expenseSum
            }

            val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            daysList.add(
                HeatmapDay(
                    dayOfMonth = day,
                    dateLabel = format.format(startCal.time),
                    isIncomeDay = hasIncome,
                    isExpenseDay = hasExpense,
                    totalExpense = expenseSum,
                    totalIncome = incomeSum,
                    intensity = 0f // will calculate next
                )
            )
        }

        // Calculate intensity based on highest spending day
        daysList.map { dayObj ->
            val spending = dayExpenses[dayObj.dayOfMonth] ?: 0.0
            dayObj.copy(intensity = if (maxExpenseSum > 0) (spending / maxExpenseSum).toFloat() else 0f)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7. Category Analytics
    val categoryAnalytics: StateFlow<List<CategoryAnalyticItem>> = combine(
        transactions, categories
    ) { txs, cats ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Current Month Txs
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val thisMonthStart = cal.timeInMillis
        val thisMonthTxs = txs.filter { it.timestamp >= thisMonthStart && !it.isDeleted }

        // Last Month Txs
        cal.add(Calendar.MONTH, -1)
        val lastMonthStart = cal.timeInMillis
        val lastMonthEnd = thisMonthStart - 1
        val lastMonthTxs = txs.filter { it.timestamp in lastMonthStart..lastMonthEnd && !it.isDeleted }

        cats.map { cat ->
            val thisMonthCatTxs = thisMonthTxs.filter { it.categoryName == cat.name && it.type == "Expense" }
            val lastMonthCatTxs = lastMonthTxs.filter { it.categoryName == cat.name && it.type == "Expense" }

            val totalSpent = thisMonthCatTxs.sumOf { it.amount }
            val lastMonthSpent = lastMonthCatTxs.sumOf { it.amount }

            val count = thisMonthCatTxs.size
            val avg = if (count > 0) totalSpent / count else 0.0
            val usagePct = if (cat.monthlyBudget > 0) (totalSpent / cat.monthlyBudget).toFloat() else 0f

            CategoryAnalyticItem(
                categoryName = cat.name,
                colorHex = cat.colorHex,
                transactionCount = count,
                totalSpent = totalSpent,
                budget = cat.monthlyBudget,
                budgetUsagePercentage = usagePct,
                averageSpent = avg,
                changeVsLastMonth = totalSpent - lastMonthSpent
            )
        }.sortedByDescending { it.totalSpent }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 8. Merchant Analytics
    val merchantAnalytics: StateFlow<List<MerchantAnalyticItem>> = combine(
        filteredTransactions, merchants
    ) { txs, merchList ->
        val expenses = txs.filter { it.type == "Expense" }
        val groupedByMerchant = expenses.groupBy { it.merchantName }

        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1000L

        groupedByMerchant.map { (mName, mTxs) ->
            val total = mTxs.sumOf { it.amount }
            val avg = if (mTxs.isNotEmpty()) total / mTxs.size else 0.0
            val visits30 = mTxs.count { it.timestamp >= thirtyDaysAgo }

            MerchantAnalyticItem(
                merchantName = mName.ifBlank { "Unknown Merchant" },
                visitCount = mTxs.size,
                totalSpending = total,
                averagePurchase = avg,
                visitsLast30Days = visits30
            )
        }.sortedByDescending { it.totalSpending }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 9. Bank Analytics
    val bankAnalytics: StateFlow<List<BankAnalyticItem>> = combine(
        transactions, bankAccounts
    ) { txs, accounts ->
        if (accounts.isEmpty()) return@combine emptyList()

        val maxBalance = accounts.maxOfOrNull { it.balance } ?: 0.0

        val grouped = txs.groupBy { it.bankAccountName }

        accounts.map { acc ->
            val accTxs = grouped[acc.bankName] ?: emptyList()
            val totalTxs = accTxs.size
            val transfers = accTxs.count { it.type == "Transfer" }

            BankAnalyticItem(
                bankName = acc.bankName,
                balance = acc.balance,
                transactionCount = totalTxs,
                transferCount = transfers,
                isHighestBalance = acc.balance == maxBalance && maxBalance > 0.0,
                isMostUsed = false // Will compute relative to others next
            )
        }.let { items ->
            val maxCount = items.maxOfOrNull { it.transactionCount } ?: 0
            items.map { it.copy(isMostUsed = it.transactionCount == maxCount && maxCount > 0) }
        }.sortedByDescending { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 10. Payment Method Analytics
    val paymentMethodAnalytics: StateFlow<List<PaymentMethodAnalyticItem>> = filteredTransactions.map { txs ->
        val totalAmount = txs.sumOf { it.amount }
        val methods = listOf("Cash", "PhonePe", "Google Pay", "Paytm", "FamPay", "Debit Card", "Credit Card", "UPI", "Bank Transfer")

        methods.map { method ->
            val methodTxs = txs.filter { it.paymentMethod.equals(method, ignoreCase = true) }
            val amount = methodTxs.sumOf { it.amount }
            val pct = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f

            PaymentMethodAnalyticItem(
                methodName = method,
                transactionCount = methodTxs.size,
                totalAmount = amount,
                usagePercentage = pct
            )
        }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 11. Savings Analytics
    val savingsAnalytics: StateFlow<SavingsAnalyticSummary> = savingsGoals.map { goals ->
        if (goals.isEmpty()) {
            return@map SavingsAnalyticSummary(0f, 0f, 0.0, 0, 0, 0)
        }

        val totalTarget = goals.sumOf { it.targetAmount }
        val totalSaved = goals.sumOf { it.currentSavedAmount }
        val progress = if (totalTarget > 0.0) (totalSaved / totalTarget).toFloat() else 0f

        val completed = goals.count { it.status == "Completed" || it.currentSavedAmount >= it.targetAmount }
        val rate = completed.toFloat() / goals.size

        val averageSaved = goals.map { it.currentSavedAmount }.average().let { if (it.isNaN()) 0.0 else it }

        // Projected Completion: simple run rate estimation
        val activeGoals = goals.filter { it.status == "Active" && it.currentSavedAmount < it.targetAmount }
        var projectedDays = 0
        if (activeGoals.isNotEmpty()) {
            // Assume default standard of 45 days projection if rate is slow, or look at date target
            val now = System.currentTimeMillis()
            val totalRemaining = activeGoals.sumOf { it.targetAmount - it.currentSavedAmount }
            val avgTargetDate = activeGoals.map { it.targetDate }.average()
            projectedDays = if (!avgTargetDate.isNaN() && avgTargetDate > now) {
                ((avgTargetDate - now) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            } else {
                30
            }
        }

        SavingsAnalyticSummary(
            overallProgress = progress,
            completionRate = rate,
            averageSavings = averageSaved,
            projectedCompletionDays = projectedDays,
            activeGoalsCount = goals.count { it.status == "Active" },
            completedGoalsCount = completed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavingsAnalyticSummary(0f, 0f, 0.0, 0, 0, 0))

    // 12. Borrow/Lend Analytics
    val borrowLendAnalytics: StateFlow<BorrowLendAnalyticSummary> = borrowLendItems.map { items ->
        if (items.isEmpty()) {
            return@map BorrowLendAnalyticSummary(0.0, 0.0, 0.0, 0.0, 0f)
        }

        val outstanding = items.filter { it.status != "Completed" && it.status != "Cancelled" }.sumOf { it.remainingAmount }
        val collected = items.filter { it.type == "Lended" }.sumOf { it.paidAmount }
        val pending = items.filter { it.status == "Pending" }.sumOf { it.remainingAmount }

        val now = System.currentTimeMillis()
        val overdue = items.filter { it.status != "Completed" && it.status != "Cancelled" && it.dueDate < now }.sumOf { it.remainingAmount }

        val totalLent = items.filter { it.type == "Lended" }.sumOf { it.amount }
        val recoveryPct = if (totalLent > 0) (collected / totalLent).toFloat() else 0f

        BorrowLendAnalyticSummary(
            outstandingAmount = outstanding,
            collectedAmount = collected,
            pendingAmount = pending,
            overdueAmount = overdue,
            recoveryPercentage = recoveryPct
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BorrowLendAnalyticSummary(0.0, 0.0, 0.0, 0.0, 0f))

    // 13. Financial Health Score
    val financialHealthScore: StateFlow<FinancialHealthScore> = combine(
        filteredTransactions, bankAccounts, savingsGoals, borrowLendItems, categoryAnalytics
    ) { txs, accounts, goals, borrowLend, catAnalytic ->
        // Budget consistency (out of 30)
        var budgetScore = 30
        val overBudgetCatCount = catAnalytic.count { it.budget > 0 && it.totalSpent > it.budget }
        val budgetCatsCount = catAnalytic.count { it.budget > 0 }
        if (budgetCatsCount > 0) {
            val ratio = overBudgetCatCount.toFloat() / budgetCatsCount
            budgetScore = (30 * (1.0f - ratio)).toInt().coerceIn(0, 30)
        }

        // Savings consistency (out of 30)
        var savingsScore = 15
        val income = txs.filter { it.type == "Income" }.sumOf { it.amount }
        val saved = goals.sumOf { it.currentSavedAmount }
        if (income > 0) {
            val ratio = saved / income
            savingsScore = if (ratio >= 0.2) 30 else (30 * (ratio / 0.2)).toInt().coerceIn(0, 30)
        } else if (saved > 0) {
            savingsScore = 25
        }

        // Borrow ratio (out of 20)
        var debtScore = 20
        val borrowed = borrowLend.filter { it.type == "Borrowed" && it.status != "Completed" }.sumOf { it.remainingAmount }
        val assets = accounts.sumOf { it.balance } + saved
        if (assets > 0) {
            val ratio = borrowed / assets
            debtScore = when {
                ratio == 0.0 -> 20
                ratio < 0.1 -> 18
                ratio < 0.3 -> 15
                ratio < 0.5 -> 10
                else -> 5
            }
        }

        // Spending consistency (out of 20)
        val expenses = txs.filter { it.type == "Expense" }
        val totalExp = expenses.sumOf { it.amount }
        var spendingScore = 20
        if (income > 0) {
            val ratio = totalExp / income
            spendingScore = when {
                ratio < 0.5 -> 20
                ratio < 0.7 -> 16
                ratio < 0.9 -> 12
                else -> 5
            }
        }

        val totalScore = budgetScore + savingsScore + debtScore + spendingScore

        val (rating, explanation, recommendation) = when {
            totalScore >= 80 -> Triple(
                "Excellent",
                "Fantastic! You have excellent control over your budget and save a solid percentage of your income. Debt is perfectly managed.",
                "Keep doing what you are doing. Consider routing your surplus funds into high-priority long-term savings goals."
            )
            totalScore >= 60 -> Triple(
                "Good",
                "Your financial health is in a good place. You are saving consistently, though some categories are occasionally over budget.",
                "Review your highest-spending categories to plug minor leakages. Put small budgets in place for entertainment."
            )
            totalScore >= 40 -> Triple(
                "Fair",
                "You are maintaining basic balances, but your savings are lower than average, or debt could be slightly high relative to assets.",
                "Set automated reminders to put money in your private vault or savings goals first thing every month. Lower non-essential spends."
            )
            else -> Triple(
                "Needs Improvement",
                "High debt levels, low savings, or regularly exceeding budget limits has significantly reduced your score.",
                "Create strict Category Budgets. Avoid taking any new borrowings. Prioritize paying off pending outstanding borrow items."
            )
        }

        FinancialHealthScore(
            score = totalScore,
            categoryScores = mapOf(
                "Budget" to budgetScore,
                "Savings" to savingsScore,
                "Borrow" to debtScore,
                "Spending" to spendingScore
            ),
            explanation = explanation,
            recommendation = recommendation,
            rating = rating
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialHealthScore(80, emptyMap(), "", "", "Excellent"))

    // 14. Financial Insights list
    val insights: StateFlow<List<InsightCard>> = combine(
        filteredTransactions, savingsGoals, merchantAnalytics, heatmapData
    ) { txs, goals, merchs, heatmap ->
        val cards = mutableListOf<InsightCard>()

        val expenses = txs.filter { it.type == "Expense" }
        val incomes = txs.filter { it.type == "Income" }

        // Biggest Expense
        val maxExpense = expenses.maxByOrNull { it.amount }
        if (maxExpense != null) {
            cards.add(
                InsightCard(
                    title = "Biggest Expense",
                    value = "₹${maxExpense.amount}",
                    description = "Spent on ${maxExpense.categoryName} at ${maxExpense.merchantName.ifBlank { "Store" }}.",
                    type = "alert"
                )
            )
        }

        // Largest Income
        val maxIncome = incomes.maxByOrNull { it.amount }
        if (maxIncome != null) {
            cards.add(
                InsightCard(
                    title = "Largest Income",
                    value = "₹${maxIncome.amount}",
                    description = "Received from ${maxIncome.categoryName}.",
                    type = "success"
                )
            )
        }

        // Most Used Merchant
        val topMerch = merchs.firstOrNull()
        if (topMerch != null) {
            cards.add(
                InsightCard(
                    title = "Most Used Merchant",
                    value = topMerch.merchantName,
                    description = "Visited ${topMerch.visitCount} times, with ₹${topMerch.totalSpending} total spending.",
                    type = "info"
                )
            )
        }

        // Fastest growing savings goal (highest current saved amount or completed)
        val bestGoal = goals.maxByOrNull { it.currentSavedAmount }
        if (bestGoal != null) {
            cards.add(
                InsightCard(
                    title = "Fastest Growing Savings Goal",
                    value = bestGoal.name,
                    description = "Accumulated ₹${bestGoal.currentSavedAmount} of ₹${bestGoal.targetAmount} target.",
                    type = "success"
                )
            )
        }

        // Highest spending day of week/month
        val maxSpendDay = heatmap.maxByOrNull { it.totalExpense }
        if (maxSpendDay != null && maxSpendDay.totalExpense > 0) {
            cards.add(
                InsightCard(
                    title = "Highest Spending Day",
                    value = maxSpendDay.dateLabel,
                    description = "Total daily outflows peaked at ₹${maxSpendDay.totalExpense}.",
                    type = "alert"
                )
            )
        }

        if (cards.isEmpty()) {
            cards.add(
                InsightCard(
                    title = "Getting Started",
                    value = "Add Transactions",
                    description = "Start adding your daily spends and incomes to unlock rich financial insights.",
                    type = "info"
                )
            )
        }

        cards
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun setFilterType(type: FilterType) {
        _filterType.value = type
        if (type != FilterType.CUSTOM) {
            _customDateRange.value = null
        }
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _filterType.value = FilterType.CUSTOM
        _customDateRange.value = DateRange(start, end)
    }

    private fun getFilterTimestamps(filterType: FilterType, customRange: DateRange?): DateRange {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now

        return when (filterType) {
            FilterType.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val end = calendar.timeInMillis
                DateRange(start, end)
            }
            FilterType.WEEK -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                DateRange(calendar.timeInMillis, now)
            }
            FilterType.MONTH -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                DateRange(calendar.timeInMillis, now)
            }
            FilterType.QUARTER -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.DAY_OF_YEAR, -90)
                DateRange(calendar.timeInMillis, now)
            }
            FilterType.YEAR -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.add(Calendar.DAY_OF_YEAR, -365)
                DateRange(calendar.timeInMillis, now)
            }
            FilterType.CUSTOM -> {
                if (customRange != null) {
                    customRange
                } else {
                    DateRange(0, now)
                }
            }
        }
    }
}
