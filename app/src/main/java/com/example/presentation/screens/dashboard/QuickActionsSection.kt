package com.example.presentation.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Storefront

data class QuickActionItem(
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val color: Color
)

@Composable
fun QuickActionsSection(
    onActionClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        QuickActionItem("Add Expense", Icons.Default.Add, "add_expense", MaterialTheme.colorScheme.primary),
        QuickActionItem("Private Vault", Icons.Default.Lock, "vault", Color(0xFF8B5CF6)),
        QuickActionItem("Import SS", Icons.Default.FileUpload, "import_ss", MaterialTheme.colorScheme.secondary),
        QuickActionItem("Categories", Icons.Default.Category, "categories", MaterialTheme.colorScheme.tertiary),
        QuickActionItem("Merchants", Icons.Default.Storefront, "merchants", Color(0xFF10B981)),
        QuickActionItem("Borrow/Lend", Icons.Default.Handshake, "borrow_lend", MaterialTheme.colorScheme.tertiary),
        QuickActionItem("Add Goal", Icons.Default.Stars, "add_goal", MaterialTheme.colorScheme.error),
        QuickActionItem("Export PDF", Icons.Default.PictureAsPdf, "export_pdf", Color(0xFFF59E0B))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEach { item ->
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1.0f,
                animationSpec = tween(100),
                label = "ActionScale"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(84.dp)
                    .scale(scale)
                    .clickable {
                        isPressed = true
                        onActionClicked(item.tag)
                        // Auto reset pressed state after scale animation
                    }
                    .testTag("quick_action_${item.tag}")
            ) {
                // Large circular icon button with glowing background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(item.color.copy(alpha = 0.12f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = item.color,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }

            LaunchedEffect(isPressed) {
                if (isPressed) {
                    tween<Float>(100)
                    isPressed = false
                }
            }
        }
    }
}
