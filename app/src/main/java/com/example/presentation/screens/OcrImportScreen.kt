package com.example.presentation.screens

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.domain.model.OcrHistory
import com.example.presentation.components.DeleteConfirmationDialog
import com.example.presentation.viewmodel.OcrUiState
import com.example.presentation.viewmodel.OcrViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrImportScreen(
    viewModel: OcrViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Trigger image pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectReceiptImage(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Smart Receipt Scanner", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.resetState() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is OcrUiState.Review) {
                        IconButton(onClick = { viewModel.clearImage() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Reset", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "OcrStateTransition"
            ) { state ->
                when (state) {
                    is OcrUiState.Idle -> {
                        IdleOcrView(
                            viewModel = viewModel,
                            onGalleryClick = { galleryLauncher.launch("image/*") }
                        )
                    }
                    is OcrUiState.Scanning -> {
                        ScanningOcrView()
                    }
                    is OcrUiState.Review -> {
                        ReviewOcrFormView(
                            viewModel = viewModel,
                            receipt = state.receipt,
                            onReplaceClick = { galleryLauncher.launch("image/*") }
                        )
                    }
                    is OcrUiState.Success -> {
                        SuccessOcrView(
                            onDoneClick = { viewModel.resetState() }
                        )
                    }
                    is OcrUiState.Error -> {
                        ErrorOcrView(
                            message = state.message,
                            onRetryClick = { viewModel.resetState() }
                        )
                    }
                }
            }

            // Dialogue: Duplicate transaction alert
            val showDuplicate by viewModel.showDuplicateWarning.collectAsState()
            val duplicateTx by viewModel.potentialDuplicate.collectAsState()
            if (showDuplicate && duplicateTx != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.showDuplicateWarning.value = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Alert", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Potential Duplicate Detected", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            Text("A transaction with similar attributes already exists in your vault:")
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Text("Merchant: ${duplicateTx!!.merchantName}", fontWeight = FontWeight.Bold)
                                    Text("Amount: ₹${duplicateTx!!.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Category: ${duplicateTx!!.categoryName}")
                                    Text("Notes: ${duplicateTx!!.notes}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Would you like to cancel, save this transaction anyway, or review the existing item?")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.showDuplicateWarning.value = false
                                viewModel.saveTransaction()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Save Anyway")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.showDuplicateWarning.value = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Dialogue: Merchant Alias mapping learning prompt
            val showLearningPrompt by viewModel.showMerchantLearningPrompt.collectAsState()
            val origMerch by viewModel.pendingOriginalMerchant.collectAsState()
            val corrMerch by viewModel.pendingCorrectedMerchant.collectAsState()
            if (showLearningPrompt) {
                AlertDialog(
                    onDismissRequest = { viewModel.rejectMerchantLearning() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = "Learn", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Learn Merchant Alias?", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text("You have mapped \"$origMerch\" to \"$corrMerch\" multiple times. Would you like VaultFlow to always map \"$origMerch\" to \"$corrMerch\" in the future?")
                    },
                    confirmButton = {
                        Button(onClick = { viewModel.acceptMerchantLearning() }) {
                            Text("Always Map")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.rejectMerchantLearning() }) {
                            Text("No, Keep Separate")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun IdleOcrView(
    viewModel: OcrViewModel,
    onGalleryClick: () -> Unit
) {
    val history by viewModel.filteredOcrHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var sortMode by remember { mutableStateOf("Newest") } // Newest, Oldest, Amount
    val sortedHistory = remember(history, sortMode) {
        when (sortMode) {
            "Oldest" -> history.sortedBy { it.importDate }
            "Amount" -> history.sortedByDescending { it.amount ?: 0.0 }
            else -> history.sortedByDescending { it.importDate }
        }
    }

    var selectedHistoryReceiptForPreview by remember { mutableStateOf<OcrHistory?>(null) }
    var ocrHistoryToDelete by remember { mutableStateOf<OcrHistory?>(null) }

    if (ocrHistoryToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteReceiptFromHistory(ocrHistoryToDelete!!)
                ocrHistoryToDelete = null
            },
            onDismiss = { ocrHistoryToDelete = null },
            title = "Delete OCR Receipt History",
            message = "Are you sure you want to delete this scanned receipt history for ${ocrHistoryToDelete!!.merchantName} of ₹${ocrHistoryToDelete!!.amount ?: 0.0}?"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_idle_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Interactive import cards
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gallery_import_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = onGalleryClick
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload Icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Upload Payment Screenshot",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Supports PhonePe, Google Pay, Paytm, and FamPay receipts.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 2. Quick App logos indicators
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Integrates with major UPI Apps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "PhonePe" to Color(0xFF5F259F),
                        "Google Pay" to Color(0xFF4285F4),
                        "Paytm" to Color(0xFF00B9F1),
                        "FamPay" to Color(0xFFE2E8F0)
                    ).forEach { (name, color) ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (name == "FamPay") MaterialTheme.colorScheme.onBackground else color
                            )
                        }
                    }
                }
            }
        }

        // 3. Receipt Gallery header & filters
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Receipt Gallery",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Newest", "Oldest", "Amount").forEach { mode ->
                            FilterChip(
                                selected = sortMode == mode,
                                onClick = { sortMode = mode },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search by merchant, amount, bank, or txn id...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 4. Receipts grid/list
        if (sortedHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = "Empty",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No receipts uploaded yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            "Upload payment screenshots to review and save transactions.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sortedHistory.forEach { ocrHistory ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedHistoryReceiptForPreview = ocrHistory }
                                .testTag("receipt_item_${ocrHistory.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left placeholder visual representing original receipt
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = "Doc",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ocrHistory.merchantName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ocrHistory.dateStr,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            text = ocrHistory.paymentApp,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${ocrHistory.amount ?: 0.0}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    IconButton(
                                        onClick = { ocrHistoryToDelete = ocrHistory },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Receipt Image Preview
    if (selectedHistoryReceiptForPreview != null) {
        Dialog(onDismissRequest = { selectedHistoryReceiptForPreview = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedHistoryReceiptForPreview!!.merchantName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedHistoryReceiptForPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Box containing receipt image or fallback decorative visual representation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "Receipt",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Transaction Receipt", fontWeight = FontWeight.Bold)
                            Text("Extracted amount: ₹${selectedHistoryReceiptForPreview!!.amount ?: 0.0}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Txn ID: ${selectedHistoryReceiptForPreview!!.transactionId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Text("Ref No: ${selectedHistoryReceiptForPreview!!.referenceNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Text("Bank Account: ${selectedHistoryReceiptForPreview!!.bankName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedHistoryReceiptForPreview = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
fun ScanningOcrView() {
    val infiniteTransition = rememberInfiniteTransition(label = "ScannerBeam")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPosition"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_scanning_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Receipt Shape with Laser scanner beam
            Box(
                modifier = Modifier
                    .size(width = 180.dp, height = 260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Mock text lines inside receipt
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(10.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(10.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                    Box(modifier = Modifier.fillMaxWidth(0.9f).height(10.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
                }

                // Laser Scan Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .graphicsLayer {
                            translationY = scanProgress * 240.dp.toPx()
                        }
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.primary,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Extracting Transaction Details...",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Running local, offline Google ML Kit OCR text engine...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ReviewOcrFormView(
    viewModel: OcrViewModel,
    receipt: com.example.domain.model.ExtractedReceipt,
    onReplaceClick: () -> Unit
) {
    // Fields binding
    val amount by viewModel.reviewAmount.collectAsState()
    val merchant by viewModel.reviewMerchant.collectAsState()
    val merchantAlias by viewModel.reviewMerchantAlias.collectAsState()
    val category by viewModel.reviewCategory.collectAsState()
    val paymentMethod by viewModel.reviewPaymentMethod.collectAsState()
    val bank by viewModel.reviewBank.collectAsState()
    val date by viewModel.reviewDate.collectAsState()
    val time by viewModel.reviewTime.collectAsState()
    val upiId by viewModel.reviewUpiId.collectAsState()
    val transactionId by viewModel.reviewTransactionId.collectAsState()
    val referenceNumber by viewModel.reviewReferenceNumber.collectAsState()
    val notes by viewModel.reviewNotes.collectAsState()
    val tags by viewModel.reviewTags.collectAsState()
    val type by viewModel.reviewTransactionType.collectAsState()

    // Confidences
    val merchConf by viewModel.merchantConfidence.collectAsState()
    val bankConf by viewModel.bankConfidence.collectAsState()
    val catConf by viewModel.categoryConfidence.collectAsState()
    val payConf by viewModel.paymentMethodConfidence.collectAsState()

    // Lists for selectors
    val accountsList by viewModel.bankAccounts.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val paymentsList by viewModel.paymentMethods.collectAsState()

    val queue by viewModel.extractedQueue.collectAsState()
    val queueIndex by viewModel.currentQueueIndex.collectAsState()

    var isReceiptPreviewExpanded by remember { mutableStateOf(false) }
    var amountInput by remember(amount) { mutableStateOf(if (amount == 0.0) "" else amount.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_review_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi-transaction queue banner
        if (queue.size > 1) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("multi_ocr_queue_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Multi-Transaction Review",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Item ${queueIndex + 1} of ${queue.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Button(
                            onClick = { viewModel.skipCurrentTransaction() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("skip_ocr_queue_button")
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Skip")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Skip", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 1. Image preview and quick actions
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                            .clickable { isReceiptPreviewExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "Scan icon",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to View Full Screenshot", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Extracted from ${receipt.paymentApp}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = onReplaceClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Replace")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Replace screenshot")
                        }

                        TextButton(
                            onClick = { viewModel.clearImage() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                }
            }
        }

        // 2. Outflow / Inflow switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
            ) {
                listOf("Outflow" to "Expense", "Inflow" to "Income").forEach { (label, value) ->
                    val isSelected = type == label
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) {
                                    if (label == "Outflow") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                } else Color.Transparent
                            )
                            .clickable { viewModel.reviewTransactionType.value = label }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) {
                                if (label == "Outflow") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // 3. Main metadata fields (Section 6 & 7)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Transaction Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Amount
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = {
                            amountInput = it
                            viewModel.reviewAmount.value = it.toDoubleOrNull() ?: 0.0
                        },
                        label = { Text("Amount (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("review_amount_field")
                    )

                    // Merchant Name
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { viewModel.reviewMerchant.value = it },
                        label = { Text("Original Merchant") },
                        trailingIcon = {
                            ConfidenceBadge(merchConf)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("review_merchant_field")
                    )

                    // Merchant Alias
                    OutlinedTextField(
                        value = merchantAlias,
                        onValueChange = { viewModel.reviewMerchantAlias.value = it },
                        label = { Text("Merchant Alias Mapping") },
                        placeholder = { Text("Normalise name (e.g. McDonald's)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("review_alias_field")
                    )

                    // Category Selector
                    OutlinedTextField(
                        value = category,
                        onValueChange = { viewModel.reviewCategory.value = it },
                        label = { Text("Category") },
                        trailingIcon = { ConfidenceBadge(catConf) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("review_category_field")
                    )

                    // Payment Method
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { viewModel.reviewPaymentMethod.value = it },
                        label = { Text("Payment Method") },
                        trailingIcon = { ConfidenceBadge(payConf) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("review_payment_field")
                    )

                    // Bank Account name
                    OutlinedTextField(
                        value = bank,
                        onValueChange = { viewModel.reviewBank.value = it },
                        label = { Text("Linked Bank") },
                        trailingIcon = { ConfidenceBadge(bankConf) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("review_bank_field")
                    )
                }
            }
        }

        // 4. Secondary reference metadata fields
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Metadata & References", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Date & Time Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { viewModel.reviewDate.value = it },
                            label = { Text("Date") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = { viewModel.reviewTime.value = it },
                            label = { Text("Time") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // UPI ID
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { viewModel.reviewUpiId.value = it },
                        label = { Text("Receiver UPI ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Transaction ID
                    OutlinedTextField(
                        value = transactionId,
                        onValueChange = { viewModel.reviewTransactionId.value = it },
                        label = { Text("Transaction ID") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Reference Number
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { viewModel.reviewReferenceNumber.value = it },
                        label = { Text("UPI Ref / UTR Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.reviewNotes.value = it },
                        label = { Text("Notes") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tags
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { viewModel.reviewTags.value = it },
                        label = { Text("Tags") },
                        placeholder = { Text("e.g. #food,#receipt") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 5. Submit Action
        item {
            Button(
                onClick = { viewModel.checkDuplicateBeforeSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_reviewed_transaction_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify & Save Transaction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    // Modal full-screen interactive preview
    if (isReceiptPreviewExpanded) {
        Dialog(onDismissRequest = { isReceiptPreviewExpanded = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Original Screenshot", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { isReceiptPreviewExpanded = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated zoomable/rotatable screenshot area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "Receipt",
                                modifier = Modifier.size(100.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Screenshot visual preview area", fontWeight = FontWeight.Bold)
                            Text("Full details processed below:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Visual text details
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Detected App: ${receipt.paymentApp}", fontWeight = FontWeight.Bold)
                                    Text("Raw text count: ${receipt.rawText.length} characters")
                                    Text("Calculated OCR Confidence: ${String.format("%.1f", receipt.confidence)}%")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { isReceiptPreviewExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Preview")
                    }
                }
            }
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Int) {
    val color = when {
        confidence >= 95 -> MaterialTheme.colorScheme.primary
        confidence >= 85 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bolt, contentDescription = "Confidence", tint = color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "$confidence%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun SuccessOcrView(
    onDoneClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_success_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Transaction Saved Successfully",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "The extracted receipt transaction has been verified, matched, mapped, and added to your financial vault.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onDoneClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Scan Another Screenshot", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ErrorOcrView(
    message: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ocr_error_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Extraction Failed",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onRetryClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Scanner Dashboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}
