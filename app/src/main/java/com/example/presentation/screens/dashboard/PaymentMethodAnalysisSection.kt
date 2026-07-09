package com.example.presentation.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

data class PaymentMethodVolume(
    val name: String,
    val amount: Double,
    val share: Float,
    val color: Color
)

@Composable
fun PaymentMethodAnalysisSection(modifier: Modifier = Modifier) {
    val items = listOf(
        PaymentMethodVolume("UPI App Payments", 8500.0, 0.47f, Color(0xFF818CF8)),
        PaymentMethodVolume("Debit & Credit Cards", 5200.0, 0.28f, Color(0xFFF472B6)),
        PaymentMethodVolume("Cash Transactions", 3000.0, 0.17f, Color(0xFF34D399)),
        PaymentMethodVolume("Net Banking Transfer", 1500.0, 0.08f, Color(0xFFFB923C))
    )

    var animatedProgressTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animatedProgressTrigger = true
    }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_method_analysis_card"),
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
                text = "Payment Method Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEach { method ->
                    val animatedShare by animateFloatAsState(
                        targetValue = if (animatedProgressTrigger) method.share else 0f,
                        animationSpec = tween(durationMillis = 1000),
                        label = "BarShareAnim"
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = method.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${currencyFormatter.format(method.amount)} (${(method.share * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom bar tracker
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedShare)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(method.color)
                            )
                        }
                    }
                }
            }
        }
    }
}
