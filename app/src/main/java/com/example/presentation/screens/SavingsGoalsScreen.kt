package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.VaultFlowApplication
import com.example.domain.model.SavingsGoal
import com.example.presentation.viewmodel.SavingsGoalViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
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

    val goals by viewModel.allGoals.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val vaultState by viewModel.vaultState.collectAsState()
    val vaultHistory by viewModel.vaultHistory.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    // Screen State
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("All") } // "All", "Active", "Completed", "Paused"
    var selectedGoalForDetail by remember { mutableStateOf<SavingsGoal?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Dialog state for active goal
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Confetti / completion celebration triggers
    var triggerCelebration by remember { mutableStateOf(false) }
    var celebratedGoalName by remember { mutableStateOf("") }

    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    // Filtered goals
    val filteredGoals = goals.filter { goal ->
        val matchesSearch = goal.name.contains(searchQuery, ignoreCase = true) ||
                goal.priority.contains(searchQuery, ignoreCase = true) ||
                goal.category.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == "All" || goal.status.equals(selectedStatusFilter, ignoreCase = true)
        matchesSearch && matchesStatus
    }

    // Stats calculations
    val totalGoalsCount = goals.size
    val activeGoalsCount = goals.count { it.status == "Active" }
    val completedGoalsCount = goals.count { it.status == "Completed" }
    val totalSavedMoney = goals.sumOf { it.currentSavedAmount }
    val averageSavingProgress = if (goals.isNotEmpty()) {
        goals.map { if (it.targetAmount > 0) it.currentSavedAmount / it.targetAmount else 0.0 }.average() * 100
    } else 0.0

    // Fastest goal estimate (closest to target by days remaining)
    val fastestGoal = goals.filter { it.status == "Active" && it.targetAmount > it.currentSavedAmount }
        .minByOrNull { it.targetDate }

    // Largest goal (highest target amount)
    val largestGoal = goals.maxByOrNull { it.targetAmount }

    // Floating notifications
    var currentNotification by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(goals) {
        // Trigger smart notifications based on goal updates
        val closeToCompletion = goals.find { it.status == "Active" && it.targetAmount > 0 && (it.currentSavedAmount / it.targetAmount) >= 0.9 }
        if (closeToCompletion != null) {
            currentNotification = "Goal '${closeToCompletion.name}' is almost completed (over 90% saved)!"
            delay(5000)
            currentNotification = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Smart Savings Goals",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("goals_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_goal_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Savings Goal")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("goals_list_column"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Notifications / Smart Tips bar
                currentNotification?.let { note ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Tips",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // 1. Goal Statistics Overview Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Goal Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Money Saved", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text(currencyFormatter.format(totalSavedMoney), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Active / Done", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    Text("$activeGoalsCount active / $completedGoalsCount completed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (fastestGoal != null) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Fastest Goal Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(fastestGoal.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Due: " + SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(fastestGoal.targetDate)), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            if (largestGoal != null) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Largest Goal Target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        Text(largestGoal.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(currencyFormatter.format(largestGoal.targetAmount), fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Interactive Savings Calendar Representation
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Savings & Activity Calendar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Monthly Goal Progress Tracker",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                // Draw a calendar grid containing custom savings achievements
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                                    days.forEach { day ->
                                        Text(
                                            text = day,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                // Render pseudo calendar dots representing Deposits (Green), Withdrawals (Red), Milestones (Blue)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (week in 0..2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            for (day in 1..7) {
                                                val dayNum = week * 7 + day
                                                // Map highlights for visual interest
                                                val circleColor = when (dayNum) {
                                                    3, 10, 17 -> Color(0xFF10B981) // Green Deposit
                                                    7 -> Color(0xFFEF4444) // Red Withdraw
                                                    15 -> Color(0xFF3B82F6) // Blue Completion
                                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(circleColor)
                                                        .clickable {
                                                            currentNotification = when (dayNum) {
                                                                3, 10, 17 -> "Interactive Calendar: Deposit logged on this date!"
                                                                7 -> "Interactive Calendar: Withdraw warning generated on this date."
                                                                15 -> "Interactive Calendar: Milestone achieved! Completed Trip savings goal."
                                                                else -> "Interactive Calendar: Normal day. Consistent savings flow!"
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$dayNum",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (dayNum in listOf(3, 7, 10, 15, 17)) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Deposit", fontSize = 10.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Withdraw", fontSize = 10.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Completed", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Search and Status Filtering Block
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search goals, priority, category...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goal_search_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        // Status Filter chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filters = listOf("All", "Active", "Completed", "Paused", "Cancelled")
                            items(filters) { filter ->
                                FilterChip(
                                    selected = selectedStatusFilter == filter,
                                    onClick = { selectedStatusFilter = filter },
                                    label = { Text(filter) }
                                )
                            }
                        }
                    }
                }

                // 4. Goals list rendering
                if (filteredGoals.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = "No goals",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No savings goals found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            }
                        }
                    }
                } else {
                    items(filteredGoals) { goal ->
                        val progress = if (goal.targetAmount > 0) (goal.currentSavedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                        val percent = (progress * 100).toInt()

                        val borderBrush = if (goal.status == "Completed") {
                            Brush.sweepGradient(listOf(Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF10B981)))
                        } else {
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant))
                        }

                        Card(
                            onClick = { selectedGoalForDetail = goal },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(2.dp, borderBrush),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goal_card_${goal.id}")
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(goal.icon.ifBlank { "🎯" }, fontSize = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = goal.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Category: ${goal.category}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    // Priority Badges
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (goal.priority) {
                                                    "High" -> Color(0xFFEF4444).copy(alpha = 0.1f)
                                                    "Medium" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                                                    else -> Color(0xFF10B981).copy(alpha = 0.1f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = goal.priority,
                                            color = when (goal.priority) {
                                                "High" -> Color(0xFFEF4444)
                                                "Medium" -> Color(0xFFF59E0B)
                                                else -> Color(0xFF10B981)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Progress bars
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${currencyFormatter.format(goal.currentSavedAmount)} saved",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${currencyFormatter.format(goal.targetAmount)} target ($percent%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (goal.status == "Completed") Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Status: ${goal.status}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (goal.status) {
                                            "Completed" -> Color(0xFF10B981)
                                            "Paused" -> Color(0xFFF59E0B)
                                            "Cancelled" -> Color(0xFFEF4444)
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Simple Deposit Quick Action
                                        Button(
                                            onClick = {
                                                selectedGoalForDetail = goal
                                                showDepositDialog = true
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Deposit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (goal.currentSavedAmount > 0.0) {
                                            Button(
                                                onClick = {
                                                    selectedGoalForDetail = goal
                                                    showWithdrawDialog = true
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Withdraw", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CONFETTI CELEBRATION OVERLAY
        AnimatedVisibility(
            visible = triggerCelebration,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { triggerCelebration = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🎉", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Goal Completed!",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Congratulations! You have fully funded: '$celebratedGoalName'",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { triggerCelebration = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Keep Saving!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Selected Goal Detail Sheet overlay (High-fidelity custom sheet)
        selectedGoalForDetail?.let { goal ->
            val plannerInfo = viewModel.calculatePlannerInfo(goal.targetAmount, goal.currentSavedAmount, goal.targetDate)

            // Make detail overlay visible if none of dialogs are active
            if (!showDepositDialog && !showWithdrawDialog && !showTransferDialog) {
                AlertDialog(
                    onDismissRequest = { selectedGoalForDetail = null },
                    confirmButton = {
                        TextButton(onClick = { selectedGoalForDetail = null }) {
                            Text("Done")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(goal.icon.ifBlank { "🎯" }, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(goal.name, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Animated Circular Progress display
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val progress = if (goal.targetAmount > 0) (goal.currentSavedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                                    Box(contentAlignment = Alignment.Center) {
                                        Canvas(modifier = Modifier.size(110.dp)) {
                                            drawArc(
                                                color = Color.LightGray.copy(alpha = 0.2f),
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = 12.dp.toPx())
                                            )
                                            drawArc(
                                                color = Color(0xFF3D5AFE),
                                                startAngle = -90f,
                                                sweepAngle = progress * 360f,
                                                useCenter = false,
                                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            Text("Completed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                }

                                // AI Savings Planner suggestions (Section 6)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("🤖 AI Assistant Planner Advice", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(plannerInfo.aiSuggestion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Daily requirement: ${currencyFormatter.format(plannerInfo.dailyTarget)}", fontSize = 11.sp)
                                        Text("Weekly requirement: ${currencyFormatter.format(plannerInfo.weeklyTarget)}", fontSize = 11.sp)
                                        Text("Monthly requirement: ${currencyFormatter.format(plannerInfo.monthlyTarget)}", fontSize = 11.sp)
                                        Text("Target Days remaining: ${plannerInfo.daysLeft} days", fontSize = 11.sp)
                                    }
                                }

                                // Timeline and actions
                                Text("Goal Timeline & Actions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(onClick = { showDepositDialog = true }, modifier = Modifier.weight(1f)) {
                                        Text("Deposit")
                                    }
                                    Button(
                                        onClick = { showWithdrawDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Withdraw")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showTransferDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Goal-to-Goal")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.updateGoalStatus(goal, if (goal.status == "Paused") "Active" else "Paused")
                                            selectedGoalForDetail = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (goal.status == "Paused") "Resume" else "Pause")
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.deleteGoal(goal)
                                        selectedGoalForDetail = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete Goal permanently")
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Timeline Events", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Goal Created: " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(goal.createdDate)), fontSize = 11.sp)
                                        }
                                        if (goal.currentSavedAmount > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2196F3))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Latest savings deposits logged successfully.", fontSize = 11.sp)
                                            }
                                        }
                                        if (goal.status == "Completed") {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Goal completed successfully!", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        // DEPOSIT DIALOG (Section 4)
        if (showDepositDialog && selectedGoalForDetail != null) {
            val goal = selectedGoalForDetail!!
            var depositAmount by remember { mutableStateOf("") }
            var depositSource by remember { mutableStateOf("Cash") } // "Cash", "Bank Account", "Private Vault"
            var selectedBankId by remember { mutableStateOf<Int?>(null) }
            var depositNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showDepositDialog = false },
                title = { Text("Deposit to ${goal.name}") },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = depositAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                viewModel.depositToGoal(goal, amount, depositSource, selectedBankId, depositNotes)
                                if (amount + goal.currentSavedAmount >= goal.targetAmount) {
                                    celebratedGoalName = goal.name
                                    triggerCelebration = true
                                }
                                showDepositDialog = false
                                selectedGoalForDetail = null
                            }
                        }
                    ) {
                        Text("Deposit")
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
                            label = { Text("Amount (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Fund Source:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "Bank Account", "Private Vault").forEach { src ->
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

                        if (depositSource == "Private Vault") {
                            Text("Secure Vault balance: ${currencyFormatter.format(vaultState.balance)}", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        TextField(
                            value = depositNotes,
                            onValueChange = { depositNotes = it },
                            label = { Text("Notes (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }

        // WITHDRAW DIALOG WITH WARN & CONFIRMATION (Section 5)
        if (showWithdrawDialog && selectedGoalForDetail != null) {
            val goal = selectedGoalForDetail!!
            var withdrawAmount by remember { mutableStateOf("") }
            var withdrawDest by remember { mutableStateOf("Cash") } // "Cash", "Bank Account", "Private Vault"
            var selectedBankId by remember { mutableStateOf<Int?>(null) }
            var withdrawReason by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showWithdrawDialog = false },
                title = { Text("Withdraw from ${goal.name}") },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = withdrawAmount.toDoubleOrNull() ?: 0.0
                            if (amount > 0 && withdrawReason.isNotBlank()) {
                                viewModel.withdrawFromGoal(goal, amount, withdrawDest, selectedBankId, withdrawReason)
                                showWithdrawDialog = false
                                selectedGoalForDetail = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Confirm Withdrawal")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWithdrawDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // WARNING (Section 5)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "WARNING: You are using money reserved for your goal. Doing so will delay your target completion date!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        TextField(
                            value = withdrawAmount,
                            onValueChange = { withdrawAmount = it },
                            label = { Text("Amount to withdraw (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Destination Account:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Cash", "Bank Account", "Private Vault").forEach { dest ->
                                FilterChip(
                                    selected = withdrawDest == dest,
                                    onClick = { withdrawDest = dest },
                                    label = { Text(dest) }
                                )
                            }
                        }

                        if (withdrawDest == "Bank Account") {
                            Text("Select Target Bank Account:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            value = withdrawReason,
                            onValueChange = { withdrawReason = it },
                            label = { Text("Reason for premature withdrawal") },
                            placeholder = { Text("e.g. medical emergency, laptop repair") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }

        // TRANSFER BETWEEN GOALS DIALOG
        if (showTransferDialog && selectedGoalForDetail != null) {
            val sourceGoal = selectedGoalForDetail!!
            var transferAmount by remember { mutableStateOf("") }
            var selectedDestGoalId by remember { mutableStateOf<Int?>(null) }
            var transferNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showTransferDialog = false },
                title = { Text("Transfer Between Goals") },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = transferAmount.toDoubleOrNull() ?: 0.0
                            val destGoal = goals.find { it.id == selectedDestGoalId }
                            if (amount > 0 && destGoal != null) {
                                viewModel.transferBetweenGoals(sourceGoal, destGoal, amount, transferNotes)
                                showTransferDialog = false
                                selectedGoalForDetail = null
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
                        Text("Source: ${sourceGoal.name} (Saved: ${currencyFormatter.format(sourceGoal.currentSavedAmount)})", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        TextField(
                            value = transferAmount,
                            onValueChange = { transferAmount = it },
                            label = { Text("Amount (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Select Destination Goal:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        LazyColumn(modifier = Modifier.height(100.dp)) {
                            items(goals.filter { it.id != sourceGoal.id }) { goal ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedDestGoalId = goal.id }
                                        .padding(8.dp)
                                ) {
                                    RadioButton(selected = selectedDestGoalId == goal.id, onClick = { selectedDestGoalId = goal.id })
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

        // CREATE GOAL DIALOG
        if (showCreateDialog) {
            var name by remember { mutableStateOf("") }
            var targetAmount by remember { mutableStateOf("") }
            var daysRemaining by remember { mutableStateOf("30") }
            var priority by remember { mutableStateOf("Medium") }
            var notes by remember { mutableStateOf("") }
            var categoryName by remember { mutableStateOf("General") }
            var iconSymbol by remember { mutableStateOf("🎯") }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create New Savings Goal") },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = targetAmount.toDoubleOrNull() ?: 0.0
                            val days = daysRemaining.toLongOrNull() ?: 30L
                            if (name.isNotBlank() && target > 0) {
                                val targetDateMs = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)
                                viewModel.createGoal(name, iconSymbol, "", target, targetDateMs, priority, notes, categoryName)
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Goal Name (e.g. Electric Kettle)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextField(
                            value = targetAmount,
                            onValueChange = { targetAmount = it },
                            label = { Text("Target Amount (₹)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextField(
                            value = daysRemaining,
                            onValueChange = { daysRemaining = it },
                            label = { Text("Days to Complete (e.g. 15)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Priority:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Low", "Medium", "High").forEach { p ->
                                FilterChip(
                                    selected = priority == p,
                                    onClick = { priority = p },
                                    label = { Text(p) }
                                )
                            }
                        }

                        Text("Icon Emoji / Symbol:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("🎯", "💻", "📱", "✈️", "☕", "📸", "🚲").forEach { icon ->
                                FilterChip(
                                    selected = iconSymbol == icon,
                                    onClick = { iconSymbol = icon },
                                    label = { Text(icon) }
                                )
                            }
                        }

                        TextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            label = { Text("Associated Category") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Description / Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    }
}
