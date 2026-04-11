package com.expenseanalyst.feature.notification.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.domain.model.PendingNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingInboxScreen(
    onBack: () -> Unit,
    onAddExpense: (amount: Double, currency: String, merchant: String?, type: String, account: String?, pendingId: Long, paymentMethod: String?) -> Unit,
    viewModel: PendingInboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.pendingDismissId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDismiss,
            title = { Text("Dismiss transaction?") },
            text = { Text("This transaction has not been added to your expenses yet. Are you sure you want to dismiss it?") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDismiss,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Dismiss") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDismiss) { Text("Cancel") }
            }
        )
    }

    if (uiState.showDismissAllConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDismissAll,
            title = { Text("Clear all transactions?") },
            text = { Text("None of these transactions have been added to your expenses yet. Clearing will permanently remove all ${uiState.items.size} pending items.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDismissAll,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDismissAll) { Text("Cancel") }
            }
        )
    }

    if (uiState.pendingSaveBillId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelSaveBill,
            title = { Text("Add as New Bill?") },
            text = { Text("A new bill entry will be created. You can track payments against it from the Bills section.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSaveBill) {
                    Text("Add Bill", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelSaveBill) { Text("Cancel") }
            }
        )
    }

    if (uiState.pendingUpdateBillId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelUpdateBill,
            title = { Text("Update Existing Bill?") },
            text = { Text("The existing open bill will be updated with the latest amount and due date from this statement.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmUpdateBill) {
                    Text("Update Bill", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelUpdateBill) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Pending Inbox", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.items.isNotEmpty()) {
                        TextButton(onClick = viewModel::requestDismissAll) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (!uiState.isLoading && uiState.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No pending transactions",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Detected bank transactions appear here until you add or dismiss them",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    if (item.pendingType == "BILL") {
                        PendingBillItem(
                            item = item,
                            onSaveAsNew = { viewModel.requestSaveBill(item.id) },
                            onUpdate = { viewModel.requestUpdateBill(item.id) },
                            onDismiss = { viewModel.requestDismiss(item.id) }
                        )
                    } else {
                        PendingInboxItem(
                            item = item,
                            onAdd = {
                                val accountStr = item.accountLast4?.let { last4 ->
                                    val bank = item.bankName.takeIf { it != "Unknown Bank" } ?: ""
                                    if (bank.isNotBlank()) "$bank *$last4" else "*$last4"
                                }
                                onAddExpense(
                                    item.amount,
                                    item.currencyCode,
                                    item.merchantName,
                                    item.transactionType,
                                    accountStr,
                                    item.id,
                                    item.paymentMethod
                                )
                            },
                            onDismiss = { viewModel.requestDismiss(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingBillItem(
    item: PendingNotification,
    onSaveAsNew: () -> Unit,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeStr = remember(item.detectedAtMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            .format(Date(item.detectedAtMillis))
    }
    val dueDateStr = remember(item.dueDateMillis) {
        item.dueDateMillis?.let {
            "Due: " + SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        // "Bill Statement" badge
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Bill Statement",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.billerName ?: item.merchantName ?: "Unknown Biller",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.amount > 0) {
                            Text(
                                text = "%.2f %s".format(item.amount, item.currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (dueDateStr != null) {
                            Text(
                                text = dueDateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.linkedBillId != null) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Open bill found — can update",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Dismiss")
                }
                if (item.linkedBillId != null) {
                    Button(
                        onClick = onUpdate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Update Bill")
                    }
                } else {
                    Button(
                        onClick = onSaveAsNew,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add as Bill")
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingInboxItem(
    item: PendingNotification,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDebit = item.transactionType == "DEBIT"
    val isPayment = item.transactionType == "PAYMENT"
    val amountColor = when {
        isPayment -> Color(0xFF7C5CBF)
        isDebit -> MaterialTheme.colorScheme.error
        else -> Color(0xFF4CAF50)
    }
    val directionIcon = when {
        isPayment -> Icons.Default.Payment
        isDebit -> Icons.Default.ArrowUpward
        else -> Icons.Default.ArrowDownward
    }

    val timeStr = remember(item.detectedAtMillis) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            .format(Date(item.detectedAtMillis))
    }

    val accountStr = remember(item.bankName, item.accountLast4) {
        val bank = item.bankName.takeIf { it != "Unknown Bank" } ?: ""
        val last4 = item.accountLast4
        when {
            bank.isNotBlank() && last4 != null -> "$bank *$last4"
            bank.isNotBlank() -> bank
            last4 != null -> "*$last4"
            else -> null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = amountColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "%.2f %s".format(item.amount, item.currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                        val merchant = item.merchantName
                        if (!merchant.isNullOrBlank()) {
                            Text(
                                text = merchant,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (accountStr != null) {
                            Text(
                                text = accountStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.isPossibleDuplicate) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "⚠ Possible duplicate",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF57C00),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Dismiss")
                }
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (item.isPossibleDuplicate) "Add Anyway" else "Add Expense")
                }
            }
        }
    }
}
