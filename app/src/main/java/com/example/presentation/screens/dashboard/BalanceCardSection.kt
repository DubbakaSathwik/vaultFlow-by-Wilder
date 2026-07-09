package com.example.presentation.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.BalanceAccount
import com.example.presentation.viewmodel.DashboardState
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue

@Composable
fun BalanceCardSection(
    state: DashboardState,
    onAccountChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts = state.accounts
    val currentIndex = state.currentAccountIndex
    val currentAccount = accounts[currentIndex]

    // Simple horizontal swipe gesture detector
    var offsetX by remember { mutableStateOf(0f) }

    // Animated counter effect
    val animatedBalance = remember { Animatable(0f) }
    LaunchedEffect(currentAccount.balance) {
        animatedBalance.animateTo(
            targetValue = currentAccount.balance.toFloat(),
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    // Gradient definitions
    val overallGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF334155)
        )
    )

    val lightGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0284C7),
            Color(0xFF0EA5E9),
            Color(0xFF38BDF8)
        )
    )

    val greenGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF064E3B),
            Color(0xFF0F766E),
            Color(0xFF14B8A6)
        )
    )

    val indigoGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF312E81),
            Color(0xFF4338CA),
            Color(0xFF6366F1)
        )
    )

    val finalGradient = when (currentAccount.name) {
        "Overall Balance" -> overallGradient
        "Cash" -> greenGradient
        "SBI Savings" -> lightGradient
        "HDFC Bank" -> indigoGradient
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                Color(0xFF2C2C2C),
                Color(0xFF3D3D3D)
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX > 150f) {
                            // Swipe Right -> Prev Account
                            val prev = if (currentIndex > 0) currentIndex - 1 else accounts.lastIndex
                            onAccountChanged(prev)
                        } else if (offsetX < -150f) {
                            // Swipe Left -> Next Account
                            val next = if (currentIndex < accounts.lastIndex) currentIndex + 1 else 0
                            onAccountChanged(next)
                        }
                        offsetX = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    }
                )
            }
            .testTag("balance_card_container"),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(finalGradient)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Account Name & Type Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentAccount.name,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentAccount.type,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "VaultFlow Safe",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Balance Number (Animated)
                Text(
                    text = currencyFormatter.format(animatedBalance.value),
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("balance_display_text")
                )

                // Bottom Row: Income, Expense, Savings Breakdown (only on Overall, or placeholder stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INFLOW",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = currencyFormatter.format(state.monthlyIncome),
                            color = Color(0xFF34D399),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "OUTFLOW",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = currencyFormatter.format(state.monthlyExpense),
                            color = Color(0xFFF87171),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "SAVED",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = currencyFormatter.format(state.savings),
                            color = Color(0xFF60A5FA),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Carousel Indicators at the bottom center
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                accounts.forEachIndexed { idx, _ ->
                    val isSelected = idx == currentIndex
                    Box(
                        modifier = Modifier
                            .size(width = if (isSelected) 14.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}
