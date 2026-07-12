package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.data.ocr.ChatMessage
import com.example.presentation.viewmodel.IntelligenceViewModel
import com.example.presentation.viewmodel.TimelineEvent
import com.example.presentation.viewmodel.FinancialReview
import com.example.presentation.viewmodel.IntelligenceHealthScore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: IntelligenceViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val insights by viewModel.aiInsights.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf(
        "AI Co-Pilot Chat",
        "Score Breakdown",
        "Time Reviews",
        "Predictions & Safe Spends",
        "Anomalies & Duplicates",
        "Patterns & Usage",
        "Milestones Timeline",
        "Streaks & Achievements"
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Finance Assistant",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("ai_assistant_title")
                        )
                        Text(
                            text = "Advanced AI Financial Co-Pilot",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("ai_assistant_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
            // Horizontal Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("ai_assistant_tabs")
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Content
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "TabTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { targetTab ->
                if (targetTab == 0) {
                    AiChatSection(viewModel)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (targetTab) {
                            1 -> {
                                // SCORE BREAKDOWN
                                item {
                                    HealthScoreRadialSection(insights.healthScore)
                                }
                                item {
                                    Text(
                                        text = "Detailed Category Explanations",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                item {
                                    ScoreExplainerCard(
                                        title = "Monthly Budget Safety",
                                        score = insights.healthScore.budgetScore,
                                        explanation = insights.healthScore.budgetExpl,
                                        icon = Icons.Default.AccountBalanceWallet,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                                item {
                                    ScoreExplainerCard(
                                        title = "Savings & Investment Rate",
                                        score = insights.healthScore.savingsScore,
                                        explanation = insights.healthScore.savingsExpl,
                                        icon = Icons.Default.Savings,
                                        color = Color(0xFFA78BFA)
                                    )
                                }
                                item {
                                    ScoreExplainerCard(
                                        title = "Debt & Lending Risk",
                                        score = insights.healthScore.debtScore,
                                        explanation = insights.healthScore.debtExpl,
                                        icon = Icons.Default.Payments,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                item {
                                    ScoreExplainerCard(
                                        title = "Logging & Habit Consistency",
                                        score = insights.healthScore.consistencyScore,
                                        explanation = insights.healthScore.consistencyExpl,
                                        icon = Icons.Default.Timeline,
                                        color = Color(0xFFFBBF24)
                                    )
                                }
                                item {
                                    ScoreExplainerCard(
                                        title = "Subscription Fixed Overhead",
                                        score = insights.healthScore.subscriptionsScore,
                                        explanation = insights.healthScore.subscriptionsExpl,
                                        icon = Icons.Default.Subscriptions,
                                        color = Color(0xFFF43F5E)
                                    )
                                }
                                item {
                                    ScoreExplainerCard(
                                        title = "Savings Goals Progress",
                                        score = insights.healthScore.goalsScore,
                                        explanation = insights.healthScore.goalsExpl,
                                        icon = Icons.Default.Star,
                                        color = Color(0xFF06B6D4)
                                    )
                                }
                            }
                            2 -> {
                                // TIME REVIEWS
                                item {
                                    TimeReviewCard(
                                        title = "Daily Financial Review",
                                        review = insights.dailyReview,
                                        icon = Icons.Default.Today,
                                        gradient = Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF38BDF8)))
                                    )
                                }
                                item {
                                    TimeReviewCard(
                                        title = "Weekly Financial Review",
                                        review = insights.weeklyReview,
                                        icon = Icons.Default.ViewWeek,
                                        gradient = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFA78BFA)))
                                    )
                                }
                                item {
                                    TimeReviewCard(
                                        title = "Monthly Financial Review",
                                        review = insights.monthlyReview,
                                        icon = Icons.Default.CalendarMonth,
                                        gradient = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399)))
                                    )
                                }
                                item {
                                    TimeReviewCard(
                                        title = "Yearly Financial Review",
                                        review = insights.yearlyReview,
                                        icon = Icons.Default.AutoAwesome,
                                        gradient = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
                                    )
                                }
                            }
                            3 -> {
                                // PREDICTIONS & RECOMMENDATIONS
                                item {
                                    ValueCard(
                                        title = "Recommended Monthly Budget Limit",
                                        value = "₹${insights.recommendedMonthlyBudget.toInt()}",
                                        subtitle = "Engineered dynamically based on past 30 days income",
                                        icon = Icons.Default.TrendingDown,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                item {
                                    ValueCard(
                                        title = "Recommended Safe Daily Spending",
                                        value = "₹${insights.recommendedDailySafeSpending.toInt()}",
                                        subtitle = "Stay below this threshold to protect your savings",
                                        icon = Icons.Default.OfflineBolt,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                item {
                                    ValueCard(
                                        title = "Next Month Expense Prediction",
                                        value = "₹${insights.expensePredictionNextMonth.toInt()}",
                                        subtitle = "Projected spending trend based on local regression analysis",
                                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                                        color = Color(0xFFF43F5E)
                                    )
                                }
                                item {
                                    PredictionsListSection(
                                        title = "Savings Goals Projected Completions",
                                        items = insights.goalCompletionPredictions,
                                        icon = Icons.Default.HourglassBottom,
                                        emptyMessage = "No active savings goals found to run predictions on."
                                    )
                                }
                            }
                            4 -> {
                                // ANOMALIES & DUPLICATES
                                item {
                                    DuplicateTransactionsSection(insights.duplicateTransactions)
                                }
                                item {
                                    StandardListSection(
                                        title = "Overspending Detection Alerts",
                                        items = insights.overspendingAlerts,
                                        icon = Icons.Default.Warning,
                                        cardColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                        textColor = MaterialTheme.colorScheme.error
                                    )
                                }
                                item {
                                    StandardListSection(
                                        title = "Missing Transaction Suggestions",
                                        items = insights.missingTransactionSuggestions,
                                        icon = Icons.Default.Help,
                                        cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            5 -> {
                                // PATTERNS & USAGE
                                item {
                                    StandardListSection(
                                        title = "Merchant Spending Analysis",
                                        items = insights.merchantSpendingAnalysis,
                                        icon = Icons.Default.Storefront
                                    )
                                }
                                item {
                                    StandardListSection(
                                        title = "Category Distribution Analysis",
                                        items = insights.categorySpendingAnalysis,
                                        icon = Icons.Default.Category
                                    )
                                }
                                item {
                                    StandardListSection(
                                        title = "Bank Account Interaction Volume",
                                        items = insights.bankUsageAnalysis,
                                        icon = Icons.Default.AccountBalance
                                    )
                                }
                                item {
                                    StandardListSection(
                                        title = "Payment Methods Usage Stats",
                                        items = insights.paymentMethodAnalysis,
                                        icon = Icons.Default.CreditCard
                                    )
                                }
                            }
                            6 -> {
                                // MILESTONES TIMELINE
                                item {
                                    Text(
                                        text = "Chronological Milestones & Highlights",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(insights.healthTimeline) { event ->
                                    TimelineEventRow(event)
                                }
                            }
                            7 -> {
                                // STREAKS, ACHIEVEMENTS & TIPS
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        StreakBox(
                                            title = "Spending Streak",
                                            days = insights.spendingStreak,
                                            subtitle = "Days under budget limit",
                                            icon = Icons.Default.LocalFireDepartment,
                                            color = Color(0xFFF97316),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StreakBox(
                                            title = "Savings Streak",
                                            days = insights.savingsStreak,
                                            subtitle = "Consecutive savings months",
                                            icon = Icons.Default.AutoAwesome,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                item {
                                    StandardListSection(
                                        title = "Unlocked Achievements",
                                        items = insights.achievements,
                                        icon = Icons.Default.WorkspacePremium,
                                        cardColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                                        textColor = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                item {
                                    StandardListSection(
                                        title = "Dynamic Saving Recommendations",
                                        items = insights.savingRecommendations,
                                        icon = Icons.Default.Lightbulb
                                    )
                                }
                                item {
                                    StandardListSection(
                                        title = "Fixed Overheads & Subscription Analysis",
                                        items = insights.subscriptionAnalysis,
                                        icon = Icons.Default.Subscriptions
                                    )
                                }
                                item {
                                    StandardListSection(
                                        title = "Ledger Borrow & Lend Risk Analysis",
                                        items = insights.borrowLendRiskAnalysis,
                                        icon = Icons.Default.VerifiedUser
                                    )
                                }
                                item {
                                    StandardListSection(
                                        title = "Upcoming Financial Events (5 Days)",
                                        items = insights.upcomingFinancialEvents,
                                        icon = Icons.Default.Event,
                                        cardColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        textColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthScoreRadialSection(score: IntelligenceHealthScore) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("health_score_radial_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Financial Health Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                val ratio = score.totalScore.toFloat() / 100f
                val animatedProgress by animateFloatAsState(
                    targetValue = ratio,
                    animationSpec = tween(1200, easing = LinearOutSlowInEasing),
                    label = "healthRing"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = Color(0xFF10B981),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${score.totalScore}",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "HEALTHY INDEX",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This score is computed fully offline using deterministic metrics from your budgets, savings streaks, debt risk ratio, and habits.",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun ScoreExplainerCard(
    title: String,
    score: Int,
    explanation: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("score_expl_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("$score/100", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { score.toFloat() / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = explanation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun TimeReviewCard(
    title: String,
    review: FinancialReview,
    icon: ImageVector,
    gradient: Brush
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("time_review_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Spent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("₹${review.totalSpent.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFF43F5E))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("₹${review.totalIncome.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF10B981))
                }
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("Top Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(review.topCategory, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = review.reviewText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun ValueCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("value_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun PredictionsListSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    emptyMessage: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("predictions_list_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (items.isEmpty() || items.all { it.isBlank() }) {
                Text(emptyMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(item, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandardListSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("standard_list_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = textColor)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (items.isEmpty() || items.all { it.isBlank() }) {
                Text("All clean. No suggestions or events currently compiled.", fontSize = 12.sp, color = textColor.copy(alpha = 0.5f))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(textColor.copy(alpha = 0.6f), CircleShape))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                item, 
                                fontSize = 12.sp, 
                                color = textColor.copy(alpha = 0.8f), 
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateTransactionsSection(duplicates: List<Pair<com.example.domain.model.Transaction, com.example.domain.model.Transaction>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("duplicates_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (duplicates.isNotEmpty()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterNone,
                    contentDescription = null,
                    tint = if (duplicates.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Potential Duplicate Transactions",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (duplicates.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (duplicates.isEmpty()) {
                Text(
                    text = "No double payments or potential identical transactions flagged in the last 7 days. Your ledger is clean!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    lineHeight = 16.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    duplicates.forEach { pair ->
                        val t = pair.first
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(t.merchantName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("₹${t.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Logged in ${t.categoryName} within 10 minutes. Review if you'd like to trash or correct this duplicate entry.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineEventRow(event: TimelineEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_row_${event.title.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (event.type) {
                    "Biggest Purchase" -> Icons.Default.ShoppingBag
                    "Highest Spending" -> Icons.AutoMirrored.Filled.TrendingUp
                    "Goal Completed" -> Icons.Default.Stars
                    "Budget Exceeded" -> Icons.Default.Warning
                    else -> Icons.Default.Lock
                }
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            // Vertical timeline path connector line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val format = SimpleDateFormat("dd MMM", Locale.getDefault())
                Text(format.format(Date(event.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun StreakBox(
    title: String,
    days: Int,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("streak_box_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (days > 0) "$days Days" else "0 Days",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AiChatSection(
    viewModel: IntelligenceViewModel,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Chat list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Conversational AI Finance Co-Pilot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ask anything about your cashflow, budgets, goals, or debts!\nTry: \"How much did I pay to xyz friend this month?\" or \"Show my top expenses.\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                items(chatMessages) { msg ->
                    ChatBubble(msg)
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Analyzing ledger and responding...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask Co-Pilot...", fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input_field"),
                trailingIcon = {
                    if (chatMessages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearChatHistory() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    if (inputQuery.isNotBlank() && !isLoading) {
                        val query = inputQuery
                        inputQuery = ""
                        viewModel.sendChatMessage(query)
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("ai_chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = if (isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .widthIn(max = 290.dp)
                .testTag(if (isUser) "chat_bubble_user" else "chat_bubble_model")
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = if (isUser) "You" else "Finance Co-Pilot",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = msg.content,
                    fontSize = 13.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

