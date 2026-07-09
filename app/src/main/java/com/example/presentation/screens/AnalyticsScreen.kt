package com.example.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.presentation.components.PremiumPlaceholderScreen

@Composable
fun AnalyticsScreen(modifier: Modifier = Modifier) {
    PremiumPlaceholderScreen(
        title = "Analytics & Insights",
        icon = Icons.Default.BarChart,
        modifier = modifier
    )
}
