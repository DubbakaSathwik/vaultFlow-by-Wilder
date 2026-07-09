package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Data Models for Dashboard
data class BalanceAccount(
    val name: String,
    val balance: Double,
    val type: String, // "Wallet", "Bank", "UPI", etc.
    val accentColorHex: String
)

data class TransactionItem(
    val id: String,
    val merchant: String,
    val category: String,
    val amount: Double,
    val isExpense: Boolean,
    val paymentMethod: String,
    val date: String,
    val time: String,
    val iconName: String
)

data class UpcomingPayment(
    val id: String,
    val service: String,
    val amount: Double,
    val remainingDays: Int,
    val dateString: String
)

data class AchievementBadge(
    val title: String,
    val description: String,
    val iconName: String,
    val unlocked: Boolean
)

data class SmartAlert(
    val id: String,
    val title: String,
    val description: String,
    val urgency: String, // "High", "Medium", "Info"
    val relativeTime: String
)

data class DashboardState(
    val totalBalance: Double = 124850.0,
    val monthlyIncome: Double = 45000.0,
    val monthlyExpense: Double = 18200.0,
    val savings: Double = 15000.0,
    
    // Accounts
    val accounts: List<BalanceAccount> = listOf(
        BalanceAccount("Overall Balance", 124850.0, "Total", "#38BDF8"),
        BalanceAccount("Cash", 4200.0, "Wallet", "#10B981"),
        BalanceAccount("SBI Savings", 45000.0, "Bank", "#0284C7"),
        BalanceAccount("HDFC Bank", 62000.0, "Bank", "#818CF8"),
        BalanceAccount("PhonePe Wallet", 3500.0, "UPI", "#A78BFA"),
        BalanceAccount("Paytm Wallet", 2150.0, "UPI", "#60A5FA"),
        BalanceAccount("FamPay Pocket", 8000.0, "UPI", "#34D399")
    ),
    val currentAccountIndex: Int = 0,
    
    // Budget
    val monthlyBudget: Double = 10000.0,
    val monthlyBudgetSpent: Double = 6800.0,
    
    // Savings Goal
    val savingsGoalTitle: String = "Electric Kettle",
    val savingsGoalTarget: Double = 900.0,
    val savingsGoalSaved: Double = 500.0,
    val savingsGoalDaysLeft: Int = 12,
    
    // Daily safe spending
    val dailySafeSpending: Double = 220.0,
    val dailySpent: Double = 110.0,
    
    // AI tips
    val aiTips: List<String> = listOf(
        "You spent 25% more on Food this week.",
        "Pro tip: You can save ₹300 this week by skipping a subscription.",
        "You are on track for your Electric Kettle goal!",
        "Remind: You have a Spotify premium renewal due tomorrow."
    ),
    val currentAiTipIndex: Int = 0,
    
    // Borrow/Lend summary
    val moneyToPay: Double = 2500.0,
    val moneyOwed: Double = 4800.0,
    
    // Upcoming bills
    val upcomingPayments: List<UpcomingPayment> = listOf(
        UpcomingPayment("1", "Netflix Premium", 649.0, 3, "12th July"),
        UpcomingPayment("2", "Spotify Individual", 119.0, 1, "10th July"),
        UpcomingPayment("3", "Pocket Money Transfer", 2000.0, 5, "14th July"),
        UpcomingPayment("4", "Electricity Bill", 1250.0, 8, "17th July"),
        UpcomingPayment("5", "WiFi Broadband", 799.0, 11, "20th July")
    ),
    
    // Transactions
    val recentTransactions: List<TransactionItem> = listOf(
        TransactionItem("1", "Starbucks Coffee", "Food", 350.0, true, "PhonePe", "Today", "10:30 AM", "coffee"),
        TransactionItem("2", "Book Store College", "College", 1200.0, true, "SBI Savings", "Today", "09:15 AM", "book"),
        TransactionItem("3", "ZARA Shopping", "Shopping", 2400.0, true, "Cards", "Yesterday", "06:45 PM", "shop"),
        TransactionItem("4", "Cinepolis", "Entertainment", 450.0, true, "FamPay", "Yesterday", "02:15 PM", "movie"),
        TransactionItem("5", "Metro Transit", "Transport", 80.0, true, "Cash", "07 July", "08:30 AM", "train")
    ),
    
    // Health score
    val financialHealthScore: Int = 91,
    val healthBudgetScore: Int = 95,
    val healthSavingsScore: Int = 88,
    val healthLoansScore: Int = 94,
    val healthSubscriptionsScore: Int = 90,
    
    // Achievements
    val achievements: List<AchievementBadge> = listOf(
        AchievementBadge("Budget Master", "Stayed within budget for 10 days", "shield", true),
        AchievementBadge("Halfway There", "Reached 50% Goal for Electric Kettle", "star", true),
        AchievementBadge("Thrifty Saver", "Saved ₹500 this month", "savings", true),
        AchievementBadge("Punctual payer", "Pay all bills on time", "schedule", true)
    ),
    
    // Smart Alerts
    val alerts: List<SmartAlert> = listOf(
        SmartAlert("1", "Pocket Money Expected", "Father's Pocket Money expected today", "Info", "Just now"),
        SmartAlert("2", "Spotify Due", "Spotify subscription auto-renews tomorrow", "Medium", "2 hours ago"),
        SmartAlert("3", "Loan Repayment Due", "Borrow repayment of ₹500 in 2 days to Amit", "High", "1 day ago"),
        SmartAlert("4", "Goal Nearing Completion", "Your Electric Kettle goal is 56% completed!", "Info", "3 days ago")
    )
)

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _isFabExpanded = MutableStateFlow(false)
    val isFabExpanded: StateFlow<Boolean> = _isFabExpanded.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

    init {
        // Auto-rotate AI Tips every 4 seconds
        viewModelScope.launch {
            while (true) {
                delay(4000)
                _state.update { current ->
                    current.copy(
                        currentAiTipIndex = (current.currentAiTipIndex + 1) % current.aiTips.size
                    )
                }
            }
        }
    }

    fun setAccountIndex(index: Int) {
        _state.update { it.copy(currentAccountIndex = index) }
    }

    fun toggleFab() {
        _isFabExpanded.update { !it }
    }

    fun setFabExpanded(expanded: Boolean) {
        _isFabExpanded.value = expanded
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBottomSheetVisible(visible: Boolean) {
        _isBottomSheetVisible.value = visible
    }

    fun triggerQuickAction(action: String) {
        // Safe interactive click with no logic needed
    }

    fun addDummyItem(action: String) {
        // Ready for future integration with TransactionScreen, Room database etc.
    }
}
