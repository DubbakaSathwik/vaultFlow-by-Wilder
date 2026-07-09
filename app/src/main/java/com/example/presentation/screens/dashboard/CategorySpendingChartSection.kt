package com.example.presentation.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CategoryShare(
    val category: String,
    val share: Float, // percentage e.g. 0.3f
    val color: Color
)

@Composable
fun CategorySpendingChartSection(
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryShares = listOf(
        CategoryShare("Food", 0.30f, Color(0xFFF43F5E)),        // Coral Red
        CategoryShare("College", 0.20f, Color(0xFF38BDF8)),     // Sky Blue
        CategoryShare("Shopping", 0.18f, Color(0xFFA78BFA)),    // Purple
        CategoryShare("Entertainment", 0.12f, Color(0xFFFBBF24)),// Amber
        CategoryShare("Transport", 0.10f, Color(0xFF10B981)),    // Green
        CategoryShare("Family", 0.06f, Color(0xFFEC4899)),       // Pink
        CategoryShare("Others", 0.04f, Color(0xFF94A3B8))        // Slate Gray
    )

    // Sweep animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_spending_pie_card"),
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
            Text(
                text = "Category Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Donut Chart Canvas
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val strokeWidthPx = 16.dp.toPx()

                        categoryShares.forEach { slice ->
                            val sweepAngle = slice.share * 360f * animationProgress.value
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidthPx)
                            )
                            startAngle += slice.share * 360f
                        }
                    }

                    // Inner circle text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹18.2K",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Legends
                Column(
                    modifier = Modifier.weight(1.2f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryShares.take(4).forEach { slice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onCategoryClick(slice.category) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = slice.category,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(slice.share * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Button to expand more legends
                    if (categoryShares.size > 4) {
                        Text(
                            text = "+ ${categoryShares.size - 4} more categories",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 18.dp)
                                .clickable { onCategoryClick("All") }
                        )
                    }
                }
            }
        }
    }
}
