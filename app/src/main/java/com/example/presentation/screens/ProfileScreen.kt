package com.example.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.BankAccount
import com.example.domain.model.BalanceAdjustment
import com.example.domain.model.PaymentMethod
import com.example.domain.model.UserProfile
import com.example.presentation.viewmodel.ProfileViewModel
import com.example.presentation.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val adjustments by viewModel.balanceAdjustments.collectAsState()
    
    val hideBalances by settingsViewModel.hideBalances.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Profile, 1: Bank Accounts, 2: Payment Methods, 3: Adjustments
    val tabs = listOf("Profile", "Banks", "Payments", "History")

    // Bottom Sheet states
    var showAddBankSheet by remember { mutableStateOf(false) }
    var selectedBankForEdit by remember { mutableStateOf<BankAccount?>(null) }

    var showAddPaymentSheet by remember { mutableStateOf(false) }
    var selectedPaymentForEdit by remember { mutableStateOf<PaymentMethod?>(null) }

    var showAdjustmentSheet by remember { mutableStateOf(false) }
    var showQrBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "VaultFlow Space",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showAdjustmentSheet = true },
                        modifier = Modifier.testTag("quick_adjust_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Balance Adjustment",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showQrBottomSheet = true },
                        modifier = Modifier.testTag("profile_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "My QRs",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // High-fidelity User summary Card
            profile?.let { p ->
                UserProfileHeader(profile = p, onPictureSelected = { uri ->
                    viewModel.updateProfile(p.copy(profilePictureUri = uri.toString()))
                })
            }

            // Centralized navigation tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("profile_tab_$index")
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> ProfileDetailsTab(
                        profile = profile ?: UserProfile(),
                        onProfileUpdated = { viewModel.updateProfile(it) }
                    )
                    1 -> BankAccountsTab(
                        bankAccounts = bankAccounts,
                        hideBalances = hideBalances,
                        onAddBankClick = { showAddBankSheet = true },
                        onEditBankClick = { selectedBankForEdit = it }
                    )
                    2 -> PaymentMethodsTab(
                        paymentMethods = paymentMethods,
                        onAddPaymentClick = { showAddPaymentSheet = true },
                        onEditPaymentClick = { selectedPaymentForEdit = it },
                        onFavoriteToggle = { viewModel.togglePaymentMethodFavorite(it) }
                    )
                    3 -> BalanceHistoryTab(
                        adjustments = adjustments,
                        hideBalances = hideBalances
                    )
                }
            }
        }
    }

    // BOTTOM SHEETS & SUB-UI DIALOGS
    if (showAddBankSheet) {
        BankUpsertDialog(
            bank = null,
            onDismiss = { showAddBankSheet = false },
            onSave = { bank ->
                viewModel.addBankAccount(bank)
                showAddBankSheet = false
            }
        )
    }

    selectedBankForEdit?.let { bank ->
        BankUpsertDialog(
            bank = bank,
            onDismiss = { selectedBankForEdit = null },
            onSave = { updatedBank ->
                viewModel.updateBankAccount(updatedBank)
                selectedBankForEdit = null
            },
            onDelete = {
                viewModel.deleteBankAccount(bank)
                selectedBankForEdit = null
            }
        )
    }

    if (showAddPaymentSheet) {
        PaymentMethodUpsertDialog(
            payment = null,
            banks = bankAccounts,
            onDismiss = { showAddPaymentSheet = false },
            onSave = { pm ->
                viewModel.addPaymentMethod(pm)
                showAddPaymentSheet = false
            }
        )
    }

    selectedPaymentForEdit?.let { pm ->
        PaymentMethodUpsertDialog(
            payment = pm,
            banks = bankAccounts,
            onDismiss = { selectedPaymentForEdit = null },
            onSave = { updatedPm ->
                viewModel.updatePaymentMethod(updatedPm)
                selectedPaymentForEdit = null
            },
            onDelete = {
                viewModel.deletePaymentMethod(pm)
                selectedPaymentForEdit = null
            }
        )
    }

    if (showAdjustmentSheet) {
        BalanceAdjustmentDialog(
            accounts = bankAccounts,
            onDismiss = { showAdjustmentSheet = false },
            onConfirm = { type, accountId, amount, reason, notes, targetId ->
                viewModel.adjustBalance(type, accountId, amount, reason, notes, targetId)
                showAdjustmentSheet = false
            }
        )
    }

    if (showQrBottomSheet) {
        MyQrCodesBottomSheet(
            paymentMethods = paymentMethods.filter { it.upiId.isNotEmpty() || it.qrImageUri.isNotEmpty() },
            onDismiss = { showQrBottomSheet = false }
        )
    }
}

// 1. User Profile Header with Image Picker
@Composable
fun UserProfileHeader(
    profile: UserProfile,
    onPictureSelected: (Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPictureSelected(it) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { launcher.launch("image/*") }
                    .testTag("avatar_picker"),
                contentAlignment = Alignment.Center
            ) {
                if (profile.profilePictureUri.isNotEmpty()) {
                    AsyncImage(
                        model = profile.profilePictureUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Edit Picture",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(20.dp)
                            .padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "@${profile.nickname.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// 2. Profile Details Tab (Edit form)
@Composable
fun ProfileDetailsTab(
    profile: UserProfile,
    onProfileUpdated: (UserProfile) -> Unit
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var nickname by remember(profile) { mutableStateOf(profile.nickname) }
    var email by remember(profile) { mutableStateOf(profile.email) }
    var mobile by remember(profile) { mutableStateOf(profile.mobile) }
    var currency by remember(profile) { mutableStateOf(profile.currency) }
    var timeFormat by remember(profile) { mutableStateOf(profile.timeFormat) }
    var dateFormat by remember(profile) { mutableStateOf(profile.dateFormat) }
    var country by remember(profile) { mutableStateOf(profile.country) }
    var language by remember(profile) { mutableStateOf(profile.language) }
    var financialYear by remember(profile) { mutableStateOf(profile.financialYearStartMonth) }
    var firstDay by remember(profile) { mutableStateOf(profile.firstDayOfWeek) }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input"),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true
            )

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Nickname") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_nickname_input"),
                leadingIcon = { Icon(Icons.Default.AlternateEmail, null) },
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_email_input"),
                leadingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true
            )

            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("Mobile (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_mobile_input"),
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                singleLine = true
            )

            // Configuration Pickers (Simulated drop downs for luxury layout)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigDropdown(
                    label = "Currency",
                    selected = currency,
                    options = listOf("INR (₹)", "USD ($)", "EUR (€)", "GBP (£)", "JPY (¥)"),
                    onSelect = { currency = it },
                    modifier = Modifier.weight(1f)
                )
                ConfigDropdown(
                    label = "Time Format",
                    selected = timeFormat,
                    options = listOf("12 Hour", "24 Hour"),
                    onSelect = { timeFormat = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigDropdown(
                    label = "Date Format",
                    selected = dateFormat,
                    options = listOf("dd MMM yyyy", "yyyy-MM-dd", "MM/dd/yyyy"),
                    onSelect = { dateFormat = it },
                    modifier = Modifier.weight(1f)
                )
                ConfigDropdown(
                    label = "Language",
                    selected = language,
                    options = listOf("English", "Hindi", "Telugu", "Tamil", "Spanish"),
                    onSelect = { language = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigDropdown(
                    label = "Financial Year Starts",
                    selected = financialYear,
                    options = listOf("January", "April", "July", "October"),
                    onSelect = { financialYear = it },
                    modifier = Modifier.weight(1f)
                )
                ConfigDropdown(
                    label = "First Day of Week",
                    selected = firstDay,
                    options = listOf("Monday", "Sunday"),
                    onSelect = { firstDay = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    onProfileUpdated(
                        profile.copy(
                            name = name,
                            nickname = nickname,
                            email = email,
                            mobile = mobile,
                            currency = currency,
                            timeFormat = timeFormat,
                            dateFormat = dateFormat,
                            country = country,
                            language = language,
                            financialYearStartMonth = financialYear,
                            firstDayOfWeek = firstDay
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_profile_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Profile Changes", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 3. Config dropdown helper
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selected,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onSelect(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 4. Bank Accounts Tab with Gradient Cards & CRUD Actions
@Composable
fun BankAccountsTab(
    bankAccounts: List<BankAccount>,
    hideBalances: Boolean,
    onAddBankClick: () -> Unit,
    onEditBankClick: (BankAccount) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Bank Accounts (${bankAccounts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onAddBankClick,
                    modifier = Modifier.testTag("add_bank_button")
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Bank")
                }
            }
        }

        if (bankAccounts.isEmpty()) {
            item {
                EmptyStateWidget(
                    title = "No bank accounts linked",
                    desc = "Add your savings, current accounts or digital wallets."
                )
            }
        } else {
            items(bankAccounts) { bank ->
                LuxuryBankCard(
                    bank = bank,
                    hideBalances = hideBalances,
                    onClick = { onEditBankClick(bank) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Visual premium credit card style
@Composable
fun LuxuryBankCard(
    bank: BankAccount,
    hideBalances: Boolean,
    onClick: () -> Unit
) {
    val color = remember(bank.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(bank.colorHex))
        } catch (e: Exception) {
            Color(0xFF0284C7)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clickable { onClick() }
            .testTag("bank_card_${bank.id}"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(color, color.copy(alpha = 0.75f), color.copy(alpha = 0.5f))
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            text = bank.bankName.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = bank.nickname,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (bank.logoName) {
                                "wallet" -> Icons.Default.Wallet
                                "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
                                else -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text(
                            text = "BALANCE",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (hideBalances) "••••" else "₹ ${String.format("%,.2f", bank.balance)}",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ACCOUNT TYPE",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = bank.accountType,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "•••• ${bank.last4Digits}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// 5. Payment Methods Tab
@Composable
fun PaymentMethodsTab(
    paymentMethods: List<PaymentMethod>,
    onAddPaymentClick: () -> Unit,
    onEditPaymentClick: (PaymentMethod) -> Unit,
    onFavoriteToggle: (PaymentMethod) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Payment Methods",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onAddPaymentClick,
                    modifier = Modifier.testTag("add_payment_method_button")
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Custom")
                }
            }
        }

        if (paymentMethods.isEmpty()) {
            item {
                EmptyStateWidget(
                    title = "No payment methods",
                    desc = "Configure cash, cards, and UPI channels."
                )
            }
        } else {
            items(paymentMethods) { pm ->
                PaymentMethodRowItem(
                    pm = pm,
                    onClick = { onEditPaymentClick(pm) },
                    onFavClick = { onFavoriteToggle(pm) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PaymentMethodRowItem(
    pm: PaymentMethod,
    onClick: () -> Unit,
    onFavClick: () -> Unit
) {
    val color = remember(pm.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(pm.colorHex))
        } catch (e: Exception) {
            Color(0xFF3B82F6)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("payment_card_${pm.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (pm.iconName) {
                        "payments" -> Icons.Default.Payments
                        "qr_code" -> Icons.Default.QrCode
                        "wallet" -> Icons.Default.Wallet
                        else -> Icons.Default.CreditCard
                    },
                    contentDescription = null,
                    tint = color
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pm.nickname,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (pm.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = "${pm.type} • Linked: ${pm.linkedBankName.ifEmpty { "None" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (pm.upiId.isNotEmpty()) {
                    Text(
                        text = pm.upiId,
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onFavClick) {
                Icon(
                    imageVector = if (pm.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Toggle Favorite",
                    tint = if (pm.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// 6. Balance Adjustment History Log Tab (Section 5)
@Composable
fun BalanceHistoryTab(
    adjustments: List<BalanceAdjustment>,
    hideBalances: Boolean
) {
    if (adjustments.isEmpty()) {
        EmptyStateWidget(
            title = "No history log yet",
            desc = "Adjustments and manual transfers will be logged here."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Adjustment Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(adjustments) { adj ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjustment_log_${adj.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (adj.type) {
                                        "Increase" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        "Decrease" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        "Transfer" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (adj.type) {
                                    "Increase" -> Icons.Default.ArrowUpward
                                    "Decrease" -> Icons.Default.ArrowDownward
                                    "Transfer" -> Icons.Default.SwapHoriz
                                    else -> Icons.Default.SyncAlt
                                },
                                contentDescription = null,
                                tint = when (adj.type) {
                                    "Increase" -> MaterialTheme.colorScheme.primary
                                    "Decrease" -> MaterialTheme.colorScheme.error
                                    "Transfer" -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.tertiary
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (adj.type) {
                                    "Transfer" -> "${adj.fromAccountName} ➔ ${adj.toAccountName}"
                                    else -> "${adj.fromAccountName} balance offset"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Reason: ${adj.reason}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(adj.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Text(
                            text = "${if (adj.type == "Decrease" || adj.type == "Transfer") "-" else "+"}₹${if (hideBalances) "••" else String.format("%.2f", adj.amount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Black,
                            color = when (adj.type) {
                                "Increase" -> MaterialTheme.colorScheme.primary
                                "Decrease" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// 7. Balance Adjustment Dialog (Section 5 Action Engine)
@Composable
fun BalanceAdjustmentDialog(
    accounts: List<BankAccount>,
    onDismiss: () -> Unit,
    onConfirm: (type: String, accountId: Int, amount: Double, reason: String, notes: String, targetId: Int?) -> Unit
) {
    var type by remember { mutableStateOf("Increase") } // "Increase", "Decrease", "Correction", "Transfer"
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 0) }
    var targetAccountId by remember { mutableStateOf(accounts.getOrNull(1)?.id) }
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Adjust Wallet Balance", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector Row
                val types = listOf("Increase", "Decrease", "Correction", "Transfer")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }

                // Account Selection
                ConfigDropdown(
                    label = if (type == "Transfer") "Source Account" else "Account to Adjust",
                    selected = accounts.find { it.id == accountId }?.nickname ?: "Select Account",
                    options = accounts.map { it.nickname },
                    onSelect = { name ->
                        accountId = accounts.find { it.nickname == name }?.id ?: 0
                    }
                )

                // Target Account Selection (Only for Transfer)
                if (type == "Transfer") {
                    ConfigDropdown(
                        label = "Destination Account",
                        selected = accounts.find { it.id == targetAccountId }?.nickname ?: "Select Destination",
                        options = accounts.filter { it.id != accountId }.map { it.nickname },
                        onSelect = { name ->
                            targetAccountId = accounts.find { it.nickname == name }?.id
                        }
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Reason
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Adjustment") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (accountId != 0 && amount > 0 && reason.isNotEmpty()) {
                        onConfirm(type, accountId, amount, reason, notes, targetAccountId)
                    }
                },
                enabled = amountStr.toDoubleOrNull() != null && reason.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// 8. Bank Account Creation & Editing Bottom Dialog (CRUD - Section 2)
@Composable
fun BankUpsertDialog(
    bank: BankAccount?,
    onDismiss: () -> Unit,
    onSave: (BankAccount) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var bankName by remember { mutableStateOf(bank?.bankName ?: "") }
    var nickname by remember { mutableStateOf(bank?.nickname ?: "") }
    var last4Digits by remember { mutableStateOf(bank?.last4Digits ?: "") }
    var balanceStr by remember { mutableStateOf(bank?.balance?.toString() ?: "") }
    var upiId by remember { mutableStateOf(bank?.upiId ?: "") }
    var accountType by remember { mutableStateOf(bank?.accountType ?: "Savings") }
    var colorHex by remember { mutableStateOf(bank?.colorHex ?: "#0284C7") }
    var notes by remember { mutableStateOf(bank?.notes ?: "") }
    var status by remember { mutableStateOf(bank?.status ?: "Active") }
    var isArchived by remember { mutableStateOf(bank?.isArchived ?: false) }

    val colors = listOf("#0284C7", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899", "#34D399")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (bank == null) "Add Bank Account" else "Edit Account Details", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name (e.g. SBI, HDFC)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (e.g. Savings Primary)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = last4Digits,
                    onValueChange = { last4Digits = it },
                    label = { Text("Last 4 digits") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("Current Balance (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ConfigDropdown(
                    label = "Account Type",
                    selected = accountType,
                    options = listOf("Savings", "Current", "Wallet"),
                    onSelect = { accountType = it }
                )

                ConfigDropdown(
                    label = "Status",
                    selected = status,
                    options = listOf("Active", "Inactive", "Default"),
                    onSelect = { status = it }
                )

                // Theme color selection
                Text("Theme Color", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (colorHex == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Custom Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceStr.toDoubleOrNull() ?: 0.0
                    val updated = BankAccount(
                        id = bank?.id ?: 0,
                        bankName = bankName,
                        nickname = nickname,
                        last4Digits = last4Digits,
                        balance = bal,
                        colorHex = colorHex,
                        upiId = upiId,
                        accountType = accountType,
                        notes = notes,
                        status = status,
                        isArchived = isArchived,
                        logoName = if (accountType == "Wallet") "wallet" else "account_balance"
                    )
                    onSave(updated)
                },
                enabled = bankName.isNotBlank() && nickname.isNotBlank() && balanceStr.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (bank != null && onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// 9. Payment Method upsert
@Composable
fun PaymentMethodUpsertDialog(
    payment: PaymentMethod?,
    banks: List<BankAccount>,
    onDismiss: () -> Unit,
    onSave: (PaymentMethod) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(payment?.type ?: "UPI") }
    var nickname by remember { mutableStateOf(payment?.nickname ?: "") }
    var colorHex by remember { mutableStateOf(payment?.colorHex ?: "#3B82F6") }
    var upiId by remember { mutableStateOf(payment?.upiId ?: "") }
    var linkedBankName by remember { mutableStateOf(payment?.linkedBankName ?: "") }
    var isFavorite by remember { mutableStateOf(payment?.isFavorite ?: false) }
    var qrImageUri by remember { mutableStateOf(payment?.qrImageUri ?: "") }

    val colors = listOf("#3B82F6", "#8B5CF6", "#10B981", "#EF4444", "#F59E0B", "#EC4899", "#14B8A6")

    val qrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { qrImageUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (payment == null) "Add Custom Payment" else "Edit Payment Method", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConfigDropdown(
                    label = "Payment Type",
                    selected = type,
                    options = listOf("UPI", "Cash", "Credit Card", "Debit Card", "Google Pay", "PhonePe", "Paytm", "Wallet", "Custom"),
                    onSelect = { type = it }
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (e.g. Sathwik Personal UPI)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (type == "UPI" || type == "Google Pay" || type == "PhonePe" || type == "Paytm") {
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("QR Code Config (Section 4)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = { qrLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (qrImageUri.isNotEmpty()) "Change QR Image" else "Upload UPI QR Graphic")
                    }
                    if (qrImageUri.isNotEmpty()) {
                        Text("QR graphic selected", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                ConfigDropdown(
                    label = "Link to Bank Account",
                    selected = linkedBankName.ifEmpty { "None" },
                    options = listOf("None") + banks.map { it.nickname },
                    onSelect = { name ->
                        linkedBankName = if (name == "None") "" else name
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = isFavorite, onCheckedChange = { isFavorite = it })
                    Text("Favorite payment method")
                }

                Text("Icon Color Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (colorHex == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = PaymentMethod(
                        id = payment?.id ?: 0,
                        type = type,
                        nickname = nickname,
                        colorHex = colorHex,
                        upiId = upiId,
                        linkedBankName = linkedBankName,
                        isFavorite = isFavorite,
                        isCustom = payment?.isCustom ?: true,
                        qrImageUri = qrImageUri,
                        iconName = when (type) {
                            "UPI", "Google Pay", "PhonePe", "Paytm" -> "qr_code"
                            "Cash" -> "payments"
                            "Wallet" -> "wallet"
                            else -> "credit_card"
                        }
                    )
                    onSave(updated)
                },
                enabled = nickname.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (payment != null && onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// 10. Swipeable UPI QR Codes Bottom Sheet (Section 4)
@Composable
fun MyQrCodesBottomSheet(
    paymentMethods: List<PaymentMethod>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("My UPI QR Codes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            if (paymentMethods.isEmpty()) {
                EmptyStateWidget(
                    title = "No QR codes configured",
                    desc = "Edit any UPI payment method to link a QR Graphic or enter a UPI address."
                )
            } else {
                var index by remember { mutableStateOf(0) }
                val pm = paymentMethods[index]
                val color = remember(pm.colorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(pm.colorHex))
                    } catch (e: Exception) {
                        Color(0xFF8B5CF6)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // QR visual container card
                    Card(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (pm.qrImageUri.isNotEmpty()) {
                                AsyncImage(
                                    model = pm.qrImageUri,
                                    contentDescription = "UPI QR code",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                )
                            } else {
                                // Dynamic premium QR placeholder mockup
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(100.dp)
                                    )
                                    Text(
                                        text = "SCAN TO PAY",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // UPI Info
                    Text(
                        text = pm.nickname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = pm.upiId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Linked Bank: ${pm.linkedBankName.ifEmpty { "None" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )

                    // Page controls
                    if (paymentMethods.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (index > 0) index-- },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowBackIosNew, null)
                            }
                            Text("${index + 1} of ${paymentMethods.size}", style = MaterialTheme.typography.bodyLarge)
                            IconButton(
                                onClick = { if (index < paymentMethods.size - 1) index++ },
                                enabled = index < paymentMethods.size - 1
                            ) {
                                Icon(Icons.Default.ArrowForwardIos, null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// 11. Empty state component
@Composable
fun EmptyStateWidget(
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
