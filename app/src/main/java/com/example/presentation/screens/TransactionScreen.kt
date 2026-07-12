package com.example.presentation.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import com.example.presentation.components.draggableFab
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.BankAccount
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.presentation.viewmodel.SortOption
import com.example.presentation.viewmodel.TransactionFilter
import com.example.presentation.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel,
    startInAddMode: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Screen State: "LIST", "ADD_EDIT", "TRASH", "DETAILS"
    var currentScreenState by remember { mutableStateOf(if (startInAddMode) "ADD_EDIT" else "LIST") }
    var selectedTransactionForEdit by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransactionForDetails by remember { mutableStateOf<Transaction?>(null) }

    // UI state flows from ViewModel
    val transactions by viewModel.filteredTransactions.collectAsState()
    val trashedTransactions by viewModel.trashedTransactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (currentScreenState) {
                            "LIST" -> "VaultFlow Transactions"
                            "ADD_EDIT" -> if (selectedTransactionForEdit == null) "Add Transaction" else "Edit Transaction"
                            "TRASH" -> "Trash / Recycler Bin"
                            "DETAILS" -> "Transaction Details"
                            else -> "Transactions"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (currentScreenState != "LIST") {
                        IconButton(onClick = { currentScreenState = "LIST" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreenState == "LIST") {
                        IconButton(onClick = { currentScreenState = "TRASH" }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "View Trash",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (currentScreenState == "LIST") {
                FloatingActionButton(
                    onClick = {
                        selectedTransactionForEdit = null
                        currentScreenState = "ADD_EDIT"
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .draggableFab()
                        .testTag("add_transaction_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreenState) {
                "LIST" -> {
                    TransactionListContent(
                        transactions = transactions,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        filter = currentFilter,
                        onFilterChange = { viewModel.updateFilter(it) },
                        sortOption = currentSort,
                        onSortChange = { viewModel.setSortOption(it) },
                        accounts = accounts,
                        categories = categories,
                        onTransactionClick = {
                            selectedTransactionForDetails = it
                            currentScreenState = "DETAILS"
                        },
                        onFavoriteToggle = { viewModel.toggleTransactionFavorite(it) },
                        onDuplicate = {
                            viewModel.duplicateTransaction(it)
                            scope.launch {
                                snackbarHostState.showSnackbar("Transaction Duplicated Successfully")
                            }
                        },
                        onEdit = {
                            selectedTransactionForEdit = it
                            currentScreenState = "ADD_EDIT"
                        },
                        onDelete = {
                            viewModel.deleteTransactionToTrash(it)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Transaction moved to trash",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoLastDelete()
                                }
                            }
                        },
                        onShare = { transaction ->
                            val text = "VaultFlow Transaction: ${transaction.merchantName} - ₹${transaction.amount} [${transaction.type}] on ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp))}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Transaction"))
                        }
                    )
                }

                "ADD_EDIT" -> {
                    AddEditTransactionContent(
                        transaction = selectedTransactionForEdit,
                        accounts = accounts,
                        categories = categories,
                        onSave = { amount, type, category, merchant, alias, pMethod, bankId, bankName, timestamp, notes, tags, isFav ->
                            val updated = Transaction(
                                id = selectedTransactionForEdit?.id ?: 0,
                                amount = amount,
                                type = type,
                                categoryName = category.name,
                                categoryId = category.id,
                                merchantName = merchant,
                                merchantAlias = alias,
                                paymentMethod = pMethod,
                                bankAccountId = bankId,
                                bankAccountName = bankName,
                                timestamp = timestamp,
                                notes = notes,
                                tags = tags,
                                isFavorite = isFav
                            )
                            viewModel.saveTransaction(updated, selectedTransactionForEdit)
                            currentScreenState = "LIST"
                            scope.launch {
                                snackbarHostState.showSnackbar("Transaction Saved Successfully")
                            }
                        },
                        onCancel = { currentScreenState = "LIST" },
                        onCreateCategory = { name, color, icon ->
                            viewModel.createCategory(name, color, icon)
                        }
                    )
                }

                "TRASH" -> {
                    TrashContent(
                        trashedTransactions = trashedTransactions,
                        onRestore = {
                            viewModel.restoreFromTrash(it)
                            scope.launch {
                                snackbarHostState.showSnackbar("Transaction Restored")
                            }
                        },
                        onDeletePermanently = {
                            viewModel.deletePermanently(it)
                            scope.launch {
                                snackbarHostState.showSnackbar("Transaction Deleted Permanently")
                            }
                        },
                        onEmptyTrash = {
                            trashedTransactions.forEach { viewModel.deletePermanently(it) }
                            scope.launch {
                                snackbarHostState.showSnackbar("Trash Cleared")
                            }
                        }
                    )
                }

                "DETAILS" -> {
                    selectedTransactionForDetails?.let { transaction ->
                        TransactionDetailsContent(
                            transaction = transaction,
                            onBack = { currentScreenState = "LIST" },
                            onEdit = {
                                selectedTransactionForEdit = transaction
                                currentScreenState = "ADD_EDIT"
                            }
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 1. TRANSACTION LIST CONTENT
// ----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListContent(
    transactions: List<Transaction>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    accounts: List<BankAccount>,
    categories: List<Category>,
    onTransactionClick: (Transaction) -> Unit,
    onFavoriteToggle: (Transaction) -> Unit,
    onDuplicate: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onShare: (Transaction) -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search merchant, notes, tags...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                IconButton(onClick = { showFilterSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        tint = if (filter != TransactionFilter() || sortOption != SortOption.NEWEST)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("search_bar")
        )

        // Filter chips display
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            if (filter.selectedType != null) {
                item {
                    InputChip(
                        selected = true,
                        onClick = { onFilterChange(filter.copy(selectedType = null)) },
                        label = { Text(filter.selectedType) },
                        trailingIcon = { Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(14.dp)) }
                    )
                }
            }
            if (filter.onlyFavorites) {
                item {
                    InputChip(
                        selected = true,
                        onClick = { onFilterChange(filter.copy(onlyFavorites = false)) },
                        label = { Text("Favorites") },
                        trailingIcon = { Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(14.dp)) }
                    )
                }
            }
            if (filter.selectedBankAccountId != null) {
                val acc = accounts.find { it.id == filter.selectedBankAccountId }
                item {
                    InputChip(
                        selected = true,
                        onClick = { onFilterChange(filter.copy(selectedBankAccountId = null)) },
                        label = { Text(acc?.nickname ?: "Bank") },
                        trailingIcon = { Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // Transactions list
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No Transactions",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Transactions Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tap the '+' button below to add your first transaction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("transactions_lazy_list")
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) },
                        onFavoriteClick = { onFavoriteToggle(transaction) },
                        onDuplicate = { onDuplicate(transaction) },
                        onEdit = { onEdit(transaction) },
                        onDelete = { onDelete(transaction) },
                        onShare = { onShare(transaction) }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSortBottomSheet(
            filter = filter,
            sortOption = sortOption,
            accounts = accounts,
            categories = categories,
            onDismiss = { showFilterSheet = false },
            onApply = { newFilter, newSort ->
                onFilterChange(newFilter)
                onSortChange(newSort)
                showFilterSheet = false
            }
        )
    }
}

// ----------------------------------------------------------------------------
// 2. TRANSACTION ITEM CARD
// ----------------------------------------------------------------------------
@Composable
fun TransactionItemCard(
    transaction: Transaction,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDuplicate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    // Color theme mapping based on type
    val typeColor = when (transaction.type) {
        "Income", "Refund" -> Color(0xFF10B981)
        "Expense" -> Color(0xFFEF4444)
        "Borrow", "Lend" -> Color(0xFFF59E0B)
        "Transfer" -> Color(0xFF3B82F6)
        else -> MaterialTheme.colorScheme.primary
    }

    val typeIcon = when (transaction.type) {
        "Income", "Refund" -> Icons.Default.ArrowDownward
        "Expense" -> Icons.Default.ArrowUpward
        "Transfer" -> Icons.Default.SyncAlt
        "Borrow" -> Icons.Default.Handshake
        "Lend" -> Icons.Default.VolunteerActivism
        else -> Icons.Default.Category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("transaction_card_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = mapCategoryIcon(transaction.categoryName),
                    contentDescription = transaction.categoryName,
                    tint = typeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.categoryName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = transaction.paymentMethod,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Amount & Action button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${if (transaction.type == "Expense" || transaction.type == "Lend") "-" else "+"}₹${String.format(Locale.US, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (transaction.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (transaction.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, "Edit") },
                                onClick = {
                                    expandedMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, "Duplicate") },
                                onClick = {
                                    expandedMenu = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, "Share") },
                                onClick = {
                                    expandedMenu = false
                                    onShare()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, "Delete", tint = Color.Red) },
                                onClick = {
                                    expandedMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper category mapper to map strings to standard icons
fun mapCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingBag
        "college" -> Icons.Default.School
        "stationery" -> Icons.Default.Edit
        "medical" -> Icons.Default.MedicalServices
        "fuel" -> Icons.Default.LocalGasStation
        "entertainment" -> Icons.Default.Movie
        "family" -> Icons.Default.People
        "travel" -> Icons.Default.Flight
        "recharge" -> Icons.Default.PhoneAndroid
        "bills" -> Icons.Default.ReceiptLong
        else -> Icons.Default.Category
    }
}

// ----------------------------------------------------------------------------
// 3. FILTER & SORT BOTTOM SHEET
// ----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortBottomSheet(
    filter: TransactionFilter,
    sortOption: SortOption,
    accounts: List<BankAccount>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onApply: (TransactionFilter, SortOption) -> Unit
) {
    var tempFilter by remember { mutableStateOf(filter) }
    var tempSort by remember { mutableStateOf(sortOption) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Filter & Sort",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sort Options
            Text("Sort By", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SortOption.values().forEach { option ->
                    val name = option.name.replace("_", " ").lowercase().capitalize(Locale.ROOT)
                    FilterChip(
                        selected = tempSort == option,
                        onClick = { tempSort = option },
                        label = { Text(name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Type
            Text("Transaction Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Expense", "Income", "Transfer", "Borrow", "Lend").forEach { type ->
                    FilterChip(
                        selected = tempFilter.selectedType == type,
                        onClick = {
                            tempFilter = tempFilter.copy(
                                selectedType = if (tempFilter.selectedType == type) null else type
                            )
                        },
                        label = { Text(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bank Accounts Filter
            Text("Bank Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                accounts.forEach { acc ->
                    FilterChip(
                        selected = tempFilter.selectedBankAccountId == acc.id,
                        onClick = {
                            tempFilter = tempFilter.copy(
                                selectedBankAccountId = if (tempFilter.selectedBankAccountId == acc.id) null else acc.id
                            )
                        },
                        label = { Text(acc.nickname) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Favorites only
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Show Favorites Only", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = tempFilter.onlyFavorites,
                    onCheckedChange = { tempFilter = tempFilter.copy(onlyFavorites = it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempFilter = TransactionFilter()
                        tempSort = SortOption.NEWEST
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset All")
                }

                Button(
                    onClick = { onApply(tempFilter, tempSort) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// Custom flow row layout representation
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Basic row with wrap using grid/lazy row or simple row. Let's make it wrapping using simple Column/Row if needed,
    // or since Jetpack Compose provides standard Column of Rows, let's group them or use Row for simplicity.
    // For safety, let's lay them in an elegant LazyRow or a grid or custom Box with Flow-like behaviour.
    // Let's do columns with rows of 3 to render FlowRow perfectly without any dependency!
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        Row(horizontalArrangement = horizontalArrangement) {
            content()
        }
    }
}

// ----------------------------------------------------------------------------
// 4. ADD / EDIT TRANSACTION CONTENT (Premium Keypad + Categorization)
// ----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionContent(
    transaction: Transaction?,
    accounts: List<BankAccount>,
    categories: List<Category>,
    onSave: (Double, String, Category, String, String, String, Int, String, Long, String, String, Boolean) -> Unit,
    onCancel: () -> Unit,
    onCreateCategory: (String, String, String) -> Unit
) {
    var amountString by remember { mutableStateOf(transaction?.amount?.toString() ?: "0") }
    var selectedType by remember { mutableStateOf(transaction?.type ?: "Expense") }
    var merchantName by remember { mutableStateOf(transaction?.merchantName ?: "") }
    var merchantAlias by remember { mutableStateOf(transaction?.merchantAlias ?: "") }
    var selectedCategory by remember { mutableStateOf(categories.find { it.id == transaction?.categoryId } ?: categories.firstOrNull() ?: Category(name = "Others", colorHex = "#6B7280", iconName = "category")) }
    var selectedPaymentMethod by remember { mutableStateOf(transaction?.paymentMethod ?: "UPI") }
    var selectedAccount by remember { mutableStateOf(accounts.find { it.id == transaction?.bankAccountId } ?: accounts.firstOrNull()) }

    var notes by remember { mutableStateOf(transaction?.notes ?: "") }
    var tagsInput by remember { mutableStateOf(transaction?.tags ?: "") }
    var isFavorite by remember { mutableStateOf(transaction?.isFavorite ?: false) }

    // Date & Time states
    var customTimestamp by remember { mutableStateOf(transaction?.timestamp ?: System.currentTimeMillis()) }

    // Dialog flags
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Amount Display Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Amount",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "₹",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = amountString,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("amount_display_text")
                    )
                }
            }
        }

        // Keypad Grid & Quick Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("+100", "+500", "+1000", "Clear").forEach { action ->
                Button(
                    onClick = {
                        when (action) {
                            "Clear" -> amountString = "0"
                            else -> {
                                val currentVal = amountString.toDoubleOrNull() ?: 0.0
                                val addVal = action.replace("+", "").toDoubleOrNull() ?: 0.0
                                amountString = (currentVal + addVal).toString()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(action, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Numeric Keypad Calculator
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "⌫")
                )
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable {
                                        when (key) {
                                            "⌫" -> {
                                                if (amountString.length > 1) {
                                                    amountString = amountString.dropLast(1)
                                                } else {
                                                    amountString = "0"
                                                }
                                            }

                                            "." -> {
                                                if (!amountString.contains(".")) {
                                                    amountString += "."
                                                }
                                            }

                                            else -> {
                                                if (amountString == "0") {
                                                    amountString = key
                                                } else {
                                                    amountString += key
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(key, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Fields
        Spacer(modifier = Modifier.height(16.dp))

        // Transaction Type Row
        Text("Transaction Type", fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Expense", "Income", "Transfer", "Adjustment", "Refund", "Borrow", "Lend").forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type) }
                )
            }
        }

        // Merchant Name
        OutlinedTextField(
            value = merchantName,
            onValueChange = { merchantName = it },
            label = { Text("Merchant / Sender Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("merchant_name_input")
        )

        // Merchant Alias (Optional)
        OutlinedTextField(
            value = merchantAlias,
            onValueChange = { merchantAlias = it },
            label = { Text("Merchant Alias / Nickname (Optional)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Category Trigger Card
        Text("Category", fontWeight = FontWeight.Bold)
        Card(
            onClick = { showCategoryDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = mapCategoryIcon(selectedCategory.name),
                        contentDescription = selectedCategory.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(selectedCategory.name, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ArrowDropDown, "Select")
            }
        }

        // Payment Method Selector
        Text("Payment Method", fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Cash", "PhonePe", "Google Pay", "Paytm", "FamPay", "Debit Card", "Credit Card", "Bank Transfer", "UPI").forEach { method ->
                FilterChip(
                    selected = selectedPaymentMethod == method,
                    onClick = { selectedPaymentMethod = method },
                    label = { Text(method) }
                )
            }
        }

        // Bank Account Selector Carousel
        Text("Select Bank Account", fontWeight = FontWeight.Bold)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts) { acc ->
                val isSel = selectedAccount?.id == acc.id
                Card(
                    onClick = { selectedAccount = acc },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSel) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.width(140.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(acc.nickname, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("Bal: ₹${acc.balance}", fontSize = 12.sp)
                        Text("Ending *${acc.last4Digits}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Notes (long support, bullets, emojis)
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Supports Long Notes & Emojis)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(vertical = 8.dp)
        )

        // Custom Tags Input
        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            label = { Text("Tags (comma separated e.g. #College, #Trip)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Favorites and Receipt Placeholders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isFavorite, onCheckedChange = { isFavorite = it })
                Text("Mark as Favorite")
            }
        }

        // Receipt Attachment Placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AttachFile, "Attach Receipt")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attach Receipt (OCR scanning in future module)", fontSize = 12.sp)
            }
        }

        // Save & Cancel Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val finalAmount = amountString.toDoubleOrNull() ?: 0.0
                    val finalAcc = selectedAccount
                    if (finalAmount > 0.0 && finalAcc != null && merchantName.isNotBlank()) {
                        onSave(
                            finalAmount,
                            selectedType,
                            selectedCategory,
                            merchantName,
                            merchantAlias,
                            selectedPaymentMethod,
                            finalAcc.id,
                            finalAcc.nickname,
                            customTimestamp,
                            notes,
                            tagsInput,
                            isFavorite
                        )
                    }
                },
                enabled = amountString.toDoubleOrNull() ?: 0.0 > 0.0 && selectedAccount != null && merchantName.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_transaction_button")
            ) {
                Text("Save Transaction")
            }
        }
    }

    // Beautiful Category Selector Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showNewCategoryDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, "Create Custom")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Custom Category")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(categories) { cat ->
                            Card(
                                onClick = {
                                    selectedCategory = cat
                                    showCategoryDialog = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCategory.id == cat.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = mapCategoryIcon(cat.name),
                                        contentDescription = cat.name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(cat.name, fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // New Custom Category Dialog
    if (showNewCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Create Custom Category") },
            text = {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            onCreateCategory(catName, "#3B82F6", "category")
                            showNewCategoryDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ----------------------------------------------------------------------------
// 5. TRASH CONTENT VIEW
// ----------------------------------------------------------------------------
@Composable
fun TrashContent(
    trashedTransactions: List<Transaction>,
    onRestore: (Transaction) -> Unit,
    onDeletePermanently: (Transaction) -> Unit,
    onEmptyTrash: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (trashedTransactions.isNotEmpty()) {
            Button(
                onClick = onEmptyTrash,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, "Empty Trash")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Empty Trash / Recycler Bin", fontWeight = FontWeight.Bold)
            }
        }

        if (trashedTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Empty Trash",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Trash is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Deleted transactions will remain here for 30 days before being automatically purged.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(trashedTransactions) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(item.merchantName, fontWeight = FontWeight.Bold)
                                Text("₹${item.amount} [${item.type}]", fontSize = 12.sp)
                            }
                            Row {
                                IconButton(onClick = { onRestore(item) }) {
                                    Icon(Icons.Default.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { onDeletePermanently(item) }) {
                                    Icon(Icons.Default.DeleteForever, "Delete Permanently", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 6. TRANSACTION DETAILS CONTENT
// ----------------------------------------------------------------------------
@Composable
fun TransactionDetailsContent(
    transaction: Transaction,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val typeColor = when (transaction.type) {
            "Income", "Refund" -> Color(0xFF10B981)
            "Expense" -> Color(0xFFEF4444)
            "Borrow", "Lend" -> Color(0xFFF59E0B)
            "Transfer" -> Color(0xFF3B82F6)
            else -> MaterialTheme.colorScheme.primary
        }

        // Large display amount
        Text(
            text = "${if (transaction.type == "Expense" || transaction.type == "Lend") "-" else "+"}₹${String.format(Locale.US, "%.2f", transaction.amount)}",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = typeColor
        )

        Text(
            text = transaction.type.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = typeColor,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Full properties layout card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailRow(label = "Merchant / Payee", value = transaction.merchantName)
                if (transaction.merchantAlias.isNotBlank()) {
                    DetailRow(label = "Merchant Alias", value = transaction.merchantAlias)
                }
                DetailRow(label = "Category", value = transaction.categoryName)
                DetailRow(label = "Payment Method", value = transaction.paymentMethod)
                DetailRow(label = "Bank Account Name", value = transaction.bankAccountName)
                DetailRow(
                    label = "Date & Time",
                    value = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
                )
                if (transaction.notes.isNotBlank()) {
                    DetailRow(label = "Notes", value = transaction.notes)
                }
                if (transaction.tags.isNotBlank()) {
                    DetailRow(label = "Tags", value = transaction.tags)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back to List")
            }

            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Edit, "Edit")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit Details")
            }
        }

        // Placeholder sections for future modules
        Spacer(modifier = Modifier.height(32.dp))
        Text("AI & Extra Actions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { },
                label = { Text("Generate AI Report") },
                leadingIcon = { Icon(Icons.Default.Analytics, "AI") }
            )
            AssistChip(
                onClick = { },
                label = { Text("Export to PDF") },
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, "PDF") }
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
