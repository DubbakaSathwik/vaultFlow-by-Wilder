package com.example.presentation.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.DashboardState

@Composable
fun FinancialHealthScoreSection(
    state: DashboardState,
    modifier: Modifier = Modifier
) {
    val score = state.financialHealthScore
    val maxScore = 100
    val normalRatio = score.toFloat() / maxScore.toFloat()

    var animatedProgressTrigger by remember { mutableStateOf(0f) }
    LaunchedEffect(normalRatio) {
        animatedProgressTrigger = normalRatio
    }

    val animatedProgress by animateFloatAsState(
        targetValue = animatedProgressTrigger,
        animationSpec = tween(durationMillis = 1200),
        label = "HealthScoreProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_health_score_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left circular indicator
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val accentColor = Color(0xFF10B981) // Green for healthy
                val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

                Canvas(modifier = Modifier.size(90.dp)) {
                    val strokeWidthPx = 10.dp.toPx()

                    // Track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx)
                    )

                    // Progress
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "EXCELLENT",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right breakdown
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "Financial Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScoreProgressRow("Budgeting", state.healthBudgetScore, Color(0xFF38BDF8))
                    ScoreProgressRow("Savings Flow", state.healthSavingsScore, Color(0xFFA78BFA))
                    ScoreProgressRow("Borrow Repay", state.healthLoansScore, Color(0xFF10B981))
                }
            }
        }
    }
}

@Composable
fun ScoreProgressRow(label: String, score: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.2f),
            maxLines = 1
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(score.toFloat() / 100f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$score",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
