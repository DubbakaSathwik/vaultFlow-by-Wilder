package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedReceipt(
    val amount: Double?,
    val merchantName: String,
    val dateStr: String,
    val timeStr: String,
    val paymentApp: String,
    val bankName: String,
    val last4Digits: String,
    val upiId: String,
    val transactionId: String,
    val referenceNumber: String,
    val accountHolder: String,
    val paymentStatus: String,
    val rawText: String,
    val confidence: Double
)
