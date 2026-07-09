package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "merchants")
@Serializable
data class Merchant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchantName: String, // raw scanned or inputted name (e.g., "SHIVA STORE")
    val alias: String = "", // custom alias input (e.g., "XYZ Super Market")
    val storeName: String = "", // formatted name (alias if present, else merchantName)
    val category: String = "",
    val paymentMethodsUsed: String = "", // Comma-separated payment methods used
    val banksUsed: String = "", // Comma-separated banks used
    val tags: String = "", // Comma-separated tags
    val address: String = "",
    val website: String = "",
    val phone: String = "",
    val notes: String = "",
    val visitCount: Int = 0,
    val totalSpending: Double = 0.0,
    val averageSpending: Double = 0.0,
    val firstTransaction: Long = 0L,
    val lastTransaction: Long = 0L,
    val isFavorite: Boolean = false
)
