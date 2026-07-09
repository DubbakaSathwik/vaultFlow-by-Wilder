package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.VaultFlowApplication
import com.example.domain.model.BorrowLendItem
import com.example.domain.model.RecurringItem
import com.example.domain.model.SavingsGoal
import com.example.domain.model.Transaction
import com.example.presentation.viewmodel.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as VaultFlowApplication
    val appContainer = context.container
    val viewModel: CalendarViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(appContainer.vaultRepository) as T
            }
        }
    )

    val selectedDate by viewModel.selectedDate.collectAsState()
    var viewMode by remember { mutableStateOf("Month") } // "Week", "Month", "Year"
    var showDayDetailsSheet by remember { mutableStateOf(false) }

    // Aggregate values
    val transactions by viewModel.transactions.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val borrowLendItems by viewModel.borrowLendItems.collectAsState()
    val recurringItems by viewModel.recurringItems.collectAsState()

    val currentMonthYearString = remember(selectedDate) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(selectedDate.time)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                title = { Text("Financial Calendar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    // Switch views
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Week", "Month", "Year").forEach { mode ->
                            val isSelected = viewMode == mode
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.clickable { viewMode = mode }
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Month switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val prev = (selectedDate.clone() as Calendar).apply {
                        if (viewMode == "Year") add(Calendar.YEAR, -1)
                        else add(Calendar.MONTH, -1)
                    }
                    viewModel.selectDate(prev)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }

                Text(
                    text = if (viewMode == "Year") {
                        SimpleDateFormat("yyyy", Locale.getDefault()).format(selectedDate.time)
                    } else {
                        currentMonthYearString
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    val next = (selectedDate.clone() as Calendar).apply {
                        if (viewMode == "Year") add(Calendar.YEAR, 1)
                        else add(Calendar.MONTH, 1)
                    }
                    viewModel.selectDate(next)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }

            // Calendar Layouts
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                when (viewMode) {
                    "Week" -> WeekCalendarView(
                        selectedDate = selectedDate,
                        viewModel = viewModel,
                        onDayClick = {
                            viewModel.selectDate(it)
                            showDayDetailsSheet = true
                        }
                    )
                    "Month" -> MonthCalendarView(
                        selectedDate = selectedDate,
                        viewModel = viewModel,
                        onDayClick = {
                            viewModel.selectDate(it)
                            showDayDetailsSheet = true
                        }
                    )
                    "Year" -> YearCalendarView(
                        selectedDate = selectedDate,
                        onMonthClick = {
                            val newCal = (selectedDate.clone() as Calendar).apply {
                                set(Calendar.MONTH, it)
                            }
                            viewModel.selectDate(newCal)
                            viewMode = "Month"
                        }
                    )
                }
            }

            Divider()

            // Dynamic bottom content preview of Selected Day
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val year = selectedDate.get(Calendar.YEAR)
                val month = selectedDate.get(Calendar.MONTH)
                val day = selectedDate.get(Calendar.DAY_OF_MONTH)

                val dayTransactions = viewModel.getTransactionsForDay(year, month, day)
                val dayGoals = viewModel.getGoalsForDay(year, month, day)
                val dayBorrow = viewModel.getBorrowLendForDay(year, month, day)
                val dayRecurring = viewModel.getRecurringForDay(year, month, day)

                DailySummaryPreview(
                    date = selectedDate,
                    transactions = dayTransactions,
                    goals = dayGoals,
                    borrow = dayBorrow,
                    recurring = dayRecurring,
                    onViewFullDetails = { showDayDetailsSheet = true }
                )
            }
        }
    }

    // Interactive Bottom Sheet displaying Transactions, Receipts, Goals, Borrow, Recurring Payments, Daily Summary
    if (showDayDetailsSheet) {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val dayTransactions = viewModel.getTransactionsForDay(year, month, day)
        val dayGoals = viewModel.getGoalsForDay(year, month, day)
        val dayBorrow = viewModel.getBorrowLendForDay(year, month, day)
        val dayRecurring = viewModel.getRecurringForDay(year, month, day)

        ModalBottomSheet(
            onDismissRequest = { showDayDetailsSheet = false },
            modifier = Modifier.testTag("calendar_day_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(selectedDate.time),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showDayDetailsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Daily Balance Calculations
                    item {
                        val totalIncome = dayTransactions.filter { it.type == "Income" || it.type == "Borrow" }.sumOf { it.amount }
                        val totalExpense = dayTransactions.filter { it.type == "Expense" || it.type == "Lend" }.sumOf { it.amount }
                        val safeRemaining = totalIncome - totalExpense

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Daily Financial Balance", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Income/Inflow", fontSize = 11.sp)
                                        Text("₹$totalIncome", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Green)
                                    }
                                    Column {
                                        Text("Expenses/Outflow", fontSize = 11.sp)
                                        Text("₹$totalExpense", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Red)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Net Balance", fontSize = 11.sp)
                                        Text("₹$safeRemaining", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (safeRemaining >= 0) Color.Green else Color.Red)
                                    }
                                }
                            }
                        }
                    }

                    // Transactions block
                    if (dayTransactions.isNotEmpty()) {
                        item {
                            Text("Transactions & Receipts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        items(dayTransactions) { tx ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tx.merchantName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${tx.categoryName} • ${tx.paymentMethod}", fontSize = 11.sp)
                                        if (!tx.notes.isNullOrBlank()) {
                                            Text(tx.notes, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "₹${tx.amount}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.type == "Income" || tx.type == "Borrow") Color.Green else Color.Red
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (tx.type == "Income" || tx.type == "Borrow") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (tx.type == "Income" || tx.type == "Borrow") Color.Green else Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Goals block
                    if (dayGoals.isNotEmpty()) {
                        item {
                            Text("Savings Goals Milestones", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        items(dayGoals) { goal ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Target: ₹${goal.targetAmount}", fontSize = 11.sp)
                                    }
                                    Text("₹${goal.currentSavedAmount} saved", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }

                    // Borrow / Lend block
                    if (dayBorrow.isNotEmpty()) {
                        item {
                            Text("Borrow & Lend Records", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        items(dayBorrow) { record ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(record.personName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(if (record.type == "Borrowed") "Money I Borrowed" else "Money Others Owe Me", fontSize = 11.sp)
                                    }
                                    Text("₹${record.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }

                    // Recurring Payments block
                    if (dayRecurring.isNotEmpty()) {
                        item {
                            Text("Recurring Subscriptions & EMI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        items(dayRecurring) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Frequency: ${item.frequency}", fontSize = 11.sp)
                                    }
                                    Text("₹${item.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthCalendarView(
    selectedDate: Calendar,
    viewModel: CalendarViewModel,
    onDayClick: (Calendar) -> Unit
) {
    val year = selectedDate.get(Calendar.YEAR)
    val month = selectedDate.get(Calendar.MONTH)

    val monthCalendar = remember(selectedDate) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1

    val days = remember(selectedDate) {
        val list = mutableListOf<Calendar?>()
        // Padding for previous month
        for (i in 0 until firstDayOfWeek) {
            list.add(null)
        }
        // Month days
        for (i in 1..daysInMonth) {
            val dayCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, i)
            }
            list.add(dayCal)
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Week days headings
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            weekDays.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid cells
        val rows = (days.size + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (c in 0..6) {
                        val index = r * 7 + c
                        if (index < days.size) {
                            val dayCal = days[index]
                            if (dayCal != null) {
                                val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                                val isSelected = dayNum == selectedDate.get(Calendar.DAY_OF_MONTH)

                                // Fetch indicator status
                                val dayTxs = viewModel.getTransactionsForDay(year, month, dayNum)
                                val dayGoals = viewModel.getGoalsForDay(year, month, dayNum)
                                val dayBorrow = viewModel.getBorrowLendForDay(year, month, dayNum)
                                val dayRecurring = viewModel.getRecurringForDay(year, month, dayNum)

                                val hasIncome = dayTxs.any { it.type == "Income" || it.type == "Borrow" }
                                val hasExpense = dayTxs.any { it.type == "Expense" || it.type == "Lend" }
                                val hasGoal = dayGoals.isNotEmpty()
                                val hasBorrow = dayBorrow.isNotEmpty()
                                val hasRecurring = dayRecurring.isNotEmpty()

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable { onDayClick(dayCal) }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Dots indicators
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (hasIncome) Box(modifier = Modifier.size(4.dp).background(Color.Green, CircleShape))
                                            if (hasExpense) Box(modifier = Modifier.size(4.dp).background(Color.Red, CircleShape))
                                            if (hasGoal) Box(modifier = Modifier.size(4.dp).background(Color.Blue, CircleShape))
                                            if (hasBorrow) Box(modifier = Modifier.size(4.dp).background(Color(0xFF8E24AA), CircleShape)) // Purple
                                            if (hasRecurring) Box(modifier = Modifier.size(4.dp).background(Color.Yellow, CircleShape))
                                        }
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f))
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekCalendarView(
    selectedDate: Calendar,
    viewModel: CalendarViewModel,
    onDayClick: (Calendar) -> Unit
) {
    val weekDays = remember(selectedDate) {
        val list = mutableListOf<Calendar>()
        val cal = selectedDate.clone() as Calendar
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        for (i in 0..6) {
            list.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekDays.forEach { dayCal ->
            val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
            val isSelected = dayNum == selectedDate.get(Calendar.DAY_OF_MONTH)
            val year = dayCal.get(Calendar.YEAR)
            val month = dayCal.get(Calendar.MONTH)

            val dayTxs = viewModel.getTransactionsForDay(year, month, dayNum)
            val hasIncome = dayTxs.any { it.type == "Income" || it.type == "Borrow" }
            val hasExpense = dayTxs.any { it.type == "Expense" || it.type == "Lend" }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onDayClick(dayCal) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = SimpleDateFormat("EEE", Locale.getDefault()).format(dayCal.time),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (hasIncome) Box(modifier = Modifier.size(4.dp).background(Color.Green, CircleShape))
                        if (hasExpense) Box(modifier = Modifier.size(4.dp).background(Color.Red, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
fun YearCalendarView(
    selectedDate: Calendar,
    onMonthClick: (Int) -> Unit
) {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0..3) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (c in 0..2) {
                    val monthIndex = r * 3 + c
                    if (monthIndex < months.size) {
                        val isCurrentMonth = monthIndex == Calendar.getInstance().get(Calendar.MONTH) &&
                                selectedDate.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.5f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrentMonth) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .clickable { onMonthClick(monthIndex) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = months[monthIndex],
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrentMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailySummaryPreview(
    date: Calendar,
    transactions: List<Transaction>,
    goals: List<SavingsGoal>,
    borrow: List<BorrowLendItem>,
    recurring: List<RecurringItem>,
    onViewFullDetails: () -> Unit
) {
    val totalIncome = transactions.filter { it.type == "Income" || it.type == "Borrow" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "Expense" || it.type == "Lend" }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable { onViewFullDetails() },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(date.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to see full ledger items",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            TextButton(onClick = onViewFullDetails) {
                Text("Open Details")
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Income", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("₹$totalIncome", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Expenses", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("₹$totalExpense", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scheduled markers row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scheduled today:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            if (goals.isNotEmpty()) {
                SuggestionChip(onClick = {}, label = { Text("Savings target achieved (${goals.size})", fontSize = 10.sp) })
            }
            if (borrow.isNotEmpty()) {
                SuggestionChip(onClick = {}, label = { Text("Debt/loan due (${borrow.size})", fontSize = 10.sp) })
            }
            if (recurring.isNotEmpty()) {
                SuggestionChip(onClick = {}, label = { Text("Subscriptions active (${recurring.size})", fontSize = 10.sp) })
            }
            if (goals.isEmpty() && borrow.isEmpty() && recurring.isEmpty()) {
                Text("No goals, borrows or subscriptions due today.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}
