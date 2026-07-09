package com.example.presentation.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MonthlyComparisonData(
    val month: String,
    val inflow: Float, // normalized e.g. 0.8f
    val outflow: Float // normalized e.g. 0.6f
)

@Composable
fun MonthlyComparisonSection(modifier: Modifier = Modifier) {
    val items = listOf(
        MonthlyComparisonData("Jan", 0.7f, 0.45f),
        MonthlyComparisonData("Feb", 0.8f, 0.55f),
        MonthlyComparisonData("Mar", 0.65f, 0.5f),
        MonthlyComparisonData("Apr", 0.9f, 0.75f),
        MonthlyComparisonData("May", 0.85f, 0.6f),
        MonthlyComparisonData("Jun", 0.95f, 0.65f)
    )

    var animateTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateTrigger = true
    }

    val animatedHeightFactor by animateFloatAsState(
        targetValue = if (animateTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "BarChartHeightAnim"
    )

    val inflowColor = Color(0xFF10B981) // Green
    val outflowColor = Color(0xFFEF4444) // Red

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_comparison_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Flow Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Legends
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(inflowColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Inflow", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(outflowColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Outflow", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bar Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val barSpacing = width / items.size
                    val barWidth = 10.dp.toPx()
                    val groupSpacing = 4.dp.toPx()

                    items.forEachIndexed { index, data ->
                        val groupCenterX = (index * barSpacing) + (barSpacing / 2)

                        // 1. Inflow Bar
                        val inflowBarHeight = data.inflow * height * animatedHeightFactor
                        val inflowLeft = groupCenterX - barWidth - (groupSpacing / 2)
                        val inflowTop = height - inflowBarHeight
                        drawRoundRect(
                            color = inflowColor,
                            topLeft = Offset(inflowLeft, inflowTop),
                            size = Size(barWidth, inflowBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // 2. Outflow Bar
                        val outflowBarHeight = data.outflow * height * animatedHeightFactor
                        val outflowLeft = groupCenterX + (groupSpacing / 2)
                        val outflowTop = height - outflowBarHeight
                        drawRoundRect(
                            color = outflowColor,
                            topLeft = Offset(outflowLeft, outflowTop),
                            size = Size(barWidth, outflowBarHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // X-Axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                items.forEach { data ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.month,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
