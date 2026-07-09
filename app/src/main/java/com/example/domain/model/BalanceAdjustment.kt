package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balance_adjustments")
data class BalanceAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Increase", "Decrease", "Correction", "Transfer"
    val fromAccountId: Int? = null,
    val fromAccountName: String = "",
    val toAccountId: Int? = null,
    val toAccountName: String = "",
    val amount: Double,
    val reason: String,
    val notes: String = "",
    val timestamp: Long
)
