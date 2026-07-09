package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "Expense", "Income", "Transfer", "Adjustment", "Refund", "Borrow", "Lend"
    val categoryName: String,
    val categoryId: Int? = null,
    val merchantName: String,
    val merchantAlias: String = "",
    val paymentMethod: String, // "Cash", "PhonePe", "Google Pay", "Paytm", "FamPay", "Debit Card", "Credit Card", "Bank Transfer", "UPI"
    val bankAccountId: Int,
    val bankAccountName: String,
    val timestamp: Long, // App Time in Milliseconds
    val notes: String = "",
    val tags: String = "", // Comma-separated like "#College,#Trip"
    val location: String? = null,
    val receiptPath: String? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null
) {
    val tagList: List<String>
        get() = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }
}
