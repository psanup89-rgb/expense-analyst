package com.expenseanalyst.feature.expenses.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    onBillClick: (Long) -> Unit = {},
    viewModel: BillsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val now = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bills", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddBillSheet) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill")
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val isEmpty = uiState.pendingBills.isEmpty() && uiState.settledBills.isEmpty()
        if (isEmpty) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No bills yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Bill statements from SMS will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.pendingBills.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Pending (${uiState.pendingBills.size})")
                }
                items(uiState.pendingBills, key = { it.bill.id }) { bwp ->
                    BillCard(
                        bwp = bwp,
                        now = now,
                        onClick = { onBillClick(bwp.bill.id) },
                        onDelete = { viewModel.deleteBill(bwp.bill.id) }
                    )
                }
            }
            if (uiState.settledBills.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("Settled (${uiState.settledBills.size})")
                }
                items(uiState.settledBills, key = { it.bill.id }) { bwp ->
                    BillCard(
                        bwp = bwp,
                        now = now,
                        onClick = { onBillClick(bwp.bill.id) },
                        onDelete = { viewModel.deleteBill(bwp.bill.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(88.dp)) } // FAB clearance
        }
    }

    if (uiState.showAddBillSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissAddBillSheet,
            sheetState = sheetState
        ) {
            AddBillSheetContent(
                uiState = uiState,
                onBillerNameChange = viewModel::onBillerNameChange,
                onReferenceChange = viewModel::onReferenceChange,
                onTotalDueChange = viewModel::onTotalDueChange,
                onMinimumDueChange = viewModel::onMinimumDueChange,
                onDueDateChange = viewModel::onDueDateChange,
                onStatusChange = viewModel::onStatusChange,
                onSave = viewModel::saveNewBill,
                onDismiss = viewModel::dismissAddBillSheet
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun BillCard(
    bwp: BillWithPayments,
    now: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bill = bwp.bill
    val dueDateMillis = bill.dueDateMillis
    val isOverdue = dueDateMillis != null && dueDateMillis < now && bill.status != BillStatus.SETTLED
    val statusColor = when (bill.status) {
        BillStatus.SETTLED -> Color(0xFF4CAF50)
        BillStatus.PARTIAL -> Color(0xFFFF9800)
        BillStatus.PENDING -> if (isOverdue) MaterialTheme.colorScheme.error else Color(0xFF7C5CBF)
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (bill.status == BillStatus.SETTLED) Icons.Default.CheckCircle else Icons.Default.Receipt,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = bill.billerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = statusColor) {
                        Text(
                            text = bill.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Total Due row
            if (bill.totalDue != null) {
                BillDetailRow("Total Due", "%.2f %s".format(bill.totalDue, bill.currencyCode))
            }
            if (bill.minimumDue != null) {
                BillDetailRow("Min Due", "%.2f %s".format(bill.minimumDue, bill.currencyCode))
            }

            // Due date
            if (dueDateMillis != null) {
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date(dueDateMillis))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverdue) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Due $dateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        BillDetailRow("Due", dateStr)
                    }
                }
            }

            // Paid amount
            if (bwp.totalPaid > 0) {
                BillDetailRow(
                    "Paid",
                    "%.2f %s  (${bwp.payments.size} payment${if (bwp.payments.size != 1) "s" else ""})".format(
                        bwp.totalPaid, bill.currencyCode
                    )
                )
            }
        }
    }
}

@Composable
private fun BillDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillSheetContent(
    uiState: BillsUiState,
    onBillerNameChange: (String) -> Unit,
    onReferenceChange: (String) -> Unit,
    onTotalDueChange: (String) -> Unit,
    onMinimumDueChange: (String) -> Unit,
    onDueDateChange: (Long?) -> Unit,
    onStatusChange: (BillStatus) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = uiState.newBillerName,
            onValueChange = onBillerNameChange,
            label = { Text("Biller / Bank name") },
            placeholder = { Text("e.g. ENBD PAYMENTS") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.newReference,
            onValueChange = onReferenceChange,
            label = { Text("Reference (account / contract number)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Total Due — currency is always home currency (read-only suffix)
        OutlinedTextField(
            value = uiState.newTotalDue,
            onValueChange = onTotalDueChange,
            label = { Text("Total Due") },
            placeholder = { Text("0.00") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text(uiState.newCurrencyCode) },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.newMinimumDue,
            onValueChange = onMinimumDueChange,
            label = { Text("Minimum Due (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        AddBillDueDateField(
            dueDateMillis = uiState.newDueDateMillis,
            onDateSelected = onDueDateChange
        )

        AddBillStatusDropdown(
            selected = uiState.newStatus,
            onSelected = onStatusChange
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onSave,
                enabled = uiState.newBillerName.isNotBlank() && !uiState.isSavingBill
            ) { Text("Save") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillDueDateField(dueDateMillis: Long?, onDateSelected: (Long?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }
    val displayText = dueDateMillis?.let { dateFormat.format(Date(it)) } ?: "Not set"

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        label = { Text("Due Date") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Pick date")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateSelected(pickerState.selectedDateMillis)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillStatusDropdown(selected: BillStatus, onSelected: (BillStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BillStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.name) },
                    onClick = {
                        onSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}
