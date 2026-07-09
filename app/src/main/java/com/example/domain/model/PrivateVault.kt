package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "private_vault_state")
data class PrivateVaultState(
    @PrimaryKey val id: Int = 1, // Singleton row
    val balance: Double = 0.0,
    val pin: String = "", // empty means not set or disabled
    val isBalanceHidden: Boolean = false,
    val isPinEnabled: Boolean = false
)

@Entity(tableName = "vault_history")
data class VaultHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "Deposit", "Withdraw", "Transfer to Goal", "Transfer to Bank", "Transfer Between Goals"
    val source: String, // "Cash", "Bank Account Name", "Vault", "Goal Name"
    val destination: String, // "Vault", "Bank Account Name", "Goal Name"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
