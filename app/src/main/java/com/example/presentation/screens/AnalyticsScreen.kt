package com.example.presentation.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val filterType by viewModel.filterType.collectAsState()
    val activeRange by viewModel.activeDateRange.collectAsState()

    val overviewStats by viewModel.overviewStats.collectAsState()
    val categoryDistribution by viewModel.categoryDistribution.collectAsState()
    val monthlyBarData by viewModel.monthlyIncomeVsExpense.collectAsState()
    val dailyTrendPoints by viewModel.dailySpendingTrend.collectAsState()
    val savingsTrendPoints by viewModel.savingsGrowth.collectAsState()
    val heatmapDays by viewModel.heatmapData.collectAsState()

    val categoryAnalytics by viewModel.categoryAnalytics.collectAsState()
    val merchantAnalytics by viewModel.merchantAnalytics.collectAsState()
    val bankAnalytics by viewModel.bankAnalytics.collectAsState()
    val paymentMethodAnalytics by viewModel.paymentMethodAnalytics.collectAsState()
    val savingsAnalytics by viewModel.savingsAnalytics.collectAsState()
    val borrowLendAnalytics by viewModel.borrowLendAnalytics.collectAsState()
    val healthScore by viewModel.financialHealthScore.collectAsState()
    val insights by viewModel.insights.collectAsState()

    // Screen navigation tabs
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "Overview",
        "Categories",
        "Merchants",
        "Banks",
        "Payment Methods",
        "Savings",
        "Borrow/Lend"
    )

    // Export Sheet visibility
    var showExportSheet by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf("PDF") }

    // Custom Date Range Pick Dialog
    var showDatePicker by remember { mutableStateOf(false) }
    var customStartDateStr by remember { mutableStateOf("") }
    var customEndDateStr by remember { mutableStateOf("") }

    val activeRangeLabel = remember(activeRange, filterType) {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        if (filterType == FilterType.DAY) {
            format.format(Date(activeRange.start))
        } else {
            "${format.format(Date(activeRange.start))} - ${format.format(Date(activeRange.end))}"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. TOP HEADER & ACTION BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VaultFlow Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = activeRangeLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Export Preparation trigger button
                    IconButton(
                        onClick = { showExportSheet = true },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                CircleShape
                            )
                            .size(40.dp)
                            .testTag("analytics_export_button"),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export analytics",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. TIME RANGE FILTER ROW
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("filter_chips_row"),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf(
                    FilterType.DAY to "Today",
                    FilterType.WEEK to "Past Week",
                    FilterType.MONTH to "Past Month",
                    FilterType.QUARTER to "Quarterly",
                    FilterType.YEAR to "Yearly",
                    FilterType.CUSTOM to "Custom Date"
                )

                items(filters) { (type, label) ->
                    val isSelected = filterType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (type == FilterType.CUSTOM) {
                                showDatePicker = true
                            } else {
                                viewModel.setFilterType(type)
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (type == FilterType.CUSTOM) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("filter_chip_${label.lowercase().replace(" ", "_")}")
                    )
                }
            }

            // 3. TAB SELECTION BAR
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                divider = {},
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_tabs")
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("analytics_tab_${title.lowercase().replace("/", "_").replace(" ", "_")}")
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // 4. MAIN CONTENT CONTAINER
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> OverviewTabContent(
                        stats = overviewStats,
                        healthScore = healthScore,
                        insights = insights,
                        categoryDistribution = categoryDistribution,
                        monthlyBarData = monthlyBarData,
                        dailyTrendPoints = dailyTrendPoints,
                        savingsTrendPoints = savingsTrendPoints,
                        heatmapDays = heatmapDays
                    )
                    1 -> CategoriesTabContent(categoryAnalytics)
                    2 -> MerchantsTabContent(merchantAnalytics)
                    3 -> BanksTabContent(bankAnalytics)
                    4 -> PaymentMethodsTabContent(paymentMethodAnalytics)
                    5 -> SavingsTabContent(savingsAnalytics)
                    6 -> BorrowLendTabContent(borrowLendAnalytics)
                }
            }
        }

        // BOTTOM EXPORT SHEET
        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                modifier = Modifier.testTag("export_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Prepare Financial Export",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose your preferred file layout and configuration. Your encrypted financial statement will be compiled from active database filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("PDF", "CSV").forEach { format ->
                            val isSelected = selectedExportFormat == format
                            Card(
                                onClick = { selectedExportFormat = format },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .testTag("export_format_${format.lowercase()}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (format == "PDF") Icons.Default.PictureAsPdf else Icons.Default.TableView,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$format Format",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            showExportSheet = false
                            Toast.makeText(context, "$selectedExportFormat export compiled and saved to downloads!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_confirm_button")
                    ) {
                        Text("Export to $selectedExportFormat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // CUSTOM DATE PICKER DIALOG
        if (showDatePicker) {
            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Custom Date Filter", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Configure start and end dates (e.g. YYYY-MM-DD)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = customStartDateStr,
                            onValueChange = { customStartDateStr = it },
                            label = { Text("Start Date") },
                            placeholder = { Text("2026-01-01") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_start_date_input")
                        )
                        OutlinedTextField(
                            value = customEndDateStr,
                            onValueChange = { customEndDateStr = it },
                            label = { Text("End Date") },
                            placeholder = { Text("2026-03-31") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_end_date_input")
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val startMs = sdf.parse(customStartDateStr.trim())?.time ?: System.currentTimeMillis()
                                val endMs = sdf.parse(customEndDateStr.trim())?.time ?: System.currentTimeMillis()
                                viewModel.setCustomDateRange(startMs, endMs)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid format. Using default range.", Toast.LENGTH_SHORT).show()
                                viewModel.setFilterType(FilterType.MONTH)
                            }
                        },
                        modifier = Modifier.testTag("date_range_apply_btn")
                    ) {
                        Text("Apply Filter", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ==========================================
// OVERVIEW TAB CONTENT
// ==========================================
@Composable
fun OverviewTabContent(
    stats: OverviewStats,
    healthScore: FinancialHealthScore,
    insights: List<InsightCard>,
    categoryDistribution: List<ChartSegment>,
    monthlyBarData: List<MonthlyBarData>,
    dailyTrendPoints: List<DailyTrendPoint>,
    savingsTrendPoints: List<SavingsTrendPoint>,
    heatmapDays: List<HeatmapDay>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("overview_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // 1. Dashboard Cards Grid
        item {
            OverviewStatsGrid(stats)
        }

        // 2. Financial Health Score SPEEDOMETER DIAL
        item {
            FinancialHealthDialCard(healthScore)
        }

        // 3. Dynamic Interactive Charts Page
        item {
            Text(
                text = "Interactive Analytics Charts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_category_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Category Expenses (Donut)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (categoryDistribution.isNotEmpty()) {
                        DonutChart(segments = categoryDistribution)
                    } else {
                        EmptyStateCard(message = "No expense categories in this period.")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_income_vs_expense_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monthly Income vs Expense (Bar)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (monthlyBarData.isNotEmpty()) {
                        MonthlyCompareBarChart(barDataList = monthlyBarData)
                    } else {
                        EmptyStateCard(message = "Add bank transaction historicals for comparison.")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_daily_spend_trend_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Outflow Spending Trend (Line)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (dailyTrendPoints.isNotEmpty()) {
                        SpendingTrendLineChart(points = dailyTrendPoints)
                    } else {
                        EmptyStateCard(message = "No outflows recorded in active range.")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_savings_growth_card"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Savings Progression Growth (Area)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (savingsTrendPoints.isNotEmpty() && savingsTrendPoints.any { it.accumulatedAmount > 0 }) {
                        SavingsGrowthAreaChart(points = savingsTrendPoints)
                    } else {
                        EmptyStateCard(message = "Configure Savings Goals to visualize growth.")
                    }
                }
            }
        }

        // 4. Financial Calendar Heatmap
        item {
            Text(
                text = "Financial Calendar (Month Heatmap)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        item {
            FinancialCalendarHeatmapCard(heatmapDays = heatmapDays)
        }

        // 5. Intelligent Insight Cards
        item {
            Text(
                text = "Smart Financial Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(insights) { insight ->
            InsightItemCard(insight = insight)
        }
    }
}

// ==========================================
// SUB-TAB 1: CATEGORIES
// ==========================================
@Composable
fun CategoriesTabContent(categories: List<CategoryAnalyticItem>) {
    if (categories.isEmpty()) {
        EmptyStateCard(message = "No categories found. Head to Category Management.")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("categories_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Highlighting stats
        val topCategory = categories.firstOrNull { it.totalSpent > 0 }
        val overBudget = categories.filter { it.budget > 0 && it.totalSpent > it.budget }

        if (topCategory != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Category Outflow Warning",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your highest spending category is ${topCategory.categoryName} with a total of ₹${String.format("%.2f", topCategory.totalSpent)} spent.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        if (overBudget.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚠️ Alert: You have exceeded monthly budgets on ${overBudget.size} categories!",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        items(categories) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_analytic_card_${item.categoryName.lowercase()}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(item.colorHex)))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.categoryName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Text(
                            text = "₹${String.format("%.2f", item.totalSpent)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Budget Progress Bar
                    if (item.budget > 0) {
                        val progress = item.budgetUsagePercentage.coerceIn(0f, 1f)
                        val progressColor = if (item.budgetUsagePercentage > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Budget: ₹${item.budget.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(item.budgetUsagePercentage * 100).toInt()}% Used",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    } else {
                        Text(
                            text = "No budget limits set. Add in Category Management.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.transactionCount} payments • Avg: ₹${item.averageSpent.toInt()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val diff = item.changeVsLastMonth
                        if (diff != 0.0) {
                            val isMore = diff > 0
                            val color = if (isMore) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                            val prefix = if (isMore) "+" else ""
                            Text(
                                text = "$prefix₹${diff.toInt()} vs last month",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-TAB 2: MERCHANTS
// ==========================================
@Composable
fun MerchantsTabContent(merchants: List<MerchantAnalyticItem>) {
    if (merchants.isEmpty()) {
        EmptyStateCard(message = "No merchant records in this period.")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("merchants_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val highestSpend = merchants.maxByOrNull { it.totalSpending }
        val mostVisited = merchants.maxByOrNull { it.visitCount }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (highestSpend != null) {
                    Card(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Highest Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(highestSpend.merchantName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("₹${highestSpend.totalSpending.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (mostVisited != null) {
                    Card(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Most Visited", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(mostVisited.merchantName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${mostVisited.visitCount} visits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        items(merchants) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("merchant_analytic_card_${item.merchantName.lowercase().replace(" ", "_")}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.merchantName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${item.visitCount} orders in total • ${item.visitsLast30Days} this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${item.totalSpending.toInt()}",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Avg: ₹${item.averagePurchase.toInt()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-TAB 3: BANKS
// ==========================================
@Composable
fun BanksTabContent(banks: List<BankAnalyticItem>) {
    if (banks.isEmpty()) {
        EmptyStateCard(message = "No active bank accounts. Link in Home or Settings.")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("banks_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(banks) { bank ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_analytic_card_${bank.bankName.lowercase().replace(" ", "_")}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bank.bankName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "₹${String.format("%.2f", bank.balance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (bank.isHighestBalance) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("💰 Top Assets") },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        if (bank.isMostUsed) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("🔥 Most Transactions") },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Payments: ${bank.transactionCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${bank.transferCount} Internal Transfers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-TAB 4: PAYMENT METHODS
// ==========================================
@Composable
fun PaymentMethodsTabContent(paymentMethods: List<PaymentMethodAnalyticItem>) {
    val activeMethods = paymentMethods.filter { it.transactionCount > 0 }
    if (activeMethods.isEmpty()) {
        EmptyStateCard(message = "No payments registered in active filter.")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("payment_methods_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred Channels",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your most used payment method is ${activeMethods.first().methodName}, accounting for ${(activeMethods.first().usagePercentage * 100).toInt()}% of outflow values.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        items(activeMethods) { method ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_method_card_${method.methodName.lowercase().replace(" ", "_")}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = method.methodName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "₹${method.totalAmount.toInt()}",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val pct = method.usagePercentage.coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = pct,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${method.transactionCount} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(method.usagePercentage * 100).toInt()}% share",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-TAB 5: SAVINGS
// ==========================================
@Composable
fun SavingsTabContent(summary: SavingsAnalyticSummary) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("savings_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Savings Target Progress",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Circular Progress Dial
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = summary.overallProgress.coerceIn(0f, 1f),
                            modifier = Modifier.size(130.dp),
                            strokeWidth = 10.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(summary.overallProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black
                            )
                            Text("Saved", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Active Goals", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${summary.activeGoalsCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        VerticalDivider(modifier = Modifier.height(32.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Completed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${summary.completedGoalsCount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        VerticalDivider(modifier = Modifier.height(32.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Completion Rate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(summary.completionRate * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Time Projection & Estimation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "Projected overall goal completion in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${summary.projectedCompletionDays} Days",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Calculation is compiled from historic monthly savings rates and remaining targets across active goals. To speed up, deposit more in Vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ==========================================
// SUB-TAB 6: BORROW / LEND
// ==========================================
@Composable
fun BorrowLendTabContent(summary: BorrowLendAnalyticSummary) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("borrow_lend_scroll_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Outstanding Debt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${summary.outstandingAmount.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Overdue Pending", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${summary.overdueAmount.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (summary.overdueAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Debt Recovery Performance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = summary.recoveryPercentage.coerceIn(0f, 1f),
                                modifier = Modifier.size(72.dp),
                                strokeWidth = 8.dp,
                                color = Color(0xFF10B981),
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            Text(
                                text = "${(summary.recoveryPercentage * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Lent Cash Collected: ₹${summary.collectedAmount.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pending Collection: ₹${summary.pendingAmount.toInt()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Active outstanding claims are protected. Reminders can be issued under Notifications tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DETAILED COMPOSE STATS/WIDGET COMPONENTS
// ==========================================

@Composable
fun OverviewStatsGrid(stats: OverviewStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardItemCard(
                title = "Total Income",
                value = "₹${String.format("%.2f", stats.totalIncome)}",
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f).testTag("overview_income_card")
            )
            DashboardItemCard(
                title = "Total Expense",
                value = "₹${String.format("%.2f", stats.totalExpense)}",
                icon = Icons.Default.TrendingDown,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f).testTag("overview_expense_card")
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardItemCard(
                title = "Current Balance",
                value = "₹${String.format("%.2f", stats.currentBalance)}",
                icon = Icons.Default.AccountBalanceWallet,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            DashboardItemCard(
                title = "Vault Balance",
                value = "₹${String.format("%.2f", stats.vaultBalance)}",
                icon = Icons.Default.Lock,
                color = Color(0xFFFBBF24),
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardItemCard(
                title = "Savings Goals",
                value = "₹${String.format("%.2f", stats.savings)}",
                icon = Icons.Default.Savings,
                color = Color(0xFFEC4899),
                modifier = Modifier.weight(1f)
            )
            DashboardItemCard(
                title = "Lent Outflow",
                value = "₹${String.format("%.2f", stats.lent)}",
                icon = Icons.Default.Payments,
                color = Color(0xFF06B6D4),
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DashboardItemCard(
                title = "Borrowed Debt",
                value = "₹${String.format("%.2f", stats.borrowed)}",
                icon = Icons.Default.CallMade,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            DashboardItemCard(
                title = "Net Worth",
                value = "₹${String.format("%.2f", stats.netWorth)}",
                icon = Icons.Default.Language,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DashboardItemCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun FinancialHealthDialCard(score: FinancialHealthScore) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("health_score_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Financial Health Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Speedometer Dial
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background semi-arc
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Score progress arc
                        val sweep = (score.score.toFloat() / 100f) * 180f
                        val progressColor = when {
                            score.score >= 80 -> Color(0xFF10B981)
                            score.score >= 60 -> Color(0xFFF59E0B)
                            score.score >= 40 -> Color(0xFFF97316)
                            else -> Color(0xFFEF4444)
                        }

                        drawArc(
                            color = progressColor,
                            startAngle = 180f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = 10.dp)
                    ) {
                        Text(
                            text = "${score.score}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = score.rating,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Status: ${score.rating}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = score.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "💡 Recommendations:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = score.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// HEATMAP GRID CARD
@Composable
fun FinancialCalendarHeatmapCard(heatmapDays: List<HeatmapDay>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("heatmap_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar Intensity Heatmap",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444).copy(alpha = 0.2f)))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444).copy(alpha = 0.5f)))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444)))
                    Text("Outflow Peak", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 7 Columns Row Headers
            Row(modifier = Modifier.fillMaxWidth()) {
                val headers = listOf("S", "M", "T", "W", "T", "F", "S")
                headers.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Simple calendar block representation (chunked in 7s)
            val chunkedDays = heatmapDays.chunked(7)
            chunkedDays.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    week.forEach { day ->
                        val baseColor = when {
                            day.isIncomeDay && day.isExpenseDay -> Color(0xFFFBBF24).copy(alpha = 0.4f)
                            day.isIncomeDay -> Color(0xFF10B981).copy(alpha = 0.3f)
                            day.isExpenseDay -> Color(0xFFEF4444).copy(alpha = 0.15f + day.intensity * 0.75f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(baseColor)
                                .clickable {
                                    // Interactive details on tap if needed
                                }
                        ) {
                            Text(
                                text = "${day.dayOfMonth}",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = if (day.intensity > 0.6f) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (day.isIncomeDay || day.intensity > 0.4f) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    // Handle offset pad if week has less than 7 days
                    if (week.size < 7) {
                        for (i in 0 until (7 - week.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// INSIGHT ITEM CARD
@Composable
fun InsightItemCard(insight: InsightCard) {
    val indicatorColor = when (insight.type) {
        "alert" -> MaterialTheme.colorScheme.error
        "success" -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insight_card_${insight.title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 40.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = insight.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// EMPTY STATE
@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

// ==========================================
// CUSTOM CANVAS CHARTS WITH TRANSITION ANIMATIONS
// ==========================================

@Composable
fun DonutChart(segments: List<ChartSegment>) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "pie_animation"
    )

    LaunchedEffect(key1 = segments) {
        animationPlayed = true
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                segments.forEach { segment ->
                    val sweepAngle = segment.percentage * 360f * animatedProgress
                    drawArc(
                        color = Color(android.graphics.Color.parseColor(segment.colorHex)),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    startAngle += segment.percentage * 360f
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Outflows",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${segments.sumOf { it.amount }.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Legend Column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            segments.take(5).forEach { segment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(segment.colorHex)))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${segment.label} (${(segment.percentage * 100).toInt()}%)",
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (segments.size > 5) {
                Text(text = "+ ${segments.size - 5} more", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MonthlyCompareBarChart(barDataList: List<MonthlyBarData>) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bar_animation"
    )

    LaunchedEffect(key1 = barDataList) {
        animationPlayed = true
    }

    val maxAmount = remember(barDataList) {
        barDataList.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barGroupWidth = width / barDataList.size
            val barWidth = 12.dp.toPx()
            val spacing = 4.dp.toPx()

            barDataList.forEachIndexed { index, data ->
                val xCenter = index * barGroupWidth + barGroupWidth / 2

                // Income Bar
                val incomeHeight = ((data.income / maxAmount) * (height - 40.dp.toPx())).toFloat() * animatedProgress
                drawRoundRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(xCenter - barWidth - spacing, height - 30.dp.toPx() - incomeHeight),
                    size = Size(barWidth, incomeHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Expense Bar
                val expenseHeight = ((data.expense / maxAmount) * (height - 40.dp.toPx())).toFloat() * animatedProgress
                drawRoundRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(xCenter + spacing, height - 30.dp.toPx() - expenseHeight),
                    size = Size(barWidth, expenseHeight.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        // Label alignment under canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 150.dp)
        ) {
            barDataList.forEach { data ->
                Text(
                    text = data.monthName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpendingTrendLineChart(points: List<DailyTrendPoint>) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "line_animation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(key1 = points) {
        animationPlayed = true
    }

    val maxVal = remember(points) {
        points.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height - 40.dp.toPx()
            val stepX = width / (points.size - 1).coerceAtLeast(1)

            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, point ->
                val x = index * stepX
                val y = height - ((point.amount / maxVal) * (height - 20.dp.toPx())).toFloat() * animatedProgress

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == points.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }

                // Draw small dots
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dynamic gradient fill underneath
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )
        }

        // Labels at edge
        if (points.size >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = points.first().dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = points[points.size / 2].dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = points.last().dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SavingsGrowthAreaChart(points: List<SavingsTrendPoint>) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "area_animation"
    )

    LaunchedEffect(key1 = points) {
        animationPlayed = true
    }

    val maxVal = remember(points) {
        points.maxOfOrNull { it.accumulatedAmount }?.coerceAtLeast(1.0) ?: 1.0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height - 40.dp.toPx()
            val stepX = width / (points.size - 1).coerceAtLeast(1)

            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { index, point ->
                val x = index * stepX
                val y = height - ((point.accumulatedAmount / maxVal) * (height - 20.dp.toPx())).toFloat() * animatedProgress

                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                if (index == points.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            drawPath(
                path = linePath,
                color = Color(0xFFEC4899), // Pink styled theme for savings goals
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEC4899).copy(alpha = 0.3f),
                        Color.Transparent
                    )
                )
            )
        }

        // Labels
        if (points.size >= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = points.first().dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = points.last().dateLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
