package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_methods")
data class PaymentMethod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Cash", "PhonePe", "Google Pay", "Paytm", "FamPay", "Amazon Pay", "Credit Card", "Debit Card", "UPI", "Bank Transfer", "Wallet", "Custom"
    val nickname: String,
    val colorHex: String,
    val linkedBankId: Int? = null,
    val linkedBankName: String = "",
    val status: String = "Active", // "Active" or "Inactive"
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val iconName: String = "credit_card",
    
    // QR Code Integration (Section 4)
    val qrImageUri: String = "", // URI to locally stored or selected QR image
    val upiId: String = ""
)
