package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String, // String resource identifier or symbol name
    val coverImage: String = "", // Base64 or local URI or preset identifier
    val targetAmount: Double,
    val currentSavedAmount: Double = 0.0,
    val targetDate: Long, // timestamp
    val priority: String, // "Low", "Medium", "High"
    val notes: String = "",
    val category: String = "General", // Associated category name
    val status: String = "Active", // "Active", "Completed", "Paused", "Cancelled"
    val createdDate: Long = System.currentTimeMillis(),
    val completionDate: Long? = null
)
