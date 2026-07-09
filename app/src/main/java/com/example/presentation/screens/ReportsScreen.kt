package com.example.presentation.screens

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.Transaction
import com.example.presentation.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe StateFlows from ViewModel
    val activeTabState = remember { mutableStateOf(0) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedMerchant by viewModel.selectedMerchant.collectAsState()
    val selectedBank by viewModel.selectedBank.collectAsState()
    val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsState()

    // Data lists & states
    val txList by viewModel.filteredTransactions.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()
    val incomeReport by viewModel.incomeReportGroupings.collectAsState()
    val expenseReport by viewModel.expenseReportDetails.collectAsState()
    val statements by viewModel.bankStatements.collectAsState()
    val paymentStatements by viewModel.paymentMethodStatements.collectAsState()
    val goalsReport by viewModel.savingsGoalsReport.collectAsState()
    val borrowLendState by viewModel.borrowLendReport.collectAsState()
    val ledgerRows by viewModel.ledgerItems.collectAsState()
    val cashBookState by viewModel.cashBook.collectAsState()
    val trialBalanceState by viewModel.trialBalance.collectAsState()
    val profitLossList by viewModel.profitAndLoss.collectAsState()

    val profileState by viewModel.userProfile.collectAsState()

    // Dialog state for filters & preview
    var showFilterDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewHtmlContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Tabs List
    val tabs = listOf(
        "Dashboard",
        "Income",
        "Expense",
        "Banks",
        "Payments",
        "Savings",
        "Borrow/Lend",
        "Ledger",
        "Cash Book",
        "Trial Bal",
        "P&L"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reports & Accounting",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("reports_title")
                        )
                        Text(
                            text = "Professional Ledger & Financial Audit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("reports_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.testTag("reports_filter_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Advanced Filters",
                            tint = if (selectedCategory != null || selectedMerchant != null || selectedBank != null || selectedPaymentMethod != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Live Search inside reports
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search transactions, categories, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("reports_search_input")
                )

                // Date Filter Dropdown Shortcut
                var dateDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { dateDropdownExpanded = true },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(52.dp).testTag("date_range_shortcut")
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Date Range",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = filterType.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = dateDropdownExpanded,
                        onDismissRequest = { dateDropdownExpanded = false }
                    ) {
                        ReportFilterType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    viewModel.setFilterType(type)
                                    dateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Quick Active Filters Status Row
            if (selectedCategory != null || selectedMerchant != null || selectedBank != null || selectedPaymentMethod != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Filters:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    selectedCategory?.let {
                        FilterTagChip(label = "Cat: $it") { viewModel.setSelectedCategory(null) }
                    }
                    selectedMerchant?.let {
                        FilterTagChip(label = "Merch: $it") { viewModel.setSelectedMerchant(null) }
                    }
                    selectedBank?.let {
                        FilterTagChip(label = "Bank: $it") { viewModel.setSelectedBank(null) }
                    }
                    selectedPaymentMethod?.let {
                        FilterTagChip(label = "Pay: $it") { viewModel.setSelectedPaymentMethod(null) }
                    }

                    TextButton(
                        onClick = { viewModel.clearAllFilters() },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Professional Scrollable Tabs for 11 Accounting Views
            ScrollableTabRow(
                selectedTabIndex = activeTabState.value,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reports_tab_row")
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTabState.value == index,
                        onClick = { activeTabState.value = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (activeTabState.value == index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("report_tab_$index")
                    )
                }
            }

            // Export Actions Panel Row
            ExportActionsPanel(
                onExportCsv = {
                    isLoading = true
                    try {
                        val csvData = viewModel.generateCSVData()
                        shareFileText(context, csvData, "text/csv", "VaultFlow_Statement.csv")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                },
                onExportJson = {
                    isLoading = true
                    try {
                        val jsonData = viewModel.generateJSONData()
                        shareFileText(context, jsonData, "application/json", "VaultFlow_Statement.json")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                },
                onExportPdf = {
                    val html = viewModel.generateHTMLReport()
                    printHtmlDocument(context, html)
                },
                onPreviewReport = {
                    previewHtmlContent = viewModel.generateHTMLReport()
                    showPreviewDialog = true
                }
            )

            // Dynamic Content Rendering of Active Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    AnimatedContent(
                        targetState = activeTabState.value,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "TabContentAnimation"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> TabDashboardSummary(summary)
                            1 -> TabIncomeReport(incomeReport)
                            2 -> TabExpenseReport(expenseReport)
                            3 -> TabBankStatements(statements)
                            4 -> TabPaymentStatements(paymentStatements)
                            5 -> TabSavingsGoals(goalsReport)
                            6 -> TabBorrowLendReport(borrowLendState)
                            7 -> TabLedgerReport(ledgerRows)
                            8 -> TabCashBookReport(cashBookState)
                            9 -> TabTrialBalanceReport(trialBalanceState)
                            10 -> TabPLSummary(profitLossList)
                        }
                    }
                }
            }
        }
    }

    // Filters selection Dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Advanced Report Filtering", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    item {
                        Text("Filter by Category", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Food", "Salary", "Rent", "Savings", "Utilities", "Shopping", "Entertainment", "Medical").forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { viewModel.setSelectedCategory(if (selectedCategory == cat) null else cat) },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Filter by Bank Account", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("SBI", "HDFC", "ICICI", "Paytm Bank", "Cash").forEach { bank ->
                                FilterChip(
                                    selected = selectedBank == bank,
                                    onClick = { viewModel.setSelectedBank(if (selectedBank == bank) null else bank) },
                                    label = { Text(bank) }
                                )
                            }
                        }
                    }

                    item {
                        Text("Filter by Payment Method", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "PhonePe", "Google Pay", "Paytm", "FamPay", "Debit Card", "Credit Card", "UPI", "Bank Transfer").forEach { pay ->
                                FilterChip(
                                    selected = selectedPaymentMethod == pay,
                                    onClick = { viewModel.setSelectedPaymentMethod(if (selectedPaymentMethod == pay) null else pay) },
                                    label = { Text(pay) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) {
                    Text("Apply Filters")
                }
            }
        )
    }

    // HTML Preview Dialog
    if (showPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Statement Preview", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showPreviewDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Preview")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                    Text(
                        text = "Professional PDF & Print Output Cover Page & Ledger Structure:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("VAULTFLOW", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("AUDITED STATE", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                            Text("Professional Financial Statement", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("Complete Accounting & Performance Audit Report", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(60.dp))
                            
                            Text("PREPARED FOR:", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(profileState?.name ?: "Pranav", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("SUMMARY STATS:", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Net Worth: ₹${String.format("%.2f", summary.netWorth)}", color = Color.White, fontSize = 13.sp)
                            Text("Income Flow: ₹${String.format("%.2f", summary.totalIncome)}", color = Color.White, fontSize = 13.sp)
                            Text("Expense Flow: ₹${String.format("%.2f", summary.totalExpense)}", color = Color.White, fontSize = 13.sp)
                            Text("Net Savings: ₹${String.format("%.2f", summary.netSavings)}", color = Color.White, fontSize = 13.sp)

                            Spacer(modifier = Modifier.height(24.dp))
                            Text("----------------------------------------", color = Color(0xFF334155))
                            Text("Designed with native HTML/CSS vector layouts. Ready for Print or Save as high-resolution vectorized PDF.", color = Color(0xFF64748B), fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        printHtmlDocument(context, previewHtmlContent)
                        showPreviewDialog = false
                    },
                    modifier = Modifier.fillMaxWidth().testTag("preview_print_action")
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Print / Save PDF")
                }
            }
        )
    }
}

@Composable
fun FilterTagChip(
    label: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove Filter",
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
fun ExportActionsPanel(
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onExportPdf: () -> Unit,
    onPreviewReport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPreviewReport,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(42.dp).testTag("action_preview")
            ) {
                Icon(Icons.Default.Preview, contentDescription = "Preview", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preview PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onExportPdf,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(42.dp).testTag("action_print")
            ) {
                Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print / PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onExportCsv,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(42.dp).testTag("action_csv")
            ) {
                Icon(Icons.Default.TableChart, contentDescription = "CSV", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onExportJson,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(42.dp).testTag("action_json")
            ) {
                Icon(Icons.Default.Code, contentDescription = "JSON", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// TAB 0: Dashboard Summary View
@Composable
fun TabDashboardSummary(summary: DashboardSummaryState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("dashboard_report_tab")
    ) {
        // Hero Financial Health & Net Worth
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Estimated Net Worth", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹${String.format("%.2f", summary.netWorth)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (summary.financialHealthScore >= 75) Color(0xFF10B981) else Color(0xFFF59E0B), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Financial Health Score: ${summary.financialHealthScore}/100",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Score Radial progress
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val scoreColor = if (summary.financialHealthScore >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = scoreColor.copy(alpha = 0.15f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx())
                            )
                            drawArc(
                                color = scoreColor,
                                startAngle = -90f,
                                sweepAngle = (summary.financialHealthScore / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx())
                            )
                        }
                        Text(
                            text = "${summary.financialHealthScore}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = scoreColor
                        )
                    }
                }
            }
        }

        // Summary Statistics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SummarySquareCard(
                        title = "Total Income",
                        value = "₹${String.format("%.2f", summary.totalIncome)}",
                        icon = Icons.Default.TrendingUp,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    SummarySquareCard(
                        title = "Total Expense",
                        value = "₹${String.format("%.2f", summary.totalExpense)}",
                        icon = Icons.Default.TrendingDown,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SummarySquareCard(
                        title = "Net Savings",
                        value = "₹${String.format("%.2f", summary.netSavings)}",
                        icon = Icons.Default.Savings,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    SummarySquareCard(
                        title = "Private Vault",
                        value = "₹${String.format("%.2f", summary.vaultBalance)}",
                        icon = Icons.Default.Lock,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SummarySquareCard(
                        title = "Outstanding Lend",
                        value = "₹${String.format("%.2f", summary.outstandingLend)}",
                        icon = Icons.Default.Handshake,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    SummarySquareCard(
                        title = "Outstanding Borrow",
                        value = "₹${String.format("%.2f", summary.outstandingBorrow)}",
                        icon = Icons.Default.Handshake,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Current Bank Balances Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Current Bank & Wallet Balances",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (summary.bankBalances.isEmpty()) {
                    Text("No bank accounts registered. Add accounts to build dynamic balances.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    summary.bankBalances.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(Color(android.graphics.Color.parseColor(item.colorHex)), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(item.bankName, fontWeight = FontWeight.Bold)
                                }
                                Text("₹${String.format("%.2f", item.balance)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummarySquareCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// TAB 1: Income Report View
@Composable
fun TabIncomeReport(incomeReport: Map<String, Map<String, Double>>) {
    val items = incomeReport["Category"] ?: emptyMap()
    val totalIncome = items.values.sum()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("income_report_tab")
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Calculated Receipts", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "₹${String.format("%.2f", totalIncome)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        item {
            Text("Category-wise Receipts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (items.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No recorded incomes matching current scope.")
                }
            }
        } else {
            items(items.toList().sortedByDescending { it.second }) { (category, amount) ->
                RowItemProgress(
                    label = category,
                    amount = amount,
                    percentage = if (totalIncome > 0) (amount / totalIncome).toFloat() else 0f,
                    barColor = Color(0xFF10B981)
                )
            }
        }
    }
}

// TAB 2: Expense Report View
@Composable
fun TabExpenseReport(expenseReport: Map<String, Any>) {
    val highest = expenseReport["highest"] as? Transaction
    val lowest = expenseReport["lowest"] as? Transaction
    val byCategory = expenseReport["byCategory"] as? Map<String, Double> ?: emptyMap()
    val totalExpense = byCategory.values.sum()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("expense_report_tab")
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Outflows & Spending", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "₹${String.format("%.2f", totalExpense)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }

        // Extremes Block
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Highest Single Spend", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (highest != null) "₹${highest.amount}" else "N/A",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = highest?.merchantName?.ifBlank { highest.categoryName } ?: "None",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Lowest Single Spend", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lowest != null) "₹${lowest.amount}" else "N/A",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = lowest?.merchantName?.ifBlank { lowest.categoryName } ?: "None",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Text("Category Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (byCategory.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No recorded spends matching current filters.")
                }
            }
        } else {
            items(byCategory.toList().sortedByDescending { it.second }) { (category, amount) ->
                RowItemProgress(
                    label = category,
                    amount = amount,
                    percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f,
                    barColor = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
fun RowItemProgress(
    label: String,
    amount: Double,
    percentage: Float,
    barColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${String.format("%.2f", amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${String.format("%.1f", percentage * 100)}%)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = percentage.coerceIn(0f, 1f))
                    .background(barColor, RoundedCornerShape(4.dp))
            )
        }
    }
}

// TAB 3: Bank Account Statement
@Composable
fun TabBankStatements(statements: List<BankStatementItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("bank_statement_tab")
    ) {
        if (statements.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No accounts statement available.")
                }
            }
        } else {
            items(statements) { statement ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(statement.bankName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Default.AccountBalance, contentDescription = "Bank", tint = MaterialTheme.colorScheme.primary)
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Opening Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.2f", statement.openingBalance)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Closing Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.2f", statement.closingBalance)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Credits (+)", fontSize = 11.sp, color = Color(0xFF10B981))
                                Text("₹${String.format("%.2f", statement.credits)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
                            }
                            Column {
                                Text("Debits (-)", fontSize = 11.sp, color = Color(0xFFEF4444))
                                Text("₹${String.format("%.2f", statement.debits)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFEF4444))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Transfers", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("₹${String.format("%.2f", statement.transfers)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// TAB 4: Payment Method Statement
@Composable
fun TabPaymentStatements(statements: List<PaymentMethodStatementItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("payment_statement_tab")
    ) {
        if (statements.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions logged for standard payment methods.")
                }
            }
        } else {
            items(statements) { statement ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(statement.methodName, fontWeight = FontWeight.Bold)
                            }
                            Text("${statement.transactionCount} entries logged", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Net Flow: ₹${String.format("%.2f", statement.netFlow)}",
                                fontWeight = FontWeight.Bold,
                                color = if (statement.netFlow >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            Text(
                                text = "In: ₹${String.format("%.1f", statement.totalCredits)} | Out: ₹${String.format("%.1f", statement.totalDebits)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// TAB 5: Savings Goals Report
@Composable
fun TabSavingsGoals(goals: List<SavingsGoalReportItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("savings_goals_report_tab")
    ) {
        if (goals.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No active savings goals found.")
                }
            }
        } else {
            items(goals) { goal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${String.format("%.1f", goal.completionPercentage * 100)}% Match", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { goal.completionPercentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Target Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.1f", goal.target)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Saved Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.1f", goal.saved)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Outstanding", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.1f", goal.remaining)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEF4444))
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Est. Dynamic Deposits: ₹${String.format("%.1f", goal.deposits)}", fontSize = 11.sp, color = Color(0xFF10B981))
                            Text("Est. Dynamic Withdrawals: ₹${String.format("%.1f", goal.withdrawals)}", fontSize = 11.sp, color = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}

// TAB 6: Borrow / Lend Report
@Composable
fun TabBorrowLendReport(state: BorrowLendReportState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("borrow_lend_report_tab")
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Credit & Liability Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Money Borrowed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%.2f", state.moneyBorrowed)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFEF4444))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Money Lent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%.2f", state.moneyLent)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF10B981))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Outstanding Borrow", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%.2f", state.outstandingBorrow)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFEF4444))
                        }
                        Column {
                            Text("Outstanding Lend", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%.2f", state.outstandingLend)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF10B981))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Overdue Items", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%.2f", state.overdue)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }
    }
}

// TAB 7: Ledger View
@Composable
fun TabLedgerReport(rows: List<LedgerItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("ledger_report_tab")
    ) {
        if (rows.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions recorded in ledger context.")
                }
            }
        } else {
            items(rows) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.dateLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(item.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.bankAccount} (${item.paymentMethod})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (item.isCredit) "+" else "-"}₹${String.format("%.2f", item.amount)}",
                                fontWeight = FontWeight.Bold,
                                color = if (item.isCredit) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Bal: ₹${String.format("%.1f", item.runningBalance)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// TAB 8: Cash Book View
@Composable
fun TabCashBookReport(state: CashBookState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("cash_book_tab")
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Cash Book Ledger Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Receipts", fontSize = 11.sp, color = Color(0xFF10B981))
                            Text("₹${String.format("%.2f", state.receipts)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF10B981))
                        }
                        Column {
                            Text("Total Payments", fontSize = 11.sp, color = Color(0xFFEF4444))
                            Text("₹${String.format("%.2f", state.payments)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFEF4444))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Closing Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text("₹${String.format("%.2f", state.closingBalance)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            Text("Cash Receipts & Payments Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (state.transactions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No cash transactions logged.")
                }
            }
        } else {
            items(state.transactions) { tx ->
                val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.timestamp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dateLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tx.merchantName.ifBlank { tx.categoryName }, fontWeight = FontWeight.Bold)
                            Text(tx.categoryName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${if (tx.type == "Income") "+" else "-"}₹${tx.amount}",
                            fontWeight = FontWeight.Bold,
                            color = if (tx.type == "Income") Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

// TAB 9: Trial Balance View
@Composable
fun TabTrialBalanceReport(state: TrialBalanceState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("trial_balance_tab")
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isVerified) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isVerified) "Trial Balance Reconciled" else "Trial Balance Out of Balance",
                            fontWeight = FontWeight.Bold,
                            color = if (state.isVerified) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Text(
                            text = if (state.isVerified) "Double-entry rules verified. Debit and Credit totals match exactly." else "Verify capital accounts or adjustment entries to clear variance.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = if (state.isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (state.isVerified) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }

        // Ledger Columns Headers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Account / Ledger Pool", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.8f))
                Text("Debit", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = Alignment.End.let { TextAlign.Right })
                Text("Credit", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = Alignment.End.let { TextAlign.Right })
            }
        }

        if (state.items.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No records found in current accounting context.")
                }
            }
        } else {
            items(state.items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.accountName, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        
                        Text(
                            text = if (item.debit > 0) "₹${String.format("%.1f", item.debit)}" else "-",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = if (item.credit > 0) "₹${String.format("%.1f", item.credit)}" else "-",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Right,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        // Matched Totals Bottom
        item {
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TOTALS", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1.8f))
                Text("₹${String.format("%.1f", state.totalDebit)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.primary)
                Text("₹${String.format("%.1f", state.totalCredit)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Right, color = Color(0xFF10B981))
            }
        }
    }
}

// TAB 10: Profit & Loss summary
@Composable
fun TabPLSummary(profitLossList: List<PLItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxSize().testTag("pl_summary_tab")
    ) {
        item {
            Text("Periodical Profits & Savings Outlines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (profitLossList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No profit & loss projections available.")
                }
            }
        } else {
            items(profitLossList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.periodLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Savings Rate: ${if (item.income > 0) String.format("%.0f", (item.netSavings / item.income) * 100) else "0"}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Revenues / Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.1f", item.income)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF10B981))
                            }
                            Column {
                                Text("Outflows / Expense", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.1f", item.expense)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEF4444))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Savings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.1f", item.netSavings)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (item.netSavings >= 0) Color(0xFF10B981) else Color(0xFFEF4444))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Android Share Action Helper
private fun shareFileText(context: Context, text: String, mimeType: String, fileName: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share $fileName via"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Android Webview Native Printing Helper (renders HTML report in invisible webview and triggers System Print Manager)
private fun printHtmlDocument(context: Context, htmlContent: String) {
    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val documentTitle = "VaultFlow_Accounting_Statement_" + System.currentTimeMillis()
                val printAdapter = webView.createPrintDocumentAdapter(documentTitle)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
                printManager.print(documentTitle, printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
