package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "ai_memories")
@Serializable
data class AiMemory(
    @PrimaryKey val merchantName: String,
    val preferredCategory: String = "",
    val preferredBank: String = "",
    val preferredPaymentMethod: String = "",
    val preferredTags: String = "",
    val preferredNotes: String = "",
    val preferredTransactionType: String = "Expense",
    val learningCount: Int = 1,
    val confidence: Double = 1.0
)
