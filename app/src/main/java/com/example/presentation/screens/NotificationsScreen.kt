package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.presentation.components.draggableFab
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.VaultFlowApplication
import com.example.domain.model.Reminder
import com.example.presentation.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as VaultFlowApplication
    val appContainer = context.container
    val viewModel: ReminderViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ReminderViewModel(appContainer.vaultRepository) as T
            }
        }
    )

    val reminders by viewModel.allReminders.collectAsState()
    val activeReminders by viewModel.activeReminders.collectAsState()

    // Simulated alerts from before
    val notifications = remember {
        listOf(
            AppNotification(
                title = "Expense Alert: Food Budget",
                description = "You have consumed 82% of your custom food group category budget. Consider slowing down expenses.",
                category = "Budget",
                timestamp = System.currentTimeMillis() - 4 * 60 * 60 * 1000L,
                unread = true
            ),
            AppNotification(
                title = "Auto-renewal Notice: Netflix Premium",
                description = "Recurring monthly sub of ₹649.0 is scheduled in 2 days from SBI Savings Primary.",
                category = "Dues",
                timestamp = System.currentTimeMillis() - 22 * 60 * 60 * 1000L,
                unread = true
            ),
            AppNotification(
                title = "Daily Financial Summary",
                description = "Yesterday you saved ₹1,200.0 compared to average weekly logs. Fantastic work keeping cash flow high!",
                category = "Daily",
                timestamp = System.currentTimeMillis() - 30 * 60 * 60 * 1000L,
                unread = false
            ),
            AppNotification(
                title = "Security Check-in Complete",
                description = "Biometrics configuration updated successfully from profile security console.",
                category = "System",
                timestamp = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L,
                unread = false
            ),
            AppNotification(
                title = "Lend Recovery: Sathwik",
                description = "Lended balance cue of ₹2,500.0 is marked as overdue. Send gentle reminder link?",
                category = "Dues",
                timestamp = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L,
                unread = false
            )
        )
    }

    var currentTab by remember { mutableStateOf("Alerts") } // "Alerts" or "Reminders"
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var selectedReminderForEdit by remember { mutableStateOf<Reminder?>(null) }
    var showOnlyUnreadAlerts by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Alerts & Reminders",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                actions = {
                    if (currentTab == "Reminders") {
                        IconButton(
                            onClick = { showAddReminderDialog = true },
                            modifier = Modifier.testTag("add_reminder_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Reminder")
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Unread", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = showOnlyUnreadAlerts,
                                onCheckedChange = { showOnlyUnreadAlerts = it },
                                modifier = Modifier.testTag("unread_notifications_switch")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (currentTab == "Reminders") {
                FloatingActionButton(
                    onClick = { showAddReminderDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .draggableFab()
                        .testTag("reminder_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reminder")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                listOf("Alerts" to "System Alerts", "Reminders" to "Smart Reminders").forEach { (tabId, label) ->
                    val isSelected = currentTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { currentTab = tabId }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (currentTab == "Alerts") {
                val filteredAlerts = if (showOnlyUnreadAlerts) {
                    notifications.filter { it.unread }
                } else {
                    notifications
                }

                if (filteredAlerts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your inbox is empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Inbox alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(filteredAlerts) { notif ->
                            NotificationItemCard(notif)
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            } else {
                // Smart Reminders Screen
                if (reminders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Reminders Created",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add reminders for Borrow/Lend, Savings Goals, Pocket Money, EMIs, Subscriptions, and never miss a payment cycle.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Your Smart Reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeReminders.size} Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(reminders, key = { it.id }) { reminder ->
                            ReminderItemCard(
                                reminder = reminder,
                                onComplete = { viewModel.markCompleted(reminder) },
                                onDismiss = { viewModel.dismissReminder(reminder) },
                                onDelete = { viewModel.deleteReminder(reminder) },
                                onEdit = { selectedReminderForEdit = reminder }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Reminder Dialog
    if (showAddReminderDialog || selectedReminderForEdit != null) {
        val editMode = selectedReminderForEdit != null
        val editing = selectedReminderForEdit

        var title by remember { mutableStateOf(editing?.title ?: "") }
        var description by remember { mutableStateOf(editing?.description ?: "") }
        var type by remember { mutableStateOf(editing?.type ?: "Custom Reminder") }
        var priority by remember { mutableStateOf(editing?.priority ?: "Medium") }
        var repeat by remember { mutableStateOf(editing?.repeat ?: "None") }
        var notifyEnabled by remember { mutableStateOf(editing?.notification ?: true) }
        var dateMs by remember { mutableStateOf(editing?.reminderDate ?: System.currentTimeMillis()) }
        var timeText by remember { mutableStateOf(editing?.reminderTime ?: "09:00 AM") }

        AlertDialog(
            onDismissRequest = {
                showAddReminderDialog = false
                selectedReminderForEdit = null
            },
            title = { Text(if (editMode) "Edit Reminder" else "Create Smart Reminder") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        // Type Selection dropdown
                        var showTypeDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showTypeDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Category: $type")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showTypeDropdown,
                                onDismissRequest = { showTypeDropdown = false }
                            ) {
                                val categoriesList = listOf(
                                    "Borrow Due", "Lend Due", "Goal Saving Reminder",
                                    "Pocket Money Reminder", "Recurring Income", "Recurring Expense",
                                    "EMI", "Subscription", "Custom Reminder"
                                )
                                categoriesList.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            type = cat
                                            showTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Priority selection dropdown
                        var showPriorityDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showPriorityDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Priority: $priority")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showPriorityDropdown,
                                onDismissRequest = { showPriorityDropdown = false }
                            ) {
                                listOf("Low", "Medium", "High").forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p) },
                                        onClick = {
                                            priority = p
                                            showPriorityDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Repeat selection dropdown
                        var showRepeatDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showRepeatDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Repeat cycle: $repeat")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showRepeatDropdown,
                                onDismissRequest = { showRepeatDropdown = false }
                            ) {
                                listOf("None", "Daily", "Weekly", "Monthly").forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r) },
                                        onClick = {
                                            repeat = r
                                            showRepeatDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = timeText,
                            onValueChange = { timeText = it },
                            label = { Text("Time (e.g. 09:00 AM)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Trigger Push Notification")
                            Switch(checked = notifyEnabled, onCheckedChange = { notifyEnabled = it })
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { dateMs = System.currentTimeMillis() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Text("Today Date", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { dateMs = System.currentTimeMillis() + 24 * 60 * 60 * 1000L },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) {
                                Text("Tomorrow Date", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toSave = Reminder(
                            id = editing?.id ?: 0,
                            title = title,
                            description = description,
                            priority = priority,
                            reminderDate = dateMs,
                            reminderTime = timeText,
                            repeat = repeat,
                            completed = editing?.completed ?: false,
                            dismissed = editing?.dismissed ?: false,
                            notification = notifyEnabled,
                            type = type
                        )

                        if (editMode) {
                            viewModel.updateReminder(toSave)
                        } else {
                            viewModel.createReminder(toSave)
                        }

                        showAddReminderDialog = false
                        selectedReminderForEdit = null
                    }
                ) {
                    Text("Save Reminder")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddReminderDialog = false
                    selectedReminderForEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ReminderItemCard(
    reminder: Reminder,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val priorityColor = when (reminder.priority) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    val icon = when (reminder.type) {
        "Borrow Due" -> Icons.Default.CallReceived
        "Lend Due" -> Icons.Default.CallMade
        "Goal Saving Reminder" -> Icons.Default.Star
        "Pocket Money Reminder" -> Icons.Default.Savings
        "EMI" -> Icons.Default.AccountBalance
        "Subscription" -> Icons.Default.EventRepeat
        else -> Icons.Default.Alarm
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_item_${reminder.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.completed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(priorityColor.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = priorityColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (reminder.completed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${reminder.type} | Priority: ${reminder.priority}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                if (reminder.completed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = reminder.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${df.format(Date(reminder.reminderDate))} at ${reminder.reminderTime}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (reminder.repeat != "None") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Repeat: ${reminder.repeat}",
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!reminder.completed && !reminder.dismissed) {
                        IconButton(onClick = onComplete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Done, contentDescription = "Complete", modifier = Modifier.size(18.dp), tint = Color(0xFF4CAF50))
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

data class AppNotification(
    val title: String,
    val description: String,
    val category: String, // "Daily", "Budget", "Dues", "System"
    val timestamp: Long,
    val unread: Boolean
)

@Composable
fun NotificationItemCard(notif: AppNotification) {
    val categoryColor = when (notif.category) {
        "Budget" -> MaterialTheme.colorScheme.error
        "Dues" -> MaterialTheme.colorScheme.secondary
        "Daily" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    val icon = when (notif.category) {
        "Budget" -> Icons.Default.Warning
        "Dues" -> Icons.Default.EventNote
        "Daily" -> Icons.Default.Insights
        else -> Icons.Default.Lock
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_item_${notif.timestamp}"),
        colors = CardDefaults.cardColors(
            containerColor = if (notif.unread) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (notif.unread) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(categoryColor.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notif.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (notif.unread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notif.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(notif.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}
