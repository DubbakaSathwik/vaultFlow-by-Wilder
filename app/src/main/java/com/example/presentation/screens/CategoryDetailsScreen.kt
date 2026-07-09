package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.presentation.viewmodel.IntelligenceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    categoryId: Int,
    viewModel: IntelligenceViewModel,
    onBackClick: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    val category = remember(categories, categoryId) {
        categories.find { it.id == categoryId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category?.name ?: "Envelope Analysis", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (category == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Envelope not found", fontWeight = FontWeight.Bold)
            }
        } else {
            val color = remember(category.colorHex) {
                try {
                    Color(android.graphics.Color.parseColor(category.colorHex))
                } catch (e: Exception) {
                    Color(0xFF3B82F6)
                }
            }

            // Filter transactions for this category
            val catTxs = remember(transactions, category.name) {
                transactions.filter { !it.isDeleted && it.categoryName.equals(category.name, ignoreCase = true) }
            }

            val spent = remember(catTxs) {
                catTxs.filter { it.type == "Expense" }.sumOf { it.amount }
            }

            val income = remember(catTxs) {
                catTxs.filter { it.type == "Income" }.sumOf { it.amount }
            }

            val remaining = remember(category.monthlyBudget, spent) {
                (category.monthlyBudget - spent).coerceAtLeast(0.0)
            }

            val mostUsedMerchant = remember(catTxs) {
                if (catTxs.isEmpty()) "None yet"
                else {
                    catTxs.groupingBy { it.merchantAlias.ifBlank { it.merchantName } }
                        .eachCount()
                        .maxByOrNull { it.value }?.key ?: "None yet"
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Ring Budget Card (Section 13)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Ring Canvas
                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val budgetValue = category.monthlyBudget
                                val progressRatio = if (budgetValue > 0) (spent / budgetValue).toFloat().coerceIn(0f, 1f) else 0f
                                val errorColor = MaterialTheme.colorScheme.error
                                
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // background track
                                    drawCircle(
                                        color = color.copy(alpha = 0.1f),
                                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    // active sweep
                                    drawArc(
                                        color = if (progressRatio >= category.notificationThreshold) errorColor else color,
                                        startAngle = -90f,
                                        sweepAngle = 360f * progressRatio,
                                        useCenter = false,
                                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val percent = if (budgetValue > 0) ((spent / budgetValue) * 100).toInt() else 0
                                    Text("$percent%", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = color)
                                    Text("used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Column {
                                Text("Monthly Breakdown", fontWeight = FontWeight.Bold, color = color)
                                Text("₹ ${String.format("%,.2f", spent)} Spent", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                                if (category.monthlyBudget > 0) {
                                    Text("of ₹ ${String.format("%,.2f", category.monthlyBudget)} budget limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Remaining: ₹ ${String.format("%,.2f", remaining)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (remaining > 0) color else MaterialTheme.colorScheme.error)
                                } else {
                                    Text("No budget set for this envelope.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                // Stats Dashboard
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Most Visited", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(mostUsedMerchant, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Inflow Recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹ ${String.format("%,.2f", income)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Section 13: Monthly Graph (Custom drawing canvas)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Weekly Expense Load", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Draw horizontal custom bars representing past 4 weeks
                            val calendar = Calendar.getInstance()
                            val now = System.currentTimeMillis()
                            val w1 = catTxs.filter { it.type == "Expense" && it.timestamp >= now - 7 * 24 * 3600 * 1000 }.sumOf { it.amount }
                            val w2 = catTxs.filter { it.type == "Expense" && it.timestamp in (now - 14 * 24 * 3600 * 1000)..(now - 7 * 24 * 3600 * 1000) }.sumOf { it.amount }
                            val w3 = catTxs.filter { it.type == "Expense" && it.timestamp in (now - 21 * 24 * 3600 * 1000)..(now - 14 * 24 * 3600 * 1000) }.sumOf { it.amount }
                            val w4 = catTxs.filter { it.type == "Expense" && it.timestamp in (now - 28 * 24 * 3600 * 1000)..(now - 21 * 24 * 3600 * 1000) }.sumOf { it.amount }

                            val weeklySpent = listOf(w4, w3, w2, w1)
                            val maxW = weeklySpent.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

                            weeklySpent.forEachIndexed { idx, amt ->
                                val label = "Week ${idx + 1}"
                                val ratio = (amt / maxW).toFloat().coerceIn(0.01f, 1f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(ratio)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(color)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("₹${amt.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                                }
                            }
                        }
                    }
                }

                // Recent Transactions List
                item {
                    Text(
                        "Envelope Transactions (${catTxs.size})",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (catTxs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No recent records for this envelope.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                } else {
                    items(catTxs) { tx ->
                        val txColor = if (tx.type == "Income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(txColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.type == "Income") Icons.Default.Add else Icons.Default.Remove,
                                        contentDescription = null,
                                        tint = txColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.merchantName, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${tx.paymentMethod} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.timestamp))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Text("₹ ${String.format("%,.2f", tx.amount)}", fontWeight = FontWeight.Bold, color = txColor)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
