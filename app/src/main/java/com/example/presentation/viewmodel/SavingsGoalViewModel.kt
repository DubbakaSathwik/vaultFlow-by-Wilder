package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.BankAccount
import com.example.domain.model.Category
import com.example.domain.model.SavingsGoal
import com.example.domain.model.PrivateVaultState
import com.example.domain.model.VaultHistoryItem
import com.example.domain.model.Transaction
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavingsGoalViewModel(private val repository: VaultRepository) : ViewModel() {

    // Goals Flow
    val allGoals: StateFlow<List<SavingsGoal>> = repository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Private Vault State Flow
    val vaultState: StateFlow<PrivateVaultState> = repository.getVaultStateFlow()
        .map { it ?: PrivateVaultState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrivateVaultState())

    // Private Vault History Flow
    val vaultHistory: StateFlow<List<VaultHistoryItem>> = repository.getAllVaultHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bank Accounts Flow
    val bankAccounts: StateFlow<List<BankAccount>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Categories Flow
    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Pin lock state
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    init {
        // Initialize Private Vault state in DB if it doesn't exist
        viewModelScope.launch {
            val state = repository.getVaultStateOneShot()
            if (state == null) {
                repository.insertOrUpdateVaultState(PrivateVaultState(id = 1, balance = 0.0, pin = "1234", isBalanceHidden = false, isPinEnabled = true))
            }
        }
    }

    fun setVaultUnlocked(unlocked: Boolean) {
        _isVaultUnlocked.value = unlocked
    }

    fun verifyPin(pin: String): Boolean {
        val currentState = vaultState.value
        val matches = currentState.pin == pin || !currentState.isPinEnabled || currentState.pin.isEmpty()
        if (matches) {
            _isVaultUnlocked.value = true
        }
        return matches
    }

    fun setVaultPin(newPin: String, enablePin: Boolean) {
        viewModelScope.launch {
            val currentState = vaultState.value
            repository.insertOrUpdateVaultState(
                currentState.copy(pin = newPin, isPinEnabled = enablePin)
            )
        }
    }

    fun toggleHideVaultBalance() {
        viewModelScope.launch {
            val currentState = vaultState.value
            repository.insertOrUpdateVaultState(
                currentState.copy(isBalanceHidden = !currentState.isBalanceHidden)
            )
        }
    }

    // CREATE GOAL
    fun createGoal(
        name: String,
        icon: String,
        coverImage: String,
        targetAmount: Double,
        targetDate: Long,
        priority: String,
        notes: String,
        category: String
    ) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                name = name,
                icon = icon,
                coverImage = coverImage,
                targetAmount = targetAmount,
                targetDate = targetDate,
                priority = priority,
                notes = notes,
                category = category,
                currentSavedAmount = 0.0,
                status = "Active",
                createdDate = System.currentTimeMillis()
            )
            repository.insertGoal(goal)
        }
    }

    // UPDATE GOAL STATUS
    fun updateGoalStatus(goal: SavingsGoal, newStatus: String) {
        viewModelScope.launch {
            var completionDate: Long? = goal.completionDate
            if (newStatus == "Completed" && goal.status != "Completed") {
                completionDate = System.currentTimeMillis()
            } else if (newStatus != "Completed") {
                completionDate = null
            }
            repository.updateGoal(goal.copy(status = newStatus, completionDate = completionDate))
        }
    }

    // DELETE GOAL
    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // DEPOSIT TO GOAL
    // source options: "Manual", "Bank Account", "Cash", "Private Vault", "Income"
    fun depositToGoal(
        goal: SavingsGoal,
        amount: Double,
        source: String,
        bankAccountId: Int?,
        notes: String
    ) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch

            // 1. Update goal savings
            val newSaved = goal.currentSavedAmount + amount
            val completed = newSaved >= goal.targetAmount
            val status = if (completed) "Completed" else goal.status
            val completionDate = if (completed) System.currentTimeMillis() else goal.completionDate

            repository.updateGoal(
                goal.copy(
                    currentSavedAmount = newSaved,
                    status = status,
                    completionDate = completionDate
                )
            )

            // 2. Adjust source balances & log history
            var sourceName = source
            if (source == "Private Vault") {
                val state = vaultState.value
                val newVaultBalance = (state.balance - amount).coerceAtLeast(0.0)
                repository.insertOrUpdateVaultState(state.copy(balance = newVaultBalance))

                repository.insertVaultHistoryItem(
                    VaultHistoryItem(
                        amount = amount,
                        type = "Transfer to Goal",
                        source = "Private Vault",
                        destination = goal.name,
                        notes = "Transferred from Secure Vault to Goal: ${goal.name}. $notes"
                    )
                )
                sourceName = "Private Vault"
            } else if (source == "Bank Account" && bankAccountId != null) {
                val account = bankAccounts.value.find { it.id == bankAccountId }
                if (account != null) {
                    val newAccBalance = account.balance - amount
                    repository.updateAccount(account.copy(balance = newAccBalance))
                    sourceName = account.nickname
                }
            }

            // 3. Create normal transaction automatically
            val category = categories.value.find { it.name == goal.category }
            val tx = Transaction(
                amount = amount,
                type = "Expense", // Classified as expense/saving outflow from main balance
                categoryName = goal.category,
                categoryId = category?.id,
                merchantName = "Savings Goal: ${goal.name}",
                paymentMethod = if (source == "Private Vault") "Vault Transfer" else if (source == "Bank Account") "Bank Transfer" else source,
                bankAccountId = bankAccountId ?: 0,
                bankAccountName = sourceName,
                timestamp = System.currentTimeMillis(),
                notes = "Deposit of ₹$amount to goal ${goal.name}. $notes",
                tags = "#SavingsGoal,#${goal.name.replace(" ", "")}"
            )
            repository.insertTransaction(tx)
        }
    }

    // WITHDRAW FROM GOAL
    // destination options: "Bank Account", "Cash", "Private Vault"
    fun withdrawFromGoal(
        goal: SavingsGoal,
        amount: Double,
        destination: String,
        bankAccountId: Int?,
        reason: String
    ) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch

            // 1. Update Goal Amount
            val newSaved = (goal.currentSavedAmount - amount).coerceAtLeast(0.0)
            repository.updateGoal(
                goal.copy(
                    currentSavedAmount = newSaved,
                    status = if (newSaved < goal.targetAmount && goal.status == "Completed") "Active" else goal.status,
                    completionDate = if (newSaved < goal.targetAmount) null else goal.completionDate
                )
            )

            // 2. Adjust Destination
            var destName = destination
            if (destination == "Private Vault") {
                val state = vaultState.value
                val newVaultBalance = state.balance + amount
                repository.insertOrUpdateVaultState(state.copy(balance = newVaultBalance))

                repository.insertVaultHistoryItem(
                    VaultHistoryItem(
                        amount = amount,
                        type = "Transfer from Goal",
                        source = goal.name,
                        destination = "Private Vault",
                        notes = "Withdrawn from Goal ${goal.name} to Secure Vault. Reason: $reason"
                    )
                )
                destName = "Private Vault"
            } else if (destination == "Bank Account" && bankAccountId != null) {
                val account = bankAccounts.value.find { it.id == bankAccountId }
                if (account != null) {
                    val newAccBalance = account.balance + amount
                    repository.updateAccount(account.copy(balance = newAccBalance))
                    destName = account.nickname
                }
            }

            // 3. Create refund/income transaction representing money returning to main pool
            val category = categories.value.find { it.name == goal.category }
            val tx = Transaction(
                amount = amount,
                type = "Income", // money coming back to active account
                categoryName = goal.category,
                categoryId = category?.id,
                merchantName = "Withdrawal: ${goal.name}",
                paymentMethod = if (destination == "Private Vault") "Vault Transfer" else if (destination == "Bank Account") "Bank Transfer" else destination,
                bankAccountId = bankAccountId ?: 0,
                bankAccountName = destName,
                timestamp = System.currentTimeMillis(),
                notes = "Goal Withdrawal of ₹$amount. Reason: $reason",
                tags = "#GoalWithdrawal,#${goal.name.replace(" ", "")}"
            )
            repository.insertTransaction(tx)
        }
    }

    // TRANSFER BETWEEN GOALS
    fun transferBetweenGoals(
        sourceGoal: SavingsGoal,
        destGoal: SavingsGoal,
        amount: Double,
        notes: String
    ) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch

            // Deduct from source
            val newSourceSaved = (sourceGoal.currentSavedAmount - amount).coerceAtLeast(0.0)
            repository.updateGoal(
                sourceGoal.copy(
                    currentSavedAmount = newSourceSaved,
                    status = if (newSourceSaved < sourceGoal.targetAmount && sourceGoal.status == "Completed") "Active" else sourceGoal.status,
                    completionDate = if (newSourceSaved < sourceGoal.targetAmount) null else sourceGoal.completionDate
                )
            )

            // Add to destination
            val newDestSaved = destGoal.currentSavedAmount + amount
            val completed = newDestSaved >= destGoal.targetAmount
            val status = if (completed) "Completed" else destGoal.status
            val completionDate = if (completed) System.currentTimeMillis() else destGoal.completionDate

            repository.updateGoal(
                destGoal.copy(
                    currentSavedAmount = newDestSaved,
                    status = status,
                    completionDate = completionDate
                )
            )

            // Log vault history for traceability
            repository.insertVaultHistoryItem(
                VaultHistoryItem(
                    amount = amount,
                    type = "Transfer Between Goals",
                    source = sourceGoal.name,
                    destination = destGoal.name,
                    notes = "Transferred ₹$amount from '${sourceGoal.name}' to '${destGoal.name}'. Notes: $notes"
                )
            )
        }
    }

    // PRIVATE VAULT DIRECT DEPOSIT
    fun depositToVault(amount: Double, source: String, bankAccountId: Int?, notes: String) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch

            // Update state
            val state = vaultState.value
            repository.insertOrUpdateVaultState(state.copy(balance = state.balance + amount))

            // Log history
            var sourceName = source
            if (source == "Bank Account" && bankAccountId != null) {
                val account = bankAccounts.value.find { it.id == bankAccountId }
                if (account != null) {
                    repository.updateAccount(account.copy(balance = account.balance - amount))
                    sourceName = account.nickname
                }
            }

            repository.insertVaultHistoryItem(
                VaultHistoryItem(
                    amount = amount,
                    type = "Deposit",
                    source = sourceName,
                    destination = "Private Vault",
                    notes = "Deposited ₹$amount into Secure Vault. Notes: $notes"
                )
            )

            // Register Transaction
            val tx = Transaction(
                amount = amount,
                type = "Expense", // standard outflow from standard banks to private vault
                categoryName = "Investment",
                merchantName = "Secure Vault Deposit",
                paymentMethod = if (source == "Bank Account") "Bank Transfer" else source,
                bankAccountId = bankAccountId ?: 0,
                bankAccountName = sourceName,
                timestamp = System.currentTimeMillis(),
                notes = "Deposit to Private Vault: $notes",
                tags = "#SecureVault,#Savings"
            )
            repository.insertTransaction(tx)
        }
    }

    // PRIVATE VAULT DIRECT WITHDRAW
    fun withdrawFromVault(amount: Double, destination: String, bankAccountId: Int?, notes: String) {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch

            // Update state
            val state = vaultState.value
            val newBalance = (state.balance - amount).coerceAtLeast(0.0)
            repository.insertOrUpdateVaultState(state.copy(balance = newBalance))

            // Log history
            var destName = destination
            if (destination == "Bank Account" && bankAccountId != null) {
                val account = bankAccounts.value.find { it.id == bankAccountId }
                if (account != null) {
                    repository.updateAccount(account.copy(balance = account.balance + amount))
                    destName = account.nickname
                }
            }

            repository.insertVaultHistoryItem(
                VaultHistoryItem(
                    amount = amount,
                    type = "Withdraw",
                    source = "Private Vault",
                    destination = destName,
                    notes = "Withdrew ₹$amount from Secure Vault. Notes: $notes"
                )
            )

            // Register Transaction
            val tx = Transaction(
                amount = amount,
                type = "Income", // inflow returning to checking accounts
                categoryName = "Investment Refund",
                merchantName = "Secure Vault Withdrawal",
                paymentMethod = if (destination == "Bank Account") "Bank Transfer" else destination,
                bankAccountId = bankAccountId ?: 0,
                bankAccountName = destName,
                timestamp = System.currentTimeMillis(),
                notes = "Withdrawal from Private Vault: $notes",
                tags = "#SecureVault,#VaultWithdrawal"
            )
            repository.insertTransaction(tx)
        }
    }

    // AI CALCULATION ENGINE
    fun calculatePlannerInfo(targetAmount: Double, currentSavedAmount: Double, targetDate: Long): PlannerInfo {
        val remaining = (targetAmount - currentSavedAmount).coerceAtLeast(0.0)
        val msLeft = targetDate - System.currentTimeMillis()
        val daysLeft = (msLeft / (24 * 60 * 60 * 1000)).coerceAtLeast(1)

        val daily = remaining / daysLeft
        val weekly = daily * 7
        val monthly = daily * 30

        val suggestion = if (remaining <= 0) {
            "Goal completed! Excellent financial discipline!"
        } else if (daily < 50.0) {
            "Save ₹${daily.toInt() + 1} per day by skipping a tea or coffee."
        } else if (daily < 250.0) {
            "Save ₹${weekly.toInt() + 1} every weekend to hit your target effortlessly."
        } else {
            "Save ₹${monthly.toInt() + 1} per month. Consider setting up a auto-transfer."
        }

        return PlannerInfo(
            daysLeft = daysLeft.toInt(),
            dailyTarget = daily,
            weeklyTarget = weekly,
            monthlyTarget = monthly,
            aiSuggestion = suggestion
        )
    }
}

data class PlannerInfo(
    val daysLeft: Int,
    val dailyTarget: Double,
    val weeklyTarget: Double,
    val monthlyTarget: Double,
    val aiSuggestion: String
)
