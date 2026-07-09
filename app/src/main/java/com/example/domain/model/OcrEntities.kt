package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "merchant_aliases")
@Serializable
data class MerchantAlias(
    @PrimaryKey val originalMerchant: String,
    val aliasMerchant: String
)

@Entity(tableName = "merchant_corrections")
@Serializable
data class MerchantCorrection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalMerchant: String,
    val correctedMerchant: String,
    val count: Int
)

@Entity(tableName = "category_learnings")
@Serializable
data class CategoryLearning(
    @PrimaryKey val merchantName: String,
    val categoryName: String
)

@Entity(tableName = "payment_learnings")
@Serializable
data class PaymentLearning(
    @PrimaryKey val merchantName: String,
    val paymentMethod: String
)

@Entity(tableName = "bank_learnings")
@Serializable
data class BankLearning(
    @PrimaryKey val merchantName: String,
    val bankAccountName: String,
    val bankAccountId: Int
)

@Entity(tableName = "ocr_histories")
@Serializable
data class OcrHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val importDate: Long = System.currentTimeMillis(),
    val confidence: Double,
    val receiptName: String,
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
    val linkedTransactionId: Int? = null,
    val receiptLocalPath: String? = null
)
