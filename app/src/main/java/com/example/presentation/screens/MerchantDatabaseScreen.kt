package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Merchant
import com.example.presentation.viewmodel.IntelligenceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantDatabaseScreen(
    viewModel: IntelligenceViewModel,
    onBackClick: () -> Unit,
    onMerchantClick: (Int) -> Unit
) {
    val merchants by viewModel.allMerchants.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var merchantToEdit by remember { mutableStateOf<Merchant?>(null) }

    val displayedMerchants = remember(merchants, searchQuery) {
        if (searchQuery.isBlank()) {
            merchants
        } else {
            merchants.filter {
                it.merchantName.lowercase().contains(searchQuery.lowercase()) ||
                it.alias.lowercase().contains(searchQuery.lowercase()) ||
                it.storeName.lowercase().contains(searchQuery.lowercase()) ||
                it.category.lowercase().contains(searchQuery.lowercase())
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Merchant Intelligence", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Merchant")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_merchant_fab")
            ) {
                Icon(Icons.Default.Add, "Add Merchant")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Search merchants, tags or categories...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("merchant_search_input"),
                singleLine = true
            )

            if (displayedMerchants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "No Merchants Discovered",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Create a custom merchant template or save transactions to populate our memory offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedMerchants) { merchant ->
                        MerchantListItemCard(
                            merchant = merchant,
                            onClick = { onMerchantClick(merchant.id) },
                            onEditClick = { merchantToEdit = merchant },
                            onFavoriteToggle = { viewModel.toggleMerchantFavorite(merchant) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        MerchantEditDialog(
            merchant = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, alias, category, address, website, phone, notes ->
                viewModel.addMerchant(name, alias, category, address, website, phone, notes)
                showAddDialog = false
            }
        )
    }

    if (merchantToEdit != null) {
        MerchantEditDialog(
            merchant = merchantToEdit,
            onDismiss = { merchantToEdit = null },
            onSave = { name, alias, category, address, website, phone, notes ->
                merchantToEdit?.let { orig ->
                    viewModel.updateMerchant(orig.copy(
                        merchantName = name,
                        alias = alias,
                        storeName = alias.ifBlank { name },
                        category = category,
                        address = address,
                        website = website,
                        phone = phone,
                        notes = notes
                    ))
                }
                merchantToEdit = null
            }
        )
    }
}

@Composable
fun MerchantListItemCard(
    merchant: Merchant,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("merchant_card_${merchant.storeName.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (merchant.category.lowercase()) {
                        "food", "food & dining", "restaurant" -> Icons.Default.Restaurant
                        "shopping" -> Icons.Default.ShoppingBag
                        "groceries" -> Icons.Default.ShoppingCart
                        "medical", "health" -> Icons.Default.MedicalServices
                        else -> Icons.Default.Storefront
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        merchant.storeName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (merchant.isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Star, "Favorite", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
                if (merchant.alias.isNotBlank() && merchant.alias != merchant.merchantName) {
                    Text(
                        "Alias: ${merchant.merchantName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Text(
                    "${merchant.visitCount} visits • Total: ₹ ${String.format("%,.2f", merchant.totalSpending)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Row {
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (merchant.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (merchant.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun MerchantEditDialog(
    merchant: Merchant?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(merchant?.merchantName ?: "") }
    var alias by remember { mutableStateOf(merchant?.alias ?: "") }
    var category by remember { mutableStateOf(merchant?.category ?: "") }
    var address by remember { mutableStateOf(merchant?.address ?: "") }
    var website by remember { mutableStateOf(merchant?.website ?: "") }
    var phone by remember { mutableStateOf(merchant?.phone ?: "") }
    var notes by remember { mutableStateOf(merchant?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (merchant == null) "Create Merchant template" else "Modify Merchant") },
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
                    label = { Text("Original/Raw Scanned Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_merchant_name_input")
                )

                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Display/Alias Name (Normalized)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_merchant_alias_input")
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Default Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, alias, category, address, website, phone, notes)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Merchant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
