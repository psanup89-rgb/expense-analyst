package com.expenseanalyst.feature.expenses.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.core.util.categoryIconVector
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.MerchantRule
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onConvertToEmi: (Long) -> Unit,
    viewModel: ExpenseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    LaunchedEffect(uiState.ruleSaved) {
        if (uiState.ruleSaved) {
            snackbarHostState.showSnackbar("Rule saved — future imports will use this category")
            viewModel.clearRuleSaved()
        }
    }

    if (uiState.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text("Delete expense?") },
            text = { Text("This expense will be removed. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteExpense,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF5555)
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirm) { Text("Cancel") }
            }
        )
    }

    if (uiState.showRuleDialog) {
        val expense = uiState.expense
        val dialogPattern = expense?.let {
            it.merchantName?.takeIf { m -> m.isNotBlank() } ?: it.description.takeIf { d -> d.isNotBlank() }
        }
        if (expense != null && dialogPattern != null) {
            RuleDialog(
                merchantName = dialogPattern,
                categories = uiState.categories,
                existingRule = uiState.existingRule,
                onSave = { category -> viewModel.saveRule(dialogPattern, category) },
                onDismiss = viewModel::dismissRuleDialog
            )
        }
    }

    if (uiState.showLinkBillSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissLinkBillSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LinkBillSheetContent(
                openBills = uiState.openBills,
                onSelect = viewModel::linkToBill,
                onDismiss = viewModel::dismissLinkBillSheet
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Expense Detail", style = MaterialTheme.typography.titleLarge) },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.expense?.let { expense ->
                        IconButton(onClick = { onEdit(expense.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = viewModel::showDeleteConfirm) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                tint = Color(0xFFFF5555))
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
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant) }

            uiState.expense == null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Expense not found", color = MaterialTheme.colorScheme.onSurfaceVariant) }

            else -> ExpenseDetailContent(
                expense = uiState.expense!!,
                existingRule = uiState.existingRule,
                linkedBillName = uiState.linkedBillName,
                hasOpenBills = uiState.openBills.isNotEmpty(),
                onConvertToEmi = { onConvertToEmi(uiState.expense!!.id) },
                onSetRule = viewModel::showRuleDialog,
                onDeleteRule = viewModel::deleteRule,
                onLinkBill = viewModel::showLinkBillSheet,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ExpenseDetailContent(
    expense: Expense,
    existingRule: MerchantRule?,
    linkedBillName: String?,
    hasOpenBills: Boolean,
    onConvertToEmi: () -> Unit,
    onSetRule: () -> Unit,
    onDeleteRule: () -> Unit,
    onLinkBill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = expense.transactionType == TransactionType.INCOME
    val isPayment = expense.transactionType == TransactionType.PAYMENT
    val amountColor = when {
        isIncome -> MaterialTheme.colorScheme.primary
        isPayment -> Color(0xFF7C5CBF)
        else -> Color(0xFFFF5555)
    }
    val amountPrefix = if (isIncome) "+" else "-"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Amount hero card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIconVector(expense.category.iconName),
                        contentDescription = expense.category.name,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "$amountPrefix${CurrencyFormatter.format(expense.amount, expense.currencyCode)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                expense.homeAmount?.let { home ->
                    if (expense.currencyCode != "SAR") { // show if not same as home
                        Text(
                            text = CurrencyFormatter.format(home, "SAR"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = expense.merchantName ?: expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = expense.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Date", DateTimeUtil.formatDateHeader(expense.date))
                DetailDivider()
                DetailRow("Time", DateTimeUtil.formatTime(expense.date))
                DetailDivider()
                DetailRow("Payment", expense.paymentMethod.label)
                // account display wired via accountId in future
                DetailDivider()
                DetailRow("Type", expense.transactionType.name.lowercase().replaceFirstChar { it.uppercase() })
                if (expense.description.isNotBlank()) {
                    DetailDivider()
                    DetailRow("Description", expense.description)
                }
                expense.merchantName?.takeIf { it.isNotBlank() }?.let { merchant ->
                    DetailDivider()
                    DetailRow("Merchant", merchant)
                }
                expense.note?.takeIf { it.isNotBlank() }?.let { note ->
                    DetailDivider()
                    DetailRow("Note", note)
                }
                expense.exchangeRate?.let { rate ->
                    DetailDivider()
                    DetailRow("Exchange Rate", "1 ${expense.currencyCode} = ${String.format("%.4f", rate)}")
                }
                expense.accountDisplayName?.let { accountName ->
                    DetailDivider()
                    DetailRow("Account", accountName)
                }
                if (expense.emiGroupId != null) {
                    DetailDivider()
                    DetailRow("EMI", "Installment ${expense.emiInstallmentNumber ?: "?"}")
                }
                DetailDivider()
                DetailRow("Source", expense.sourceType.name.replace("_", " "))
                // Bill link row — only visible for PAYMENT type
                if (isPayment) {
                    DetailDivider()
                    if (linkedBillName != null) {
                        DetailRow("Linked Bill", linkedBillName)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Linked Bill",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = onLinkBill,
                                enabled = hasOpenBills,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (hasOpenBills) "Link to Bill" else "No open bills",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (hasOpenBills) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                // Auto-category rule row (for all auto-imported expenses; use merchant or description as pattern)
                val rulePattern = (expense.merchantName?.takeIf { it.isNotBlank() } ?: expense.description.takeIf { it.isNotBlank() })
                if (rulePattern != null &&
                    expense.sourceType in setOf(SourceType.SMS_AUTO, SourceType.NOTIFICATION_AUTO)
                ) {
                    DetailDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-category rule",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (existingRule != null) {
                                Text(
                                    text = "\"${existingRule.merchantPattern}\" → ${existingRule.categoryName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        if (existingRule != null) {
                            TextButton(onClick = onSetRule, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text("Edit", style = MaterialTheme.typography.labelMedium)
                            }
                            TextButton(
                                onClick = onDeleteRule,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                            ) {
                                Text("Remove", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            TextButton(onClick = onSetRule, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Text("Set Rule", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                val rawSmsBody = expense.rawSmsBody
                if (expense.sourceType == SourceType.SMS_AUTO && rawSmsBody != null) {
                    DetailDivider()
                    var smsExpanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { smsExpanded = !smsExpanded }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Original SMS",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (smsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (smsExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (smsExpanded) {
                        Text(
                            text = rawSmsBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(12.dp)
                        )
                    }
                }
            }
        }

        // Convert to EMI (only for standalone expense transactions)
        if (expense.emiGroupId == null && expense.transactionType == TransactionType.EXPENSE) {
            OutlinedButton(
                onClick = onConvertToEmi,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Convert to EMI / Installments", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
private fun LinkBillSheetContent(
    openBills: List<Bill>,
    onSelect: (Bill) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Link to Bill",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        if (openBills.isEmpty()) {
            Text(
                text = "No open bills found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            openBills.forEach { bill ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(bill) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bill.billerName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val dueText = bill.totalDue?.let { "%.2f %s".format(it, bill.currencyCode) } ?: "Amount unknown"
                        Text(
                            text = "${bill.status.name.lowercase().replaceFirstChar { it.uppercase() }} · $dueText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text("Cancel")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleDialog(
    merchantName: String,
    categories: List<Category>,
    existingRule: MerchantRule?,
    onSave: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == existingRule?.categoryId })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set category rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Always categorize expenses from \"$merchantName\" as:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory?.id == category.id,
                            onClick = { selectedCategory = category },
                            label = { Text(category.name, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedCategory?.let { onSave(it) } },
                enabled = selectedCategory != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
