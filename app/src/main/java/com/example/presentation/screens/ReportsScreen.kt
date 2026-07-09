package com.example.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.presentation.components.PremiumPlaceholderScreen

@Composable
fun ReportsScreen(modifier: Modifier = Modifier) {
    PremiumPlaceholderScreen(
        title = "Financial Reports",
        icon = Icons.Default.Assessment,
        modifier = modifier
    )
}
