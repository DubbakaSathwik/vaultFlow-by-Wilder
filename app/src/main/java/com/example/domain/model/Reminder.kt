package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val priority: String = "Medium", // "Low", "Medium", "High"
    val reminderDate: Long = System.currentTimeMillis(),
    val reminderTime: String = "",
    val repeat: String = "None", // "None", "Daily", "Weekly", "Monthly"
    val completed: Boolean = false,
    val dismissed: Boolean = false,
    val notification: Boolean = true,
    val type: String = "Custom Reminder" // "Borrow Due", "Lend Due", "Goal Saving Reminder", "Pocket Money Reminder", "Recurring Income", "Recurring Expense", "EMI", "Subscription", "Custom Reminder"
)
