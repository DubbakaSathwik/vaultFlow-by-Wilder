package com.example.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.presentation.components.PremiumPlaceholderScreen

@Composable
fun BorrowScreen(modifier: Modifier = Modifier) {
    PremiumPlaceholderScreen(
        title = "Borrow & Lend",
        icon = Icons.Default.Payments,
        modifier = modifier
    )
}
