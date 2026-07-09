package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,
    val nickname: String,
    val last4Digits: String,
    val balance: Double,
    val colorHex: String, // e.g. "#0284C7"
    val logoName: String = "bank", // Logo placeholder icon string
    val accountHolder: String = "Default Holder",
    val openingBalance: Double = 0.0,
    val upiId: String = "",
    val accountType: String = "Savings", // "Savings", "Current", "Wallet"
    val notes: String = "",
    val status: String = "Active", // "Active", "Inactive", "Default"
    val isArchived: Boolean = false
)
