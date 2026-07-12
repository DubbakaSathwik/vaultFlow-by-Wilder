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
import androidx.compose.ui.platform.testTag
import com.example.presentation.components.draggableFab
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.VaultFlowApplication
import com.example.domain.model.BorrowLendItem
import com.example.presentation.components.DeleteConfirmationDialog
import com.example.presentation.viewmodel.BorrowLendViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as VaultFlowApplication
    val appContainer = context.container
    val viewModel: BorrowLendViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return BorrowLendViewModel(appContainer.vaultRepository) as T
            }
        }
    )

    val items by viewModel.filteredItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterStatus by viewModel.filterStatus.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItemForEdit by remember { mutableStateOf<BorrowLendItem?>(null) }
    var itemForRepay by remember { mutableStateOf<BorrowLendItem?>(null) }
    var itemToDelete by remember { mutableStateOf<BorrowLendItem?>(null) }
    var repayAmountText by remember { mutableStateOf("") }
    var repayNotesText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Debt & Loans",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_debt_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Borrow/Lend")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .draggableFab()
                    .testTag("borrow_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Borrow/Lend")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Filter Headers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search person name, notes, or bank...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrow_search_field"),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf("All", "Borrowed", "Lended")
                    tabs.forEach { tab ->
                        val isSelected = filterType == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .clickable { viewModel.setFilterType(tab) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab == "Borrowed") "Money I Borrowed" else if (tab == "Lended") "Money Others Owe Me" else "All Types",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Filter row (Status, Sort)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Dropdown
                    var showStatusMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showStatusMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Status: $filterStatus", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            val statuses = listOf("All", "Pending", "Partially Paid", "Completed", "Cancelled")
                            statuses.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = {
                                        viewModel.setFilterStatus(s)
                                        showStatusMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Sort Dropdown
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Sort: $sortBy", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            val sorts = listOf("Date (Newest)", "Date (Oldest)", "Amount (High-Low)", "Amount (Low-High)", "Due Date")
                            sorts.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort) },
                                    onClick = {
                                        viewModel.setSortBy(sort)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Borrow/Lend Records Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add details of money you have borrowed or lent to keep your balance perfectly synced.",
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
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        BorrowLendItemCard(
                            item = item,
                            onRepayClick = { itemForRepay = item },
                            onMarkCompleted = { viewModel.markCompleted(item) },
                            onEditClick = { selectedItemForEdit = item },
                            onDeleteClick = { itemToDelete = item }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Record Repayment Dialog
    if (itemForRepay != null) {
        val current = itemForRepay!!
        AlertDialog(
            onDismissRequest = {
                itemForRepay = null
                repayAmountText = ""
                repayNotesText = ""
            },
            title = { Text("Log Repayment / Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Record payment/payout with ${current.personName} for ₹${current.amount}.")
                    Text(
                        "Remaining: ₹${current.remainingAmount}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = repayAmountText,
                        onValueChange = { repayAmountText = it },
                        label = { Text("Payment Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = repayNotesText,
                        onValueChange = { repayNotesText = it },
                        label = { Text("Payment Notes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountVal = repayAmountText.toDoubleOrNull()
                        if (amountVal != null && amountVal > 0) {
                            viewModel.recordPayment(current, amountVal, repayNotesText)
                        }
                        itemForRepay = null
                        repayAmountText = ""
                        repayNotesText = ""
                    }
                ) {
                    Text("Save Payout")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    itemForRepay = null
                    repayAmountText = ""
                    repayNotesText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (itemToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteBorrowLendItem(itemToDelete!!)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null },
            title = "Delete Debt/Loan Record",
            message = "Are you sure you want to delete the record for ${itemToDelete!!.personName} of ₹${itemToDelete!!.amount}?"
        )
    }

    // Add / Edit Dialog Form
    if (showAddDialog || selectedItemForEdit != null) {
        val editMode = selectedItemForEdit != null
        val editingItem = selectedItemForEdit

        var type by remember { mutableStateOf(editingItem?.type ?: "Borrowed") }
        var name by remember { mutableStateOf(editingItem?.personName ?: "") }
        var phone by remember { mutableStateOf(editingItem?.mobileNumber ?: "") }
        var amountText by remember { mutableStateOf(editingItem?.amount?.toString() ?: "") }
        var paidAmountText by remember { mutableStateOf(editingItem?.paidAmount?.toString() ?: "0.0") }
        var notes by remember { mutableStateOf(editingItem?.notes ?: "") }
        var bankField by remember { mutableStateOf(editingItem?.bank ?: "Primary Bank") }
        var payMethod by remember { mutableStateOf(editingItem?.paymentMethod ?: "Cash") }
        var receiptLink by remember { mutableStateOf(editingItem?.receipt ?: "") }
        var txLink by remember { mutableStateOf(editingItem?.transactionLink ?: "") }
        var statusField by remember { mutableStateOf(editingItem?.status ?: "Pending") }

        // Date selection placeholders
        var dateMs by remember { mutableStateOf(editingItem?.date ?: System.currentTimeMillis()) }
        var dueDateMs by remember { mutableStateOf(editingItem?.dueDate ?: (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)) }
        var timeText by remember { mutableStateOf(editingItem?.time ?: "12:00 PM") }

        val df = remember { SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                selectedItemForEdit = null
            },
            title = { Text(if (editMode) "Edit Record" else "New Debt/Loan Record") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        // Type Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("Borrowed", "Lended").forEach { typeOption ->
                                val selected = type == typeOption
                                FilterChip(
                                    selected = selected,
                                    onClick = { type = typeOption },
                                    label = { Text(if (typeOption == "Borrowed") "Money I Borrowed" else "Money Others Owe Me") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Person Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Total Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = { Text("Already Paid Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = bankField,
                            onValueChange = { bankField = it },
                            label = { Text("Bank / Account Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = payMethod,
                            onValueChange = { payMethod = it },
                            label = { Text("Payment Method") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = receiptLink,
                            onValueChange = { receiptLink = it },
                            label = { Text("Receipt Attachment Link / URI") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = txLink,
                            onValueChange = { txLink = it },
                            label = { Text("Deep Transaction Link (UPI Ref)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        // Simple Status Field
                        var showStatusDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showStatusDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Status: $statusField")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showStatusDropdown,
                                onDismissRequest = { showStatusDropdown = false }
                            ) {
                                listOf("Pending", "Partially Paid", "Completed", "Cancelled").forEach { statusOption ->
                                    DropdownMenuItem(
                                        text = { Text(statusOption) },
                                        onClick = {
                                            statusField = statusOption
                                            showStatusDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = df.format(Date(dateMs)),
                                onValueChange = {},
                                label = { Text("Date") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = dateMs }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val sel = Calendar.getInstance().apply { set(y, m, d) }
                                                dateMs = sel.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = df.format(Date(dueDateMs)),
                                onValueChange = {},
                                label = { Text("Due Date") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = dueDateMs }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val sel = Calendar.getInstance().apply { set(y, m, d) }
                                                dueDateMs = sel.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Select Due Date")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val paid = paidAmountText.toDoubleOrNull() ?: 0.0
                        val itemToSave = BorrowLendItem(
                            id = editingItem?.id ?: 0,
                            type = type,
                            personName = name,
                            profilePhoto = null,
                            mobileNumber = phone.ifBlank { null },
                            amount = amt,
                            paidAmount = paid,
                            date = dateMs,
                            dueDate = dueDateMs,
                            time = timeText,
                            bank = bankField.ifBlank { null },
                            paymentMethod = payMethod.ifBlank { null },
                            notes = notes.ifBlank { null },
                            receipt = receiptLink.ifBlank { null },
                            transactionLink = txLink.ifBlank { null },
                            status = statusField
                        )

                        if (editMode) {
                            viewModel.updateBorrowLendItem(itemToSave)
                        } else {
                            viewModel.createBorrowLendItem(itemToSave)
                        }

                        showAddDialog = false
                        selectedItemForEdit = null
                    }
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    selectedItemForEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BorrowLendItemCard(
    item: BorrowLendItem,
    onRepayClick: () -> Unit,
    onMarkCompleted: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val remaining = item.amount - item.paidAmount
    val isBorrowed = item.type == "Borrowed"

    val statusColor = when (item.status) {
        "Completed" -> Color(0xFF4CAF50)
        "Partially Paid" -> Color(0xFF2196F3)
        "Cancelled" -> Color(0xFF9E9E9E)
        else -> Color(0xFFFF9800) // Pending
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("borrow_item_card_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (isBorrowed) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBorrowed) Icons.Default.CallReceived else Icons.Default.CallMade,
                            contentDescription = null,
                            tint = if (isBorrowed) Color.Red else Color.Green,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.personName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBorrowed) "Money I Borrowed" else "Money Others Owe Me",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    contentColor = statusColor,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("₹${item.amount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("₹${item.paidAmount}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("₹$remaining", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (remaining > 0) Color.Red else Color.Green)
                }
            }

            // Timeline details
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.mobileNumber != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(item.mobileNumber, fontSize = 12.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Date: ${df.format(Date(item.date))} | Due: ${df.format(Date(item.dueDate))}", fontSize = 11.sp)
                }
                if (item.bank != null || item.paymentMethod != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bank: ${item.bank ?: "N/A"} | Method: ${item.paymentMethod ?: "N/A"}", fontSize = 11.sp)
                    }
                }
                if (item.notes != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(item.notes, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Unified Action & Management Row
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Management actions on the left
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Transaction actions on the right
                if (item.status != "Completed" && item.status != "Cancelled") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRepayClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Payout", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = onMarkCompleted,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Paid", fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                } else {
                    Text(
                        text = if (item.status == "Completed") "Settled" else "Cancelled",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.status == "Completed") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}
