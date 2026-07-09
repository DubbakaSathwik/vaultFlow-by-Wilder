package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.screens.dashboard.*
import com.example.presentation.viewmodel.HomeViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onOcrImportClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onMerchantsClick: () -> Unit = {},
    onSavingsGoalsClick: () -> Unit = {},
    onVaultClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val isFabExpanded by viewModel.isFabExpanded.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Determine current time-based greeting
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Dashboard Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_scroll_list"),
            contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP APP BAR BLOCK (Greeting + Profile)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("dashboard_top_bar"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pranav",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Circular Premium Avatar Button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                                .clickable { viewModel.setBottomSheetVisible(true) }
                                .testTag("profile_avatar_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // 2. FLOATING SEARCH BAR
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Search transactions, bills, goals...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("dashboard_search_bar")
                )
            }

            // 3. BALANCE CARD CAROUSEL (Section 1)
            item {
                BalanceCardSection(
                    state = state,
                    onAccountChanged = { viewModel.setAccountIndex(it) }
                )
            }

            // 4. QUICK ACTION BUTTONS (Section 2)
            item {
                QuickActionsSection(
                    onActionClicked = { action ->
                        when (action) {
                            "import_ss" -> onOcrImportClick()
                            "categories" -> onCategoriesClick()
                            "merchants" -> onMerchantsClick()
                            "vault" -> onVaultClick()
                            "add_goal" -> onSavingsGoalsClick()
                            else -> viewModel.triggerQuickAction(action)
                        }
                    }
                )
            }

            // 5. MONTHLY BUDGET CARD (Section 3)
            item {
                MonthlyBudgetSection(
                    budget = state.monthlyBudget,
                    spent = state.monthlyBudgetSpent
                )
            }

            // 6. TODAY'S SPENDING LINE CHART (Section 4)
            item {
                TodaySpendingChartSection()
            }

            // 7. CATEGORY SPENDING PIE CHART (Section 5)
            item {
                CategorySpendingChartSection(
                    onCategoryClick = { category ->
                        // Show simple interactive snackbar
                    }
                )
            }

            // 8. PAYMENT METHOD ANALYSIS (Section 6)
            item {
                PaymentMethodAnalysisSection()
            }

            // 9. SAVINGS GOAL CARD (Section 7)
            item {
                SavingsGoalSection(
                    title = state.savingsGoalTitle,
                    target = state.savingsGoalTarget,
                    saved = state.savingsGoalSaved,
                    daysLeft = state.savingsGoalDaysLeft,
                    modifier = Modifier.clickable { onSavingsGoalsClick() }
                )
            }

            // 10. BORROW & LEND SUMMARY (Section 8)
            item {
                BorrowLendSummarySection(
                    toPay = state.moneyToPay,
                    owed = state.moneyOwed
                )
            }

            // 11. UPCOMING PAYMENTS TIMELINE (Section 9)
            item {
                UpcomingPaymentsSection(
                    payments = state.upcomingPayments
                )
            }

            // 12. DAILY BUDGET CARD (Section 10)
            item {
                DailyBudgetSection(
                    dailyLimit = state.dailySafeSpending,
                    dailySpent = state.dailySpent
                )
            }

            // 13. AI FINANCIAL ASSISTANT CARD (Section 11)
            item {
                AiAssistantSection(
                    tips = state.aiTips,
                    currentIndex = state.currentAiTipIndex,
                    onNextTip = { viewModel.addDummyItem("next_tip") }
                )
            }

            // 14. RECENT TRANSACTIONS (Section 12)
            item {
                RecentTransactionsSection(
                    transactions = state.recentTransactions,
                    onTransactionClick = { /* Clicked */ }
                )
            }

            // 15. FINANCIAL HEALTH SCORE (Section 13)
            item {
                FinancialHealthScoreSection(state = state)
            }

            // 16. ACHIEVEMENTS (Section 14)
            item {
                AchievementsSection(achievements = state.achievements)
            }

            // 17. SMART ALERTS (Section 15)
            item {
                SmartAlertsSection(alerts = state.alerts)
            }

            // 18. MONTHLY COMPARISON (Section 16)
            item {
                MonthlyComparisonSection()
            }

            // 19. EXPENSE HEATMAP (Section 17)
            item {
                ExpenseHeatmapSection()
            }

            // 20. FINANCE INSIGHTS STATS (Section 18)
            item {
                FinanceInsightsSection()
            }

            // 21. HOME WIDGET PLACEHOLDER (Do not implement, just create a placeholder)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_widget_placeholder_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = "Widget icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Home Widgets",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enable custom Home Widgets to track balances directly from your home screen.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 22. INTERACTIVE BOTTOM SHEET (My QR Code, Balance Manager, Profile & Settings)
        if (isBottomSheetVisible) {
            // Dim background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.setBottomSheetVisible(false) }
            )

            // Actual sliding Bottom Sheet
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .testTag("premium_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "VaultFlow Wallet Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR CODE RENDERER (drawn dynamically)
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val rows = 10
                            val cols = 10
                            val cellW = size.width / cols
                            val cellH = size.height / rows

                            // Draw a simulated QR grid pattern beautifully
                            val seededQr = listOf(
                                listOf(1, 1, 1, 1, 0, 1, 1, 1, 1, 1),
                                listOf(1, 0, 0, 1, 0, 0, 1, 0, 0, 1),
                                listOf(1, 0, 0, 1, 1, 0, 1, 0, 0, 1),
                                listOf(1, 1, 1, 1, 0, 1, 1, 1, 1, 1),
                                listOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 0),
                                listOf(1, 1, 0, 1, 0, 1, 1, 0, 1, 1),
                                listOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 1),
                                listOf(1, 0, 0, 1, 0, 0, 1, 0, 0, 1),
                                listOf(1, 0, 0, 1, 0, 0, 1, 0, 0, 1),
                                listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
                            )

                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    if (seededQr[r][c] == 1) {
                                        drawRect(
                                            color = Color(0xFF0F172A),
                                            topLeft = Offset(c * cellW, r * cellH),
                                            size = Size(cellW, cellH)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "My Personal QR Code",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons/Toggles
                    Button(
                        onClick = { viewModel.setBottomSheetVisible(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("close_sheet_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Wallet Manager", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 23. EXPANDABLE FAB OVERLAY (Expense, Income, Transfer, Borrow, Goal)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Expanded Sub-actions
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        FabSubItem("New Income", Icons.Default.TrendingUp, Color(0xFF10B981)) {
                            viewModel.toggleFab()
                        }
                        FabSubItem("New Expense", Icons.Default.TrendingDown, Color(0xFFEF4444)) {
                            viewModel.toggleFab()
                        }
                        FabSubItem("Money Transfer", Icons.Default.SyncAlt, Color(0xFF3B82F6)) {
                            viewModel.toggleFab()
                        }
                        FabSubItem("New Borrow/Lend", Icons.Default.Handshake, Color(0xFFF59E0B)) {
                            viewModel.toggleFab()
                        }
                        FabSubItem("New Goal", Icons.Default.Star, Color(0xFF8B5CF6)) {
                            viewModel.toggleFab()
                        }
                    }
                }

                // Main floating trigger
                FloatingActionButton(
                    onClick = { viewModel.toggleFab() },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .scale(1.1f)
                        .testTag("expandable_fab_trigger")
                ) {
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Expand menu",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FabSubItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp))
        }
    }
}
