package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.SettingsViewModel

data class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val useDynamicColor by viewModel.useDynamicColor.collectAsState()
    
    // Section 6 Appearance
    val accentColor by viewModel.accentColor.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val animationSpeed by viewModel.animationSpeed.collectAsState()

    // Section 7 Date & Time
    val useDeviceTime by viewModel.useDeviceTime.collectAsState()
    val timeOffset by viewModel.vaultFlowTimeOffset.collectAsState()
    val timezone by viewModel.vaultFlowTimezone.collectAsState()

    // Section 8 Security
    val pinEnabled by viewModel.pinEnabled.collectAsState()
    val pinCode by viewModel.pinCode.collectAsState()
    val biometricsEnabled by viewModel.biometricsEnabled.collectAsState()
    val hideBalances by viewModel.hideBalances.collectAsState()
    val hideVault by viewModel.hideVault.collectAsState()
    val hideRecents by viewModel.hideRecents.collectAsState()
    val autoLockTiming by viewModel.autoLockTiming.collectAsState()

    // Section 9 Notifications
    val remDaily by viewModel.reminderDaily.collectAsState()
    val remExpense by viewModel.reminderExpense.collectAsState()
    val remIncome by viewModel.reminderIncome.collectAsState()
    val remGoal by viewModel.reminderGoal.collectAsState()
    val remBorrow by viewModel.reminderBorrow.collectAsState()
    val remLend by viewModel.reminderLend.collectAsState()
    val remSub by viewModel.reminderSubscription.collectAsState()
    val remPocket by viewModel.reminderPocketMoney.collectAsState()
    val remEmi by viewModel.reminderEmi.collectAsState()

    var selectedCategoryForDetail by remember { mutableStateOf<SettingsCategory?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Dialog trigger states
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showDateTimeDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        SettingsCategory("Appearance Panel", Icons.Default.Palette, "Manage font sizes, accent colors, and animations.", "appearance_panel"),
        SettingsCategory("App Security Locks", Icons.Default.Security, "Configure secure PIN, biometrics and privacy masks.", "security"),
        SettingsCategory("Vault Date & Time", Icons.Default.Event, "Manage timezone offsets and simulated travel time.", "date_time"),
        SettingsCategory("Notification Reminders", Icons.Default.Notifications, "Set system alert thresholds and reminders.", "notifications"),
        SettingsCategory("Reset Preferences", Icons.Default.Refresh, "Revert all settings and onboard state to factory defaults.", "reset")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LargeTopAppBar(
                title = {
                    Text(
                        "VaultFlow Settings",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Appearance Panel (Current Quick Theme)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("appearance_quick_panel"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                "Quick Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Theme Mode",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Current Mode: $themeMode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Button(
                                    onClick = { showThemeDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Change")
                                }
                            }
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Dynamic Material Colors",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Extract colors from Android system theme.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Switch(
                                    checked = useDynamicColor,
                                    onCheckedChange = { viewModel.setUseDynamicColor(it) },
                                    modifier = Modifier.testTag("dynamic_color_switch")
                                )
                            }
                        }
                    }
                }

                // List of setting panels
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (category.tag) {
                                    "appearance_panel" -> showAppearanceDialog = true
                                    "security" -> showSecurityDialog = true
                                    "date_time" -> showDateTimeDialog = true
                                    "notifications" -> showNotificationDialog = true
                                    "reset" -> viewModel.resetOnboarding()
                                }
                            }
                            .testTag("settings_category_${category.tag}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    category.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    category.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Sub-dialogs implementing fully functioning preferences

        // 1. Theme picker dialog
        if (showThemeDialog) {
            val themes = listOf("System", "Light", "Dark", "AMOLED", "Blue Space", "Emerald Rose")
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Select Theme Mode", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        themes.forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setThemeMode(theme)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themeMode == theme,
                                    onClick = {
                                        viewModel.setThemeMode(theme)
                                        showThemeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(theme, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 2. Section 6: Full Appearance Config
        if (showAppearanceDialog) {
            AlertDialog(
                onDismissRequest = { showAppearanceDialog = false },
                title = { Text("Appearance Settings", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Accent Color Palette", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        val colors = listOf("Default", "Sky Blue", "Forest Green", "Amber Gold", "Crimson Red", "Deep Indigo")
                        colors.forEach { color ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAccentColor(color) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = accentColor == color, onClick = { viewModel.setAccentColor(color) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(color)
                            }
                        }

                        HorizontalDivider()

                        Text("App Font Size", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        val sizes = listOf("Small", "Medium", "Large", "Extra Large")
                        sizes.forEach { size ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setFontSize(size) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = fontSize == size, onClick = { viewModel.setFontSize(size) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(size)
                            }
                        }

                        HorizontalDivider()

                        Text("Animation Speed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        val speeds = listOf("Standard", "High Performance", "Minimal")
                        speeds.forEach { speed ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAnimationSpeed(speed) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = animationSpeed == speed, onClick = { viewModel.setAnimationSpeed(speed) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(speed)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAppearanceDialog = false }) {
                        Text("Apply")
                    }
                }
            )
        }

        // 3. Section 8: Security Config Dialog
        if (showSecurityDialog) {
            AlertDialog(
                onDismissRequest = { showSecurityDialog = false },
                title = { Text("App Lock & Privacy Security", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Secure PIN Lock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Prompt for a digits code on app entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(checked = pinEnabled, onCheckedChange = { viewModel.setPinEnabled(it) })
                        }

                        if (pinEnabled) {
                            var customPin by remember { mutableStateOf(pinCode) }
                            OutlinedTextField(
                                value = customPin,
                                onValueChange = {
                                    if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                        customPin = it
                                        viewModel.setPinCode(it)
                                    }
                                },
                                label = { Text("Set 4-Digit Secure PIN") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric Authentication", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Unlock with fingerprint or face ID recognition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(checked = biometricsEnabled, onCheckedChange = { viewModel.setBiometricsEnabled(it) })
                        }

                        HorizontalDivider()

                        Text("Privacy Masks (Section 8)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mask account bank balances")
                            Switch(checked = hideBalances, onCheckedChange = { viewModel.setHideBalances(it) })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hide vault item values")
                            Switch(checked = hideVault, onCheckedChange = { viewModel.setHideVault(it) })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hide recent transaction details")
                            Switch(checked = hideRecents, onCheckedChange = { viewModel.setHideRecents(it) })
                        }

                        HorizontalDivider()

                        Text("Auto-Lock Inactivity Timing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        val timings = listOf("Immediately", "30 Seconds", "1 Minute", "5 Minutes", "Never")
                        timings.forEach { timing ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAutoLockTiming(timing) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = autoLockTiming == timing, onClick = { viewModel.setAutoLockTiming(timing) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(timing)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSecurityDialog = false }) {
                        Text("Save Profile")
                    }
                }
            )
        }

        // 4. Section 7: Date & Time System Dialog (VaultFlow Time)
        if (showDateTimeDialog) {
            AlertDialog(
                onDismissRequest = { showDateTimeDialog = false },
                title = { Text("VaultFlow Date & Time System", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Use Device Synchronized Time", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Uses standard Android device system datetime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Switch(checked = useDeviceTime, onCheckedChange = { viewModel.setUseDeviceTime(it) })
                        }

                        if (!useDeviceTime) {
                            Text("VaultFlow Time Travel Offset", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Simulate future or past datetimes (hours offset)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            
                            var currentOffsetStr by remember { mutableStateOf((timeOffset / (60 * 60 * 1000L)).toString()) }
                            OutlinedTextField(
                                value = currentOffsetStr,
                                onValueChange = {
                                    currentOffsetStr = it
                                    val hr = it.toLongOrNull() ?: 0L
                                    viewModel.setVaultFlowTimeOffset(hr * 60 * 60 * 1000L)
                                },
                                label = { Text("Hours Offset (e.g. +24 for next day)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider()

                        Text("VaultFlow Simulated Timezone", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        val zones = listOf("UTC", "GMT", "IST (India)", "EST (US East)", "PST (US West)")
                        zones.forEach { zone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setVaultFlowTimezone(zone) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = timezone == zone, onClick = { viewModel.setVaultFlowTimezone(zone) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(zone)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDateTimeDialog = false }) {
                        Text("Apply")
                    }
                }
            )
        }

        // 5. Section 9: Custom Alerts & Notification Triggers Dialog
        if (showNotificationDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationDialog = false },
                title = { Text("App Alert Notifications", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Select Alert Subscriptions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remDaily, onCheckedChange = { viewModel.setReminderDaily(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily financial summary journal alert")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remExpense, onCheckedChange = { viewModel.setReminderExpense(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Expense threshold budget limits alerts")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remIncome, onCheckedChange = { viewModel.setReminderIncome(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Income credit receipt confirmations")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remGoal, onCheckedChange = { viewModel.setReminderGoal(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Savings goal milestone alarms")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remBorrow, onCheckedChange = { viewModel.setReminderBorrow(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Borrow dues repayment reminders")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remLend, onCheckedChange = { viewModel.setReminderLend(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lend recovery follow-up updates")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remSub, onCheckedChange = { viewModel.setReminderSubscription(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-renewal subscription alerts")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remPocket, onCheckedChange = { viewModel.setReminderPocketMoney(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pocket money balance alerts")
                        }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = remEmi, onCheckedChange = { viewModel.setReminderEmi(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Monthly loan EMI & recurring bill logs")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNotificationDialog = false }) {
                        Text("Save Configurations")
                    }
                }
            )
        }
    }
}
