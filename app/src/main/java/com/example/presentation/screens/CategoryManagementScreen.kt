package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.presentation.viewmodel.IntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: IntelligenceViewModel,
    onBackClick: () -> Unit,
    onCategoryClick: (Int) -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Category Hub",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCategoryDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_category_fab")
            ) {
                Icon(Icons.Default.Add, "Add Category")
            }
        }
    ) { innerPadding ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val activeCategories = categories.filter { it.status == "Active" }
            val archivedCategories = categories.filter { it.status == "Archived" }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Header Stats
                item {
                    val totalBudget = activeCategories.sumOf { it.monthlyBudget }
                    val currentMonthSpent = transactions
                        .filter { !it.isDeleted && it.type == "Expense" }
                        .sumOf { it.amount }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Monthly Budget Enforcer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Icon(Icons.Default.VerifiedUser, "Intelligence Protected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text("Spent This Month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                    Text("₹ ${String.format("%,.2f", currentMonthSpent)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Budget Limit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                    Text("₹ ${String.format("%,.2f", totalBudget)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (totalBudget > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val ratio = (currentMonthSpent / totalBudget).toFloat().coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (ratio > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }

                // Section: Active Categories List
                item {
                    Text(
                        "Active Envelopes (${activeCategories.size})",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(activeCategories) { category ->
                    val catTxs = transactions.filter { !it.isDeleted && it.categoryName.equals(category.name, ignoreCase = true) }
                    val catSpent = catTxs.filter { it.type == "Expense" }.sumOf { it.amount }

                    CategoryListItemCard(
                        category = category,
                        spent = catSpent,
                        onClick = { onCategoryClick(category.id) },
                        onEditClick = { categoryToEdit = category },
                        onArchiveClick = { viewModel.updateCategory(category.copy(status = "Archived")) },
                        onFavoriteToggle = { viewModel.toggleCategoryFavorite(category) }
                    )
                }

                // Section: Archived Categories List
                if (archivedCategories.isNotEmpty()) {
                    item {
                        Text(
                            "Archived Envelopes (${archivedCategories.size})",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    items(archivedCategories) { category ->
                        CategoryListItemCard(
                            category = category,
                            spent = 0.0,
                            onClick = { onCategoryClick(category.id) },
                            onEditClick = { categoryToEdit = category },
                            onArchiveClick = { viewModel.updateCategory(category.copy(status = "Active")) }, // restore
                            onFavoriteToggle = {}
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Add dialog
    if (showAddCategoryDialog) {
        CategoryEditDialog(
            category = null,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, color, icon, desc, monthly, weekly, daily, type ->
                viewModel.addCategory(name, color, icon, desc, monthly, weekly, daily, type)
                showAddCategoryDialog = false
            }
        )
    }

    // Edit dialog
    if (categoryToEdit != null) {
        CategoryEditDialog(
            category = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onSave = { name, color, icon, desc, monthly, weekly, daily, type ->
                categoryToEdit?.let { orig ->
                    viewModel.updateCategory(orig.copy(
                        name = name,
                        colorHex = color,
                        iconName = icon,
                        description = desc,
                        monthlyBudget = monthly,
                        weeklyBudget = weekly,
                        dailyBudget = daily,
                        defaultTransactionType = type
                    ))
                }
                categoryToEdit = null
            }
        )
    }
}

@Composable
fun CategoryListItemCard(
    category: Category,
    spent: Double,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val color = remember(category.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(category.colorHex))
        } catch (e: Exception) {
            Color(0xFF3B82F6)
        }
    }

    val iconVector = when (category.iconName) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_bag" -> Icons.Default.ShoppingBag
        "school" -> Icons.Default.School
        "edit" -> Icons.Default.Edit
        "medical_services" -> Icons.Default.MedicalServices
        "local_gas_station" -> Icons.Default.LocalGasStation
        "movie" -> Icons.Default.Movie
        "people" -> Icons.Default.People
        "flight" -> Icons.Default.Flight
        "phone_android" -> Icons.Default.PhoneAndroid
        "receipt_long" -> Icons.Default.ReceiptLong
        else -> Icons.Default.Category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("category_card_${category.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconVector, contentDescription = category.name, tint = color)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            category.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (category.isFavorite) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Star, "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (category.description.isNotEmpty()) {
                        Text(
                            category.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (category.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (category.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onArchiveClick) {
                        Icon(
                            imageVector = if (category.status == "Active") Icons.Default.Archive else Icons.Default.Unarchive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (category.status == "Active" && category.monthlyBudget > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Spent: ₹${String.format("%,.2f", spent)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Budget: ₹${String.format("%,.2f", category.monthlyBudget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val budgetRatio = (spent / category.monthlyBudget).toFloat().coerceIn(0f, 1f)
                val alertThreshold = category.notificationThreshold.toFloat()
                
                LinearProgressIndicator(
                    progress = { budgetRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        budgetRatio >= 1.0f -> MaterialTheme.colorScheme.error
                        budgetRatio >= alertThreshold -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        else -> color
                    },
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )

                if (budgetRatio >= alertThreshold) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (budgetRatio >= 1.0f) "⚠️ Budget Exceeded!" else "⚠️ Threshold crossed! (${(alertThreshold * 100).toInt()}% of budget reached)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryEditDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var colorHex by remember { mutableStateOf(category?.colorHex ?: "#3B82F6") }
    var iconName by remember { mutableStateOf(category?.iconName ?: "category") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var monthlyBudget by remember { mutableStateOf(category?.monthlyBudget?.toString() ?: "0.0") }
    var weeklyBudget by remember { mutableStateOf(category?.weeklyBudget?.toString() ?: "0.0") }
    var dailyBudget by remember { mutableStateOf(category?.dailyBudget?.toString() ?: "0.0") }
    var defaultType by remember { mutableStateOf(category?.defaultTransactionType ?: "Expense") }

    val presetColors = listOf("#3B82F6", "#EF4444", "#10B981", "#F59E0B", "#8B5CF6", "#EC4899", "#06B6D4", "#F97316")
    val presetIcons = listOf("restaurant", "shopping_bag", "school", "edit", "medical_services", "local_gas_station", "movie", "people", "flight", "phone_android", "receipt_long", "category")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Create Category Envelope" else "Modify Envelope") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_category_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = monthlyBudget,
                        onValueChange = { monthlyBudget = it },
                        label = { Text("Monthly Budget (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weeklyBudget,
                        onValueChange = { weeklyBudget = it },
                        label = { Text("Weekly Budget (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = dailyBudget,
                    onValueChange = { dailyBudget = it },
                    label = { Text("Daily Budget (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Default Transaction Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Expense", "Income", "Both").forEach { type ->
                        val isSel = defaultType == type
                        FilterChip(
                            selected = isSel,
                            onClick = { defaultType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Text("Pick Identifier Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(col, CircleShape)
                                .clickable { colorHex = hex }
                                .clip(CircleShape)
                        ) {
                            if (colorHex == hex) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp).align(Alignment.Center))
                            }
                        }
                    }
                }

                Text("Select Icon Symbol", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetIcons) { icon ->
                        val isSelected = iconName == icon
                        val iconVec = when (icon) {
                            "restaurant" -> Icons.Default.Restaurant
                            "shopping_bag" -> Icons.Default.ShoppingBag
                            "school" -> Icons.Default.School
                            "edit" -> Icons.Default.Edit
                            "medical_services" -> Icons.Default.MedicalServices
                            "local_gas_station" -> Icons.Default.LocalGasStation
                            "movie" -> Icons.Default.Movie
                            "people" -> Icons.Default.People
                            "flight" -> Icons.Default.Flight
                            "phone_android" -> Icons.Default.PhoneAndroid
                            "receipt_long" -> Icons.Default.ReceiptLong
                            else -> Icons.Default.Category
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { iconName = icon }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(iconVec, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val mB = monthlyBudget.toDoubleOrNull() ?: 0.0
                        val wB = weeklyBudget.toDoubleOrNull() ?: 0.0
                        val dB = dailyBudget.toDoubleOrNull() ?: 0.0
                        onSave(name, colorHex, iconName, description, mB, wB, dB, defaultType)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Envelope")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
