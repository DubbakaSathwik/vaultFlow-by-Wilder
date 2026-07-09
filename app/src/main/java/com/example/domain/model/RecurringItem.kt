package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_items")
data class RecurringItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Income" or "Expense"
    val name: String, // e.g. "Pocket Money", "Father", "Netflix", "Spotify"
    val amount: Double,
    val frequency: String, // "Daily", "Weekly", "Monthly", "Yearly"
    val bankAccountId: Int,
    val bankAccountName: String = "",
    val paymentMethod: String = "UPI", // Only for expenses typically
    val categoryId: Int,
    val categoryName: String,
    val notes: String = "",
    val isAutoAdd: Boolean = true, // Auto add without asking
    val askBeforeAdd: Boolean = false, // Ask before adding
    val hasReminder: Boolean = true,
    val lastTriggeredTimestamp: Long? = null,
    val nextTriggeredTimestamp: Long? = null,
    val isActive: Boolean = true
)
