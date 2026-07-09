package com.example.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashDestination

@Serializable
object OnboardingDestination

@Serializable
object HomeDestination

@Serializable
object TransactionDestination

@Serializable
object BorrowDestination

@Serializable
object OcrImportDestination

@Serializable
object SavingsGoalsDestination

@Serializable
object VaultDestination

@Serializable
object AnalyticsDestination

@Serializable
object ReportsDestination

@Serializable
object ProfileDestination

@Serializable
object SettingsDestination

@Serializable
object RecurringPaymentsDestination

@Serializable
object NotificationsDestination

@Serializable
object SearchDestination

@Serializable
object CategoryManagementDestination

@Serializable
data class CategoryDetailsDestination(val categoryId: Int)

@Serializable
object MerchantDatabaseDestination

@Serializable
data class MerchantDetailsDestination(val merchantId: Int)

