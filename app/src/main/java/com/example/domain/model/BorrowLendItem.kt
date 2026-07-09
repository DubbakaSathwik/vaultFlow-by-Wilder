package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "borrow_lend_items")
data class BorrowLendItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Borrowed" or "Lended"
    val personName: String,
    val profilePhoto: String? = null,
    val mobileNumber: String? = null,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis(),
    val time: String = "",
    val bank: String? = null,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val receipt: String? = null,
    val transactionLink: String? = null,
    val status: String = "Pending" // "Pending", "Partially Paid", "Completed", "Cancelled"
) {
    val remainingAmount: Double
        get() = amount - paidAmount
}
