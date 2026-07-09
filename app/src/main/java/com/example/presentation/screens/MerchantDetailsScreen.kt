package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Merchant
import com.example.domain.model.OcrHistory
import com.example.domain.model.Transaction
import com.example.presentation.viewmodel.IntelligenceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDetailsScreen(
    merchantId: Int,
    viewModel: IntelligenceViewModel,
    onBackClick: () -> Unit
) {
    val merchants by viewModel.allMerchants.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val ocrHistory by viewModel.allOcrHistories.collectAsState()

    val merchant = remember(merchants, merchantId) {
        merchants.find { it.id == merchantId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(merchant?.storeName ?: "Merchant Insights", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (merchant == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Merchant not found", fontWeight = FontWeight.Bold)
            }
        } else {
            // Find transactions and OCR entries matching this merchant storeName, name, or alias
            val matchedTxs = remember(transactions, merchant) {
                transactions.filter { tx ->
                    !tx.isDeleted && (
                        tx.merchantName.equals(merchant.merchantName, ignoreCase = true) ||
                        tx.merchantName.equals(merchant.alias, ignoreCase = true) ||
                        tx.merchantAlias.equals(merchant.alias, ignoreCase = true) ||
                        tx.merchantAlias.equals(merchant.merchantName, ignoreCase = true)
                    )
                }
            }

            val matchedOcr = remember(ocrHistory, merchant) {
                ocrHistory.filter { hist ->
                    hist.merchantName.equals(merchant.merchantName, ignoreCase = true) ||
                    hist.merchantName.equals(merchant.alias, ignoreCase = true)
                }
            }

            // Calculate metrics
            val totalVisits = matchedTxs.size
            val totalSpending = matchedTxs.filter { it.type == "Expense" }.sumOf { it.amount }
            val averageSpending = if (totalVisits > 0) totalSpending / totalVisits else 0.0

            val favoritePaymentMethod = remember(matchedTxs) {
                if (matchedTxs.isEmpty()) "Cash"
                else {
                    matchedTxs.groupingBy { it.paymentMethod }.eachCount().maxByOrNull { it.value }?.key ?: "Cash"
                }
            }

            val favoriteBank = remember(matchedTxs) {
                if (matchedTxs.isEmpty()) "Cash"
                else {
                    matchedTxs.groupingBy { it.bankAccountName }.eachCount().maxByOrNull { it.value }?.key ?: "Cash"
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main stats banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(merchant.storeName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                                    if (merchant.category.isNotEmpty()) {
                                        Text(merchant.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Spending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("₹ ${String.format("%,.2f", totalSpending)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Visits Registered", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("$totalVisits times", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                // AI Learned Favorites
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Payments, "Payment Mode", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Fav Payment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(favoritePaymentMethod, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountBalance, "Bank Card", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Fav Bank", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(favoriteBank, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // General Stats Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("General Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Average Order Size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("₹ ${String.format("%,.2f", averageSpending)}", fontWeight = FontWeight.Bold)
                            }
                            if (merchant.address.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Location", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(merchant.address, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (merchant.phone.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Contact Phone", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(merchant.phone, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (merchant.notes.isNotEmpty()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Memo/Notes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(merchant.notes, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // OCR Receipt Gallery (Section 12)
                if (matchedOcr.isNotEmpty()) {
                    item {
                        Text(
                            "Receipt Image Gallery (${matchedOcr.size})",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(matchedOcr) { ocr ->
                                Card(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(120.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                                            Icon(Icons.Default.Receipt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("₹ ${(ocr.amount?.toInt() ?: 0)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text(ocr.dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Transactions History
                item {
                    Text(
                        "Visit History (${matchedTxs.size})",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (matchedTxs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No recent records found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                } else {
                    items(matchedTxs) { tx ->
                        val txColor = if (tx.type == "Income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(txColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.type == "Income") Icons.Default.Add else Icons.Default.Remove,
                                        contentDescription = null,
                                        tint = txColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.categoryName, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${tx.paymentMethod} • ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.timestamp))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                Text("₹ ${String.format("%,.2f", tx.amount)}", fontWeight = FontWeight.Bold, color = txColor)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
