package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Only one single record
    val name: String = "Sathwik",
    val nickname: String = "Sathwik",
    val email: String = "dubbakasathwik@gmail.com",
    val mobile: String = "+91 9876543210",
    val profilePictureUri: String = "", // Local image URI
    val currency: String = "INR (₹)",
    val timeFormat: String = "12 Hour", // "12 Hour" or "24 Hour"
    val dateFormat: String = "dd MMM yyyy",
    val country: String = "India",
    val language: String = "English",
    val financialYearStartMonth: String = "April",
    val firstDayOfWeek: String = "Monday"
)
