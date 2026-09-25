package com.expenseanalyst.feature.loans.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.domain.model.LentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.item?.personName ?: "Loan Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.item?.status == LentStatus.PENDING) {
                        IconButton(onClick = { onEdit(loanId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    IconButton(onClick = viewModel::showDeleteDialog) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.item == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("Loan not found") }

            else -> {
                val item = uiState.item!!
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${item.currencyCode} ${"%.2f".format(item.amount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (item.status == LentStatus.PENDING) "Pending repayment" else "Settled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.status == LentStatus.PENDING)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    DetailRow("Person", item.personName)
                    DetailRow("Description", item.description)
                    DetailRow("Lent on", dateFormat.format(Date(item.lentDateMillis)))
                    item.reminderDatetimeMillis?.let { millis ->
                        DetailRow("Reminder", dateTimeFormat.format(Date(millis)))
                    }
                    item.settledDateMillis?.let { millis ->
                        DetailRow("Settled on", dateFormat.format(Date(millis)))
                    }

                    HorizontalDivider()

                    LoanLegsSection(
                        legs = uiState.legs,
                        principal = item.amount,
                        principalCurrency = item.currencyCode
                    )

                    HorizontalDivider()

                    if (item.status == LentStatus.PENDING) {
                        Button(
                            onClick = viewModel::showSettleDialog,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !uiState.isSaving
                        ) {
                            if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            else Text("Mark as Settled")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = viewModel::showReminderPicker,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null)
                                Text(" Set Reminder")
                            }
                            if (item.reminderDatetimeMillis != null) {
                                OutlinedButton(
                                    onClick = viewModel::clearReminder,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AlarmOff, contentDescription = null)
                                    Text(" Clear")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSettleDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideSettleDialog,
            title = { Text("Mark as Settled") },
            text = { Text("This closes the loan. It does not create a transaction — link the actual repayment from its own detail screen so it stays out of your Received total.") },
            confirmButton = {
                Button(onClick = viewModel::markSettled) { Text("Settle") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideSettleDialog) { Text("Cancel") }
            }
        )
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideDeleteDialog,
            title = { Text("Delete Loan") },
            text = { Text("This will remove this loan record. Transactions linked to it are not deleted, and stay excluded from Spent and Received until you unlink them.") },
            confirmButton = {
                Button(onClick = viewModel::delete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDeleteDialog) { Text("Cancel") }
            }
        )
    }

    if (uiState.showReminderPicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.item?.reminderDatetimeMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = viewModel::hideReminderPicker,
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { viewModel.setReminder(it) }
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = viewModel::hideReminderPicker) { Text("Cancel") } }
        ) { DatePicker(state = dateState) }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/**
 * The transactions linked to a loan, grouped into money lent out and money repaid. A loan can be
 * lent in several transfers and repaid in several, and its principal is stored independently of
 * these rows — so a loan whose lending predates the app's history still shows its repayments.
 * Totals are in the home currency because the two sides are often in different currencies.
 */
@Composable
private fun LoanLegsSection(
    legs: List<com.expenseanalyst.domain.model.Expense>,
    principal: Double,
    principalCurrency: String
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val lent = legs.filter { !com.expenseanalyst.domain.util.SpendClassifier.isLoanRepayment(it) }
    val repaid = legs.filter { com.expenseanalyst.domain.util.SpendClassifier.isLoanRepayment(it) }

    Text("Linked transactions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    if (legs.isEmpty()) {
        Text(
            text = "None yet. Open a transaction and choose \"Link to loan\" to attach money lent out or a repayment. " +
                "Recorded principal: $principalCurrency ${"%.2f".format(principal)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    listOf("Lent out" to lent, "Repaid" to repaid).forEach { (label, rows) ->
        if (rows.isEmpty()) return@forEach
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        rows.forEach { leg ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(leg.date.toEpochMilliseconds())),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${leg.currencyCode} ${"%.2f".format(leg.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
