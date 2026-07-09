package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.BankAccount
import com.example.domain.model.BalanceAdjustment
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Transaction
import com.example.domain.model.UserProfile
import com.example.domain.repository.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: VaultRepository) : ViewModel() {

    // User Profile Flow
    val userProfile: StateFlow<UserProfile?> = repository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Bank Accounts Flow
    val bankAccounts: StateFlow<List<BankAccount>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Payment Methods Flow
    val paymentMethods: StateFlow<List<PaymentMethod>> = repository.getAllPaymentMethods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Balance Adjustments Flow
    val balanceAdjustments: StateFlow<List<BalanceAdjustment>> = repository.getAllAdjustments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        seedInitialProfileAndPaymentMethods()
    }

    private fun seedInitialProfileAndPaymentMethods() {
        viewModelScope.launch {
            // Seed a default user profile if empty
            val profile = repository.getUserProfile().first()
            if (profile == null) {
                repository.insertOrUpdateProfile(UserProfile())
            }

            // Seed default payment methods if empty
            val methods = repository.getAllPaymentMethods().first()
            if (methods.isEmpty()) {
                val defaults = listOf(
                    PaymentMethod(type = "Cash", nickname = "Pocket Cash", colorHex = "#10B981", iconName = "payments", isFavorite = true),
                    PaymentMethod(type = "Google Pay", nickname = "GPay Primary", colorHex = "#3B82F6", iconName = "qr_code", upiId = "sathwik@oksbi", isFavorite = true),
                    PaymentMethod(type = "PhonePe", nickname = "PhonePe Sathwik", colorHex = "#8B5CF6", iconName = "qr_code", upiId = "sathwik@ybl", isFavorite = false),
                    PaymentMethod(type = "Paytm", nickname = "Paytm Wallet", colorHex = "#0EA5E9", iconName = "wallet", isFavorite = false),
                    PaymentMethod(type = "Credit Card", nickname = "SBI Card Elite", colorHex = "#F59E0B", iconName = "credit_card", isFavorite = true)
                )
                defaults.forEach { repository.insertPaymentMethod(it) }
            }
        }
    }

    // User Profile Actions
    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.insertOrUpdateProfile(profile)
        }
    }

    // Bank Account CRUD
    fun addBankAccount(account: BankAccount) {
        viewModelScope.launch {
            repository.insertAccount(account)
        }
    }

    fun updateBankAccount(account: BankAccount) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteBankAccount(account: BankAccount) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    // Payment Method CRUD
    fun addPaymentMethod(method: PaymentMethod) {
        viewModelScope.launch {
            repository.insertPaymentMethod(method)
        }
    }

    fun updatePaymentMethod(method: PaymentMethod) {
        viewModelScope.launch {
            repository.updatePaymentMethod(method)
        }
    }

    fun deletePaymentMethod(method: PaymentMethod) {
        viewModelScope.launch {
            repository.deletePaymentMethod(method)
        }
    }

    fun togglePaymentMethodFavorite(method: PaymentMethod) {
        viewModelScope.launch {
            repository.updatePaymentMethod(method.copy(isFavorite = !method.isFavorite))
        }
    }

    // Balance Adjustments & Transfers (Section 5)
    fun adjustBalance(
        type: String, // "Increase", "Decrease", "Correction", "Transfer"
        accountId: Int,
        amount: Double,
        reason: String,
        notes: String = "",
        targetAccountId: Int? = null
    ) {
        viewModelScope.launch {
            val sourceAccount = repository.getAccountById(accountId) ?: return@launch
            
            if (type == "Transfer") {
                val targetId = targetAccountId ?: return@launch
                val targetAccount = repository.getAccountById(targetId) ?: return@launch

                // Update accounts balances
                val updatedSource = sourceAccount.copy(balance = sourceAccount.balance - amount)
                val updatedTarget = targetAccount.copy(balance = targetAccount.balance + amount)
                repository.updateAccount(updatedSource)
                repository.updateAccount(updatedTarget)

                // Insert balance adjustment history log
                val adjustmentId = repository.insertAdjustment(
                    BalanceAdjustment(
                        type = "Transfer",
                        fromAccountId = accountId,
                        fromAccountName = sourceAccount.nickname,
                        toAccountId = targetId,
                        toAccountName = targetAccount.nickname,
                        amount = amount,
                        reason = reason,
                        notes = notes,
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Create and insert standard Transaction
                val transferTransaction = Transaction(
                    amount = amount,
                    type = "Transfer",
                    categoryName = "Transfer",
                    merchantName = targetAccount.nickname, // Transfer destination
                    paymentMethod = "Bank Transfer",
                    bankAccountId = accountId,
                    bankAccountName = sourceAccount.nickname,
                    timestamp = System.currentTimeMillis(),
                    notes = "Transfer adjustment: $reason. $notes",
                    tags = "#transfer"
                )
                repository.insertTransaction(transferTransaction)

            } else {
                // Adjustment (Increase, Decrease, Correct)
                val oldBalance = sourceAccount.balance
                val newBalance = when (type) {
                    "Increase" -> oldBalance + amount
                    "Decrease" -> oldBalance - amount
                    "Correction" -> amount // Exact override
                    else -> oldBalance
                }
                
                // Update source account
                repository.updateAccount(sourceAccount.copy(balance = newBalance))

                // Log the Adjustment
                repository.insertAdjustment(
                    BalanceAdjustment(
                        type = type,
                        fromAccountId = accountId,
                        fromAccountName = sourceAccount.nickname,
                        amount = amount,
                        reason = reason,
                        notes = notes,
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Log standard Transaction for balance integrity
                val delta = if (type == "Correction") (newBalance - oldBalance) else amount
                val transactionType = if (type == "Increase" || (type == "Correction" && delta >= 0)) "Income" else "Expense"
                val finalAmount = if (delta < 0) -delta else delta

                if (finalAmount > 0.01) {
                    val adjTransaction = Transaction(
                        amount = finalAmount,
                        type = transactionType,
                        categoryName = "Adjustment",
                        merchantName = "Balance correction: $reason",
                        paymentMethod = "Adjustment",
                        bankAccountId = accountId,
                        bankAccountName = sourceAccount.nickname,
                        timestamp = System.currentTimeMillis(),
                        notes = notes,
                        tags = "#adjustment"
                    )
                    repository.insertTransaction(adjTransaction)
                }
            }
        }
    }
}
