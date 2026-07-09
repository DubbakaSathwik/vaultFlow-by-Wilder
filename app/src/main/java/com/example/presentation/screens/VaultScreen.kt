package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.VaultFlowApplication
import com.example.domain.model.BankAccount
import com.example.domain.model.SavingsGoal
import com.example.presentation.viewmodel.SavingsGoalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as VaultFlowApplication
    val appContainer = context.container
    val viewModel: SavingsGoalViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SavingsGoalViewModel(appContainer.vaultRepository) as T
            }
        }
    )

    val vaultState by viewModel.vaultState.collectAsState()
    val vaultHistory by viewModel.vaultHistory.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val goals by viewModel.allGoals.collectAsState()

    val isUnlocked by viewModel.isVaultUnlocked.collectAsState()

    // Pin State
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Dialog state
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isUnlocked) {
            // LOCK SCREEN VIEW (PIN Protection)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Secure Private Vault",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter vault PIN to unlock your hidden savings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                TextField(
                    value = enteredPin,
                    onValueChange = {
                        enteredPin = it
                        pinError = false
                        if (it.length == 4) {
                            if (viewModel.verifyPin(it)) {
                                enteredPin = ""
                            } else {
                                pinError = true
                                enteredPin = ""
                            }
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = pinError,
                    label = { Text("4-Digit PIN") },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                    modifier = Modifier
                        .width(180.dp)
                        .testTag("vault_pin_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                if (pinError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Invalid PIN code. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Default PIN: 1234",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(48.dp))
                TextButton(onClick = onBackClick) {
                    Text("Exit Lock Screen", color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            // UNLOCKED PRIVATE VAULT DASHBOARD
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Secure Private Vault",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.setVaultUnlocked(false) }) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock Vault", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("vault_unlocked_scroll"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Vault Balance Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_balance_card"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "HIDDEN SAVINGS BALANCE",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { viewModel.toggleHideVaultBalance() }
                                        ) {
                                            Icon(
                                                imageVector = if (vaultState.isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle hidden",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (vaultState.isBalanceHidden) "••••••" else currencyFormatter.format(vaultState.balance),
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "🔒 Securely hidden away from bank totals",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // 2. Action buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showDepositDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deposit")
                            }

                            Button(
                                onClick = { showWithdrawDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Withdraw")
                            }

                            Button(
                                onClick = { showTransferDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Transfer")
                            }
                        }
                    }

                    // 3. Vault History Log List
                    item {
                        Text(
                            text = "Vault Transaction History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (vaultHistory.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No vault logs recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    } else {
                        items(vaultHistory) { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (log.type) {
                                                        "Deposit" -> Color(0xFF10B981).copy(alpha = 0.12f)
                                                        "Withdraw" -> Color(0xFFEF4444).copy(alpha = 0.12f)
                                                        else -> Color(0xFF3B82F6).copy(alpha = 0.12f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (log.type) {
                                                    "Deposit" -> Icons.Default.ArrowUpward
                                                    "Withdraw" -> Icons.Default.ArrowDownward
                                                    else -> Icons.Default.Send
                                                },
                                                contentDescription = null,
                                                tint = when (log.type) {
                                                    "Deposit" -> Color(0xFF10B981)
                                                    "Withdraw" -> Color(0xFFEF4444)
                                                    else -> Color(0xFF3B82F6)
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = log.notes.ifEmpty { log.type },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = "Source: ${log.source} ➔ Dest: ${log.destination}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (log.type == "Withdraw" || log.type == "Transfer to Goal") "-${currencyFormatter.format(log.amount)}" else "+${currencyFormatter.format(log.amount)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (log.type == "Withdraw" || log.type == "Transfer to Goal") Color(0xFFEF4444) else Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // DEPOSIT DIALOG
        if (showDepositDialog) {
            var depositAmount by remember { mutableStateOf("") }
            var depositSource by remember { mutableStateOf("Cash") }
            var selectedBankId by remember { mutableStateOf<Int?>(null) }
            var depositNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showDepositDialog = false },
                title = { Text("Deposit Into Secure Vault") },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = depositAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                viewModel.depositToVault(amount, depositSource, selectedBankId, depositNotes)
                                showDepositDialog = false
                            }
                        }
                    ) {
                        Text("Confirm Deposit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDepositDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(
                            value = depositAmount,
                            onValueChange = { depositAmount = it },
                            label = { Text("Deposit Amount (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Fund Source:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "Bank Account").forEach { src ->
                                FilterChip(
                                    selected = depositSource == src,
                                    onClick = { depositSource = src },
                                    label = { Text(src) }
                                )
                            }
                        }

                        if (depositSource == "Bank Account") {
                            Text("Select Bank Account:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(bankAccounts) { account ->
                                    FilterChip(
                                        selected = selectedBankId == account.id,
                                        onClick = { selectedBankId = account.id },
                                        label = { Text(account.nickname) }
                                    )
                                }
                            }
                        }

                        TextField(
                            value = depositNotes,
                            onValueChange = { depositNotes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }

        // WITHDRAW DIALOG
        if (showWithdrawDialog) {
            var withdrawAmount by remember { mutableStateOf("") }
            var withdrawDest by remember { mutableStateOf("Cash") }
            var selectedBankId by remember { mutableStateOf<Int?>(null) }
            var withdrawNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showWithdrawDialog = false },
                title = { Text("Withdraw From Secure Vault") },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = withdrawAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                viewModel.withdrawFromVault(amount, withdrawDest, selectedBankId, withdrawNotes)
                                showWithdrawDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Withdraw")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWithdrawDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(
                            value = withdrawAmount,
                            onValueChange = { withdrawAmount = it },
                            label = { Text("Amount (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Withdrawal Destination:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "Bank Account").forEach { dest ->
                                FilterChip(
                                    selected = withdrawDest == dest,
                                    onClick = { withdrawDest = dest },
                                    label = { Text(dest) }
                                )
                            }
                        }

                        if (withdrawDest == "Bank Account") {
                            Text("Select Bank Account:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(bankAccounts) { account ->
                                    FilterChip(
                                        selected = selectedBankId == account.id,
                                        onClick = { selectedBankId = account.id },
                                        label = { Text(account.nickname) }
                                    )
                                }
                            }
                        }

                        TextField(
                            value = withdrawNotes,
                            onValueChange = { withdrawNotes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }

        // TRANSFER DIALOG (To goals)
        if (showTransferDialog) {
            var transferAmount by remember { mutableStateOf("") }
            var selectedGoalId by remember { mutableStateOf<Int?>(null) }
            var transferNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showTransferDialog = false },
                title = { Text("Transfer Vault Funds to Savings Goal") },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = transferAmount.toDoubleOrNull() ?: 0.0
                            val targetGoal = goals.find { it.id == selectedGoalId }
                            if (amount > 0 && targetGoal != null) {
                                viewModel.depositToGoal(targetGoal, amount, "Private Vault", null, transferNotes)
                                showTransferDialog = false
                            }
                        }
                    ) {
                        Text("Transfer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTransferDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(
                            value = transferAmount,
                            onValueChange = { transferAmount = it },
                            label = { Text("Transfer Amount (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Target Savings Goal:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        LazyColumn(modifier = Modifier.height(120.dp)) {
                            items(goals) { goal ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedGoalId = goal.id }
                                        .padding(8.dp)
                                ) {
                                    RadioButton(selected = selectedGoalId == goal.id, onClick = { selectedGoalId = goal.id })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(goal.name)
                                }
                            }
                        }

                        TextField(
                            value = transferNotes,
                            onValueChange = { transferNotes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }

        // VAULT SETTINGS / PIN CONFIG DIALOG
        if (showSettingsDialog) {
            var newPin by remember { mutableStateOf("") }
            var isPinEnabled by remember { mutableStateOf(vaultState.isPinEnabled) }

            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Vault Secure Protection Settings") },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPin.length == 4 || !isPinEnabled) {
                                viewModel.setVaultPin(if (isPinEnabled) newPin else "", isPinEnabled)
                                showSettingsDialog = false
                            }
                        }
                    ) {
                        Text("Save Config")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable PIN Protection", fontWeight = FontWeight.Bold)
                            Switch(checked = isPinEnabled, onCheckedChange = { isPinEnabled = it })
                        }

                        if (isPinEnabled) {
                            TextField(
                                value = newPin,
                                onValueChange = { if (it.length <= 4) newPin = it },
                                label = { Text("Configure New 4-Digit PIN") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            )
        }
    }
}
