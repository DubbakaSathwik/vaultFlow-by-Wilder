package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.database.VaultDatabase
import com.example.data.preferences.SettingsManager
import com.example.data.repository.VaultRepositoryImpl
import com.example.domain.repository.VaultRepository

interface AppContainer {
    val settingsManager: SettingsManager
    val vaultRepository: VaultRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {
    
    private val database: VaultDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            VaultDatabase::class.java,
            "vaultflow_database"
        ).fallbackToDestructiveMigration().build()
    }
    
    override val settingsManager: SettingsManager by lazy {
        SettingsManager(context)
    }
    
    override val vaultRepository: VaultRepository by lazy {
        VaultRepositoryImpl(
            database.vaultDao(),
            database.transactionDao(),
            database.bankAccountDao(),
            database.categoryDao(),
            database.userProfileDao(),
            database.paymentMethodDao(),
            database.balanceAdjustmentDao(),
            database.recurringItemDao(),
            database.ocrDao(),
            database.merchantDao(),
            database.savingsGoalDao(),
            database.privateVaultDao()
        )
    }
}
