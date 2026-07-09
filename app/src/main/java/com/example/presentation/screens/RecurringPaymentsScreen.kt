package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.domain.model.BankAccount
import com.example.domain.model.Category
import com.example.domain.model.RecurringItem
import com.example.presentation.viewmodel.RecurringPaymentsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringPaymentsScreen(
    viewModel: RecurringPaymentsViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0: Expenses, 1: Incomes
    val tabs = listOf("Recurring Expenses", "Recurring Incomes")

    val incomes by viewModel.recurringIncomes.collectAsState()
    val expenses by viewModel.recurringExpenses.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<RecurringItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Recurring Payments",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_recurring_btn")
                    ) {
                        Icon(Icons.Default.Add, "Add Recurring Rule")
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
            // Summary Banner
            SubscriptionSummaryCard(
                expenseTotal = expenses.filter { it.isActive }.sumOf { it.amount },
                incomeTotal = incomes.filter { it.isActive }.sumOf { it.amount }
            )

            // Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("recurring_tab_$index")
                    )
                }
            }

            // List of items
            Box(modifier = Modifier.weight(1f)) {
                val listToDisplay = if (activeTab == 0) expenses else incomes

                if (listToDisplay.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No recurring rules here",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tapping + will create schedules for salaries, rents, subsciptions, or pocket money.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        items(listToDisplay) { item ->
                            RecurringItemRowCard(
                                item = item,
                                onClick = { selectedItemForEdit = item },
                                onTriggerClick = { viewModel.triggerRecurringPayment(item) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecurringUpsertDialog(
            item = null,
            defaultType = if (activeTab == 0) "Expense" else "Income",
            onDismiss = { showAddDialog = false },
            onSave = { rule ->
                viewModel.addRecurringItem(rule)
                showAddDialog = false
            }
        )
    }

    selectedItemForEdit?.let { item ->
        RecurringUpsertDialog(
            item = item,
            defaultType = item.type,
            onDismiss = { selectedItemForEdit = null },
            onSave = { updatedRule ->
                viewModel.updateRecurringItem(updatedRule)
                selectedItemForEdit = null
            },
            onDelete = {
                viewModel.deleteRecurringItem(item)
                selectedItemForEdit = null
            }
        )
    }
}

// 1. Subscription summary visual banner
@Composable
fun SubscriptionSummaryCard(
    expenseTotal: Double,
    incomeTotal: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Monthly Recurring Out",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "₹ ${String.format("%,.2f", expenseTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Monthly Recurring In",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "₹ ${String.format("%,.2f", incomeTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// 2. Individual recurring row item card
@Composable
fun RecurringItemRowCard(
    item: RecurringItem,
    onClick: () -> Unit,
    onTriggerClick: () -> Unit
) {
    val color = if (item.type == "Income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recurring_item_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.type == "Income") Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = color
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.frequency} • ${item.categoryName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹ ${String.format("%,.2f", item.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = if (item.isActive) "Active" else "Paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
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
                Column {
                    Text(
                        text = "Linked: ${item.bankAccountName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Mode: " + if (item.isAutoAdd) "Auto-Post" else "Prompt Before Post",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.lastTriggeredTimestamp != null) {
                        Text(
                            text = "Last triggered: " + SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(item.lastTriggeredTimestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }

                Button(
                    onClick = onTriggerClick,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color.copy(alpha = 0.15f),
                        contentColor = color
                    )
                ) {
                    Icon(Icons.Default.FlashOn, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trigger Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 3. Upsert dialogue for schedules
@Composable
fun RecurringUpsertDialog(
    item: RecurringItem?,
    defaultType: String,
    onDismiss: () -> Unit,
    onSave: (RecurringItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(item?.type ?: defaultType) }
    var name by remember { mutableStateOf(item?.name ?: "") }
    var amountStr by remember { mutableStateOf(item?.amount?.toString() ?: "") }
    var frequency by remember { mutableStateOf(item?.frequency ?: "Monthly") }
    var bankAccountName by remember { mutableStateOf(item?.bankAccountName ?: "") }
    var paymentMethod by remember { mutableStateOf(item?.paymentMethod ?: "UPI") }
    var categoryName by remember { mutableStateOf(item?.categoryName ?: "Bills") }
    var notes by remember { mutableStateOf(item?.notes ?: "") }
    var isAutoAdd by remember { mutableStateOf(item?.isAutoAdd ?: true) }
    var hasReminder by remember { mutableStateOf(item?.hasReminder ?: true) }
    var isActive by remember { mutableStateOf(item?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item == null) "Schedule Recurring Event" else "Configure Schedule", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type filter
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "Expense",
                        onClick = { type = "Expense" },
                        label = { Text("Expense Schedule") }
                    )
                    FilterChip(
                        selected = type == "Income",
                        onClick = { type = "Income" },
                        label = { Text("Income Schedule") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event Name (e.g. Salary, Rent)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Frequency Dropdown
                ConfigDropdown(
                    label = "Recurrence Frequency",
                    selected = frequency,
                    options = listOf("Daily", "Weekly", "Monthly", "Yearly"),
                    onSelect = { frequency = it }
                )

                // Category selection mockup
                ConfigDropdown(
                    label = "Category",
                    selected = categoryName,
                    options = listOf("Bills", "Salary", "Food", "Shopping", "Pocket Money", "Others"),
                    onSelect = { categoryName = it }
                )

                ConfigDropdown(
                    label = "Payment Account",
                    selected = bankAccountName.ifEmpty { "Cash Wallet" },
                    options = listOf("Cash Wallet", "Primary SBI", "HDFC Salary", "PhonePe Wallet"),
                    onSelect = { bankAccountName = it }
                )

                if (type == "Expense") {
                    ConfigDropdown(
                        label = "Payment Method",
                        selected = paymentMethod,
                        options = listOf("UPI", "Cash", "Credit Card", "Debit Card", "Google Pay"),
                        onSelect = { paymentMethod = it }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = isAutoAdd, onCheckedChange = { isAutoAdd = it })
                    Column {
                        Text("Auto-Post without asking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Writes directly into transaction ledger when triggered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = hasReminder, onCheckedChange = { hasReminder = it })
                    Text("Trigger notification alert reminder")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Active schedule (uncheck to pause)")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Schedule Description Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val rule = RecurringItem(
                        id = item?.id ?: 0,
                        type = type,
                        name = name,
                        amount = amt,
                        frequency = frequency,
                        bankAccountId = item?.bankAccountId ?: 1, // mapping default
                        bankAccountName = bankAccountName.ifEmpty { "Cash Wallet" },
                        paymentMethod = paymentMethod,
                        categoryId = item?.categoryId ?: 1,
                        categoryName = categoryName,
                        notes = notes,
                        isAutoAdd = isAutoAdd,
                        askBeforeAdd = !isAutoAdd,
                        hasReminder = hasReminder,
                        lastTriggeredTimestamp = item?.lastTriggeredTimestamp,
                        nextTriggeredTimestamp = item?.nextTriggeredTimestamp,
                        isActive = isActive
                    )
                    onSave(rule)
                },
                enabled = name.isNotBlank() && amountStr.toDoubleOrNull() != null
            ) {
                Text("Confirm rule")
            }
        },
        dismissButton = {
            Row {
                if (item != null && onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
