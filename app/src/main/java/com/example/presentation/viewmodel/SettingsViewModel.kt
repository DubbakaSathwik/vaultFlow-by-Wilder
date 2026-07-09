package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    val themeMode: StateFlow<String> = settingsManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val useDynamicColor: StateFlow<Boolean> = settingsManager.useDynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Section 6 Flows
    val accentColor: StateFlow<String> = settingsManager.accentColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Default")
    val fontSize: StateFlow<String> = settingsManager.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Medium")
    val animationSpeed: StateFlow<String> = settingsManager.animationSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Standard")

    // Section 7 Flows
    val useDeviceTime: StateFlow<Boolean> = settingsManager.useDeviceTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val vaultFlowTimeOffset: StateFlow<Long> = settingsManager.vaultFlowTimeOffsetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val vaultFlowTimezone: StateFlow<String> = settingsManager.vaultFlowTimezoneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "UTC")

    // Section 8 Flows
    val pinEnabled: StateFlow<Boolean> = settingsManager.pinEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val pinCode: StateFlow<String> = settingsManager.pinCodeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val biometricsEnabled: StateFlow<Boolean> = settingsManager.biometricsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hideBalances: StateFlow<Boolean> = settingsManager.hideBalancesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hideVault: StateFlow<Boolean> = settingsManager.hideVaultFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val hideRecents: StateFlow<Boolean> = settingsManager.hideRecentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoLockTiming: StateFlow<String> = settingsManager.autoLockTimingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Never")

    // Section 9 Flows
    val reminderDaily: StateFlow<Boolean> = settingsManager.reminderDailyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderExpense: StateFlow<Boolean> = settingsManager.reminderExpenseFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderIncome: StateFlow<Boolean> = settingsManager.reminderIncomeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderGoal: StateFlow<Boolean> = settingsManager.reminderGoalFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderBorrow: StateFlow<Boolean> = settingsManager.reminderBorrowFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderLend: StateFlow<Boolean> = settingsManager.reminderLendFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderSubscription: StateFlow<Boolean> = settingsManager.reminderSubscriptionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderPocketMoney: StateFlow<Boolean> = settingsManager.reminderPocketMoneyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val reminderEmi: StateFlow<Boolean> = settingsManager.reminderEmiFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Setters
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsManager.setThemeMode(mode)
        }
    }

    fun setUseDynamicColor(useDynamic: Boolean) {
        viewModelScope.launch {
            settingsManager.setUseDynamicColor(useDynamic)
        }
    }
    
    fun resetOnboarding() {
        viewModelScope.launch {
            settingsManager.setOnboarded(false)
        }
    }

    // Section 6 Setters
    fun setAccentColor(color: String) { viewModelScope.launch { settingsManager.setAccentColor(color) } }
    fun setFontSize(size: String) { viewModelScope.launch { settingsManager.setFontSize(size) } }
    fun setAnimationSpeed(speed: String) { viewModelScope.launch { settingsManager.setAnimationSpeed(speed) } }

    // Section 7 Setters
    fun setUseDeviceTime(useDevice: Boolean) { viewModelScope.launch { settingsManager.setUseDeviceTime(useDevice) } }
    fun setVaultFlowTimeOffset(offset: Long) { viewModelScope.launch { settingsManager.setVaultFlowTimeOffset(offset) } }
    fun setVaultFlowTimezone(timezone: String) { viewModelScope.launch { settingsManager.setVaultFlowTimezone(timezone) } }

    // Section 8 Setters
    fun setPinEnabled(enabled: Boolean) { viewModelScope.launch { settingsManager.setPinEnabled(enabled) } }
    fun setPinCode(pin: String) { viewModelScope.launch { settingsManager.setPinCode(pin) } }
    fun setBiometricsEnabled(enabled: Boolean) { viewModelScope.launch { settingsManager.setBiometricsEnabled(enabled) } }
    fun setHideBalances(hide: Boolean) { viewModelScope.launch { settingsManager.setHideBalances(hide) } }
    fun setHideVault(hide: Boolean) { viewModelScope.launch { settingsManager.setHideVault(hide) } }
    fun setHideRecents(hide: Boolean) { viewModelScope.launch { settingsManager.setHideRecents(hide) } }
    fun setAutoLockTiming(timing: String) { viewModelScope.launch { settingsManager.setAutoLockTiming(timing) } }

    // Section 9 Setters
    fun setReminderDaily(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderDaily(enabled) } }
    fun setReminderExpense(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderExpense(enabled) } }
    fun setReminderIncome(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderIncome(enabled) } }
    fun setReminderGoal(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderGoal(enabled) } }
    fun setReminderBorrow(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderBorrow(enabled) } }
    fun setReminderLend(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderLend(enabled) } }
    fun setReminderSubscription(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderSubscription(enabled) } }
    fun setReminderPocketMoney(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderPocketMoney(enabled) } }
    fun setReminderEmi(enabled: Boolean) { viewModelScope.launch { settingsManager.setReminderEmi(enabled) } }
}
