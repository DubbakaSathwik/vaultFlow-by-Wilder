package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class AppNotification(
    val title: String,
    val description: String,
    val category: String, // "Daily", "Budget", "Dues", "System"
    val timestamp: Long,
    val unread: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(modifier: Modifier = Modifier) {
    // Generate simulated notification items based on realistic financial triggers
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

    var showOnlyUnread by remember { mutableStateOf(false) }

    val filteredNotifications = if (showOnlyUnread) {
        notifications.filter { it.unread }
    } else {
        notifications
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Alert Hub",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Unread", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = showOnlyUnread,
                            onCheckedChange = { showOnlyUnread = it },
                            modifier = Modifier.testTag("unread_notifications_switch")
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (filteredNotifications.isEmpty()) {
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
                        Text(
                            text = "No pending notifications or budget threshold limit alerts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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

                    items(filteredNotifications) { notif ->
                        NotificationItemCard(notif)
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

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
