package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vaultflow_preferences")

class SettingsManager(private val context: Context) {

    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR_KEY = booleanPreferencesKey("use_dynamic_color")
        val IS_ONBOARDED_KEY = booleanPreferencesKey("is_onboarded")
        
        // Appearance (Section 6)
        val ACCENT_COLOR_KEY = stringPreferencesKey("accent_color")
        val FONT_SIZE_KEY = stringPreferencesKey("font_size")
        val ANIMATION_SPEED_KEY = stringPreferencesKey("animation_speed")
        
        // Date & Time override (Section 7)
        val USE_DEVICE_TIME_KEY = booleanPreferencesKey("use_device_time")
        val VAULTFLOW_TIME_OFFSET_KEY = longPreferencesKey("vaultflow_time_offset")
        val VAULTFLOW_TIMEZONE_KEY = stringPreferencesKey("vaultflow_timezone")
        
        // Security (Section 8)
        val PIN_ENABLED_KEY = booleanPreferencesKey("pin_enabled")
        val PIN_CODE_KEY = stringPreferencesKey("pin_code")
        val BIOMETRICS_ENABLED_KEY = booleanPreferencesKey("biometrics_enabled")
        val HIDE_BALANCES_KEY = booleanPreferencesKey("hide_balances")
        val HIDE_VAULT_KEY = booleanPreferencesKey("hide_vault")
        val HIDE_RECENTS_KEY = booleanPreferencesKey("hide_recents")
        val AUTO_LOCK_TIMING_KEY = stringPreferencesKey("auto_lock_timing")
        
        // Notifications Reminders (Section 9)
        val REMINDER_DAILY_KEY = booleanPreferencesKey("reminder_daily")
        val REMINDER_EXPENSE_KEY = booleanPreferencesKey("reminder_expense")
        val REMINDER_INCOME_KEY = booleanPreferencesKey("reminder_income")
        val REMINDER_GOAL_KEY = booleanPreferencesKey("reminder_goal")
        val REMINDER_BORROW_KEY = booleanPreferencesKey("reminder_borrow")
        val REMINDER_LEND_KEY = booleanPreferencesKey("reminder_lend")
        val REMINDER_SUBSCRIPTION_KEY = booleanPreferencesKey("reminder_subscription")
        val REMINDER_POCKET_MONEY_KEY = booleanPreferencesKey("reminder_pocket_money")
        val REMINDER_EMI_KEY = booleanPreferencesKey("reminder_emi")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY] ?: "System"
    }

    val useDynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_DYNAMIC_COLOR_KEY] ?: false
    }

    val isOnboardedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDED_KEY] ?: false
    }

    // Section 6 Flows
    val accentColorFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACCENT_COLOR_KEY] ?: "Default"
    }
    val fontSizeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FONT_SIZE_KEY] ?: "Medium"
    }
    val animationSpeedFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ANIMATION_SPEED_KEY] ?: "Standard"
    }

    // Section 7 Flows
    val useDeviceTimeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USE_DEVICE_TIME_KEY] ?: true
    }
    val vaultFlowTimeOffsetFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[VAULTFLOW_TIME_OFFSET_KEY] ?: 0L
    }
    val vaultFlowTimezoneFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[VAULTFLOW_TIMEZONE_KEY] ?: "UTC"
    }

    // Section 8 Flows
    val pinEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PIN_ENABLED_KEY] ?: false
    }
    val pinCodeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PIN_CODE_KEY] ?: ""
    }
    val biometricsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRICS_ENABLED_KEY] ?: false
    }
    val hideBalancesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_BALANCES_KEY] ?: false
    }
    val hideVaultFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_VAULT_KEY] ?: false
    }
    val hideRecentsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HIDE_RECENTS_KEY] ?: false
    }
    val autoLockTimingFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AUTO_LOCK_TIMING_KEY] ?: "Never"
    }

    // Section 9 Flows
    val reminderDailyFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_DAILY_KEY] ?: true }
    val reminderExpenseFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_EXPENSE_KEY] ?: true }
    val reminderIncomeFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_INCOME_KEY] ?: true }
    val reminderGoalFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_GOAL_KEY] ?: true }
    val reminderBorrowFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_BORROW_KEY] ?: true }
    val reminderLendFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_LEND_KEY] ?: true }
    val reminderSubscriptionFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_SUBSCRIPTION_KEY] ?: true }
    val reminderPocketMoneyFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_POCKET_MONEY_KEY] ?: true }
    val reminderEmiFlow: Flow<Boolean> = context.dataStore.data.map { preferences -> preferences[REMINDER_EMI_KEY] ?: true }

    // Setters
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode }
    }
    suspend fun setUseDynamicColor(useDynamic: Boolean) {
        context.dataStore.edit { preferences -> preferences[USE_DYNAMIC_COLOR_KEY] = useDynamic }
    }
    suspend fun setOnboarded(onboarded: Boolean) {
        context.dataStore.edit { preferences -> preferences[IS_ONBOARDED_KEY] = onboarded }
    }

    // Section 6 Setters
    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { preferences -> preferences[ACCENT_COLOR_KEY] = color }
    }
    suspend fun setFontSize(size: String) {
        context.dataStore.edit { preferences -> preferences[FONT_SIZE_KEY] = size }
    }
    suspend fun setAnimationSpeed(speed: String) {
        context.dataStore.edit { preferences -> preferences[ANIMATION_SPEED_KEY] = speed }
    }

    // Section 7 Setters
    suspend fun setUseDeviceTime(useDevice: Boolean) {
        context.dataStore.edit { preferences -> preferences[USE_DEVICE_TIME_KEY] = useDevice }
    }
    suspend fun setVaultFlowTimeOffset(offset: Long) {
        context.dataStore.edit { preferences -> preferences[VAULTFLOW_TIME_OFFSET_KEY] = offset }
    }
    suspend fun setVaultFlowTimezone(timezone: String) {
        context.dataStore.edit { preferences -> preferences[VAULTFLOW_TIMEZONE_KEY] = timezone }
    }

    // Section 8 Setters
    suspend fun setPinEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PIN_ENABLED_KEY] = enabled }
    }
    suspend fun setPinCode(pin: String) {
        context.dataStore.edit { preferences -> preferences[PIN_CODE_KEY] = pin }
    }
    suspend fun setBiometricsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[BIOMETRICS_ENABLED_KEY] = enabled }
    }
    suspend fun setHideBalances(hide: Boolean) {
        context.dataStore.edit { preferences -> preferences[HIDE_BALANCES_KEY] = hide }
    }
    suspend fun setHideVault(hide: Boolean) {
        context.dataStore.edit { preferences -> preferences[HIDE_VAULT_KEY] = hide }
    }
    suspend fun setHideRecents(hide: Boolean) {
        context.dataStore.edit { preferences -> preferences[HIDE_RECENTS_KEY] = hide }
    }
    suspend fun setAutoLockTiming(timing: String) {
        context.dataStore.edit { preferences -> preferences[AUTO_LOCK_TIMING_KEY] = timing }
    }

    // Section 9 Setters
    suspend fun setReminderDaily(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_DAILY_KEY] = enabled } }
    suspend fun setReminderExpense(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_EXPENSE_KEY] = enabled } }
    suspend fun setReminderIncome(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_INCOME_KEY] = enabled } }
    suspend fun setReminderGoal(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_GOAL_KEY] = enabled } }
    suspend fun setReminderBorrow(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_BORROW_KEY] = enabled } }
    suspend fun setReminderLend(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_LEND_KEY] = enabled } }
    suspend fun setReminderSubscription(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_SUBSCRIPTION_KEY] = enabled } }
    suspend fun setReminderPocketMoney(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_POCKET_MONEY_KEY] = enabled } }
    suspend fun setReminderEmi(enabled: Boolean) { context.dataStore.edit { preferences -> preferences[REMINDER_EMI_KEY] = enabled } }
}
