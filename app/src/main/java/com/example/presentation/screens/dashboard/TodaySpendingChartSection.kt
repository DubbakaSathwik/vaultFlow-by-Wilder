package com.example.presentation.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TodaySpendingChartSection(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf("This Week") }
    val tabs = listOf("Today", "Yesterday", "This Week", "This Month")

    // Chart values for each tab (normalized 0..100)
    val chartData = when (selectedTab) {
        "Today" -> listOf(10f, 25f, 15f, 40f, 35f, 75f, 45f)
        "Yesterday" -> listOf(30f, 40f, 20f, 60f, 55f, 30f, 50f)
        "This Week" -> listOf(20f, 45f, 28f, 80f, 42f, 90f, 65f)
        else -> listOf(40f, 30f, 65f, 50f, 85f, 60f, 95f) // This Month
    }

    // Line drawing animation progress
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedTab) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_spending_chart_card"),
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
                text = "Spending Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sub tabs
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTab),
                edgePadding = 0.dp,
                divider = {},
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)])
                            .height(3.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(primaryColor)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Beautiful Canvas Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (chartData.size - 1)
                    
                    // Draw grid helper lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = height * i / gridLines
                        drawLine(
                            color = primaryColor.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (chartData.isNotEmpty()) {
                        // Create smooth path
                        val strokePath = Path()
                        val fillPath = Path()

                        val progress = animationProgress.value

                        // Coordinate calculator helper
                        fun getCoordinates(index: Int): Offset {
                            val x = index * stepX
                            // Invert y because (0,0) is top-left
                            val valNormal = (chartData[index] / 100f).coerceIn(0f, 1f)
                            val y = height - (valNormal * height * progress)
                            return Offset(x, y)
                        }

                        val startPoint = getCoordinates(0)
                        strokePath.moveTo(startPoint.x, startPoint.y)
                        fillPath.moveTo(startPoint.x, height)
                        fillPath.lineTo(startPoint.x, startPoint.y)

                        for (i in 1 until chartData.size) {
                            val prevPoint = getCoordinates(i - 1)
                            val currPoint = getCoordinates(i)

                            // Cubic Bezier curve controls for smoothness
                            val control1 = Offset(prevPoint.x + (currPoint.x - prevPoint.x) / 2f, prevPoint.y)
                            val control2 = Offset(prevPoint.x + (currPoint.x - prevPoint.x) / 2f, currPoint.y)

                            strokePath.cubicTo(
                                control1.x, control1.y,
                                control2.x, control2.y,
                                currPoint.x, currPoint.y
                            )

                            fillPath.cubicTo(
                                control1.x, control1.y,
                                control2.x, control2.y,
                                currPoint.x, currPoint.y
                            )
                        }

                        fillPath.lineTo(width, height)
                        fillPath.close()

                        // Draw background gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Draw path stroke
                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw dot anchors at data points
                        for (i in chartData.indices) {
                            val pt = getCoordinates(i)
                            drawCircle(
                                color = secondaryColor,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 2.5.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // X-axis values placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labels = when (selectedTab) {
                    "Today" -> listOf("9 AM", "12 PM", "3 PM", "6 PM", "9 PM", "Midnight", "Now")
                    "Yesterday" -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    "This Week" -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    else -> listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4", "Wk 5", "Wk 6", "Wk 7")
                }
                labels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
