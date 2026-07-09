package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "categories")
@Serializable
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val description: String = "",
    val monthlyBudget: Double = 0.0,
    val weeklyBudget: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val notificationThreshold: Double = 0.85, // 85% of budget as alert threshold
    val defaultTransactionType: String = "Expense", // "Expense", "Income", "Both"
    val displayOrder: Int = 0,
    val status: String = "Active", // "Active", "Archived", "Deleted"
    val usageCount: Int = 0,
    val createdDate: Long = System.currentTimeMillis(),
    val lastUsed: Long = 0L
)

