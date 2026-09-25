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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.core.util.categoryIconVector
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.MerchantRule
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.util.SpendClassifier
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onConvertToEmi: (Long) -> Unit,
    onViewBill: (Long) -> Unit = {},
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
                selectedTags = uiState.ruleSelectedTags,
                availableTags = uiState.availableTags,
                tagSearchQuery = uiState.ruleTagSearchQuery,
                onTagSearchQueryChange = viewModel::onRuleTagSearchQueryChange,
                onTagSelect = viewModel::onRuleTagSelect,
                onTagRemove = viewModel::onRuleTagRemove,
                onCreateTag = viewModel::onRuleCreateTag,
                onSave = { category, tags -> viewModel.saveRule(dialogPattern, category, tags) },
                onDismiss = viewModel::dismissRuleDialog
            )
        }
    }

    if (uiState.showLoanSheet) {
        val expense = uiState.expense
        if (expense != null) {
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissLoanSheet,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                LoanLinkSheet(
                    expense = expense,
                    pendingLoans = uiState.pendingLoans,
                    onStartLoan = viewModel::startLoanFromThisRow,
                    onLink = viewModel::linkToLoan
                )
            }
        }
    }

    if (uiState.showTransferSheet) {
        val expense = uiState.expense
        if (expense != null) {
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissTransferSheet,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                TransferClassificationSheet(
                    recipientName = expense.merchantName?.takeIf { it.isNotBlank() },
                    current = expense.transferClassification,
                    hasExistingRule = uiState.existingTransferRule != null,
                    onSelect = viewModel::classifyTransfer,
                    onForgetRule = viewModel::forgetTransferRule
                )
            }
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
                linkedBillId = uiState.linkedBillId,
                hasOpenBills = uiState.openBills.isNotEmpty(),
                homeCurrency = uiState.homeCurrency,
                onConvertToEmi = { onConvertToEmi(uiState.expense!!.id) },
                onSetRule = viewModel::showRuleDialog,
                onDeleteRule = viewModel::deleteRule,
                transferRuleName = uiState.existingTransferRule?.recipientDisplayName,
                onClassifyTransfer = viewModel::showTransferSheet,
                linkedLoan = uiState.linkedLoan,
                onLinkLoan = viewModel::showLoanSheet,
                onUnlinkLoan = viewModel::unlinkFromLoan,
                onLinkBill = viewModel::showLinkBillSheet,
                onViewBill = onViewBill,
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
    linkedBillId: Long?,
    hasOpenBills: Boolean,
    homeCurrency: String,
    transferRuleName: String?,
    onClassifyTransfer: () -> Unit,
    linkedLoan: LentItem?,
    onLinkLoan: () -> Unit,
    onUnlinkLoan: () -> Unit,
    onConvertToEmi: () -> Unit,
    onSetRule: () -> Unit,
    onDeleteRule: () -> Unit,
    onLinkBill: () -> Unit,
    onViewBill: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val style = transactionAmountStyle(expense)
    val amountColor = style.color
    val amountPrefix = style.prefix

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
                    if (expense.currencyCode != homeCurrency) {
                        Text(
                            text = CurrencyFormatter.format(home, homeCurrency),
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
                if (expense.tags.isNotEmpty()) {
                    DetailDivider()
                    TagsDetailRow(expense.tags)
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
                if (expense.isReimbursable) {
                    DetailDivider()
                    val reimbursedDate = expense.reimbursedDate
                    DetailRow(
                        "Reimbursement",
                        if (reimbursedDate != null) {
                            "Reimbursed ${DateTimeUtil.formatDateHeader(reimbursedDate)}"
                        } else {
                            "Pending"
                        }
                    )
                }
                DetailDivider()
                DetailRow("Source", expense.sourceType.name.replace("_", " "))
                // Bill link row — only visible for PAYMENT type
                if (expense.transactionType == TransactionType.PAYMENT) {
                    DetailDivider()
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
                        if (linkedBillName != null && linkedBillId != null) {
                            TextButton(
                                onClick = { onViewBill(linkedBillId) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = linkedBillName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
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
                // Loan link. A row attached to a loan counts toward neither Spent nor Received,
                // whatever its type. PAYMENT rows (card/bill settlement) are never loan legs.
                if (expense.transactionType != TransactionType.PAYMENT) {
                    DetailDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = if (expense.loanId == null) onLinkLoan else ({}))
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Loan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (expense.loanId != null) {
                                val side = if (SpendClassifier.isLoanRepayment(expense)) "Repayment" else "Lent out"
                                Text(
                                    text = "$side · ${linkedLoan?.personName ?: "loan #${expense.loanId}"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Not counted in Spent or Received",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "Not linked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        if (expense.loanId != null) {
                            TextButton(
                                onClick = onUnlinkLoan,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                            ) { Text("Unlink", style = MaterialTheme.typography.labelMedium) }
                        } else {
                            TextButton(
                                onClick = onLinkLoan,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) { Text("Link to loan", style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }

                // Transfer classification — the only place a single row's classification can
                // be set or overridden. Above the auto-category rule row because for a transfer
                // this is the more consequential setting: it decides whether the row counts
                // toward Spent at all.
                if (expense.transactionType == TransactionType.TRANSFER) {
                    DetailDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onClassifyTransfer)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Transfer type",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = expense.transferClassification?.label ?: "Not classified",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (expense.transferClassification == null) {
                                    Color(0xFFF57C00)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (expense.transferClassification == null) {
                                Text(
                                    text = "Counts toward neither Spent nor Received until set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (transferRuleName != null) {
                                Text(
                                    text = "Remembered for $transferRuleName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = onClassifyTransfer,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (expense.transferClassification == null) "Set" else "Change",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

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
                                if (existingRule.tags.isNotEmpty()) {
                                    Text(
                                        text = "+ tags: " + existingRule.tags.joinToString(", ") { it.name },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                // Not gated to SMS_AUTO — NOTIFICATION_AUTO-captured expenses (the majority of
                // recent auto-saves, per PendingNotificationManager) also carry the source text
                // and deserve to show it here too, same as the Edit screen already does.
                if (expense.sourceType != SourceType.MANUAL && rawSmsBody != null) {
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
                    if (!expense.sourceSender.isNullOrBlank()) {
                        Text(
                            text = "From: ${expense.sourceSender}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = if (smsExpanded) 8.dp else 4.dp)
                        )
                    }
                    if (smsExpanded) {
                        Text(
                            text = rawSmsBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(12.dp)
                        )
                        if (expense.sourceType == SourceType.SMS_AUTO) {
                            TextButton(
                                onClick = {
                                    val address = findSmsAddress(context, rawSmsBody)
                                    val intent = if (address != null) {
                                        Intent(Intent.ACTION_VIEW, Uri.parse("sms:$address"))
                                    } else {
                                        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
                                    }
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text("Open in Messages ↗", style = MaterialTheme.typography.labelSmall)
                            }
                        }
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
    selectedTags: List<Tag>,
    availableTags: List<Tag>,
    tagSearchQuery: String,
    onTagSearchQueryChange: (String) -> Unit,
    onTagSelect: (Tag) -> Unit,
    onTagRemove: (Tag) -> Unit,
    onCreateTag: (String) -> Unit,
    onSave: (Category, List<Tag>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == existingRule?.categoryId })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set category rule") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                Text(
                    text = "Also apply these tags:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TagSelector(
                    selectedTags = selectedTags,
                    availableTags = availableTags,
                    searchQuery = tagSearchQuery,
                    onSearchQueryChange = onTagSearchQueryChange,
                    onTagSelect = onTagSelect,
                    onTagRemove = onTagRemove,
                    onCreateTag = onCreateTag,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedCategory?.let { onSave(it, selectedTags) } },
                enabled = selectedCategory != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsDetailRow(tags: List<com.expenseanalyst.domain.model.Tag>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { tag ->
                FilterChip(selected = false, onClick = {}, label = { Text(tag.name) })
            }
        }
    }
}

/**
 * Lets the user say what a transfer actually was. Three options rather than two because the
 * parsers emit TransactionDirection.TRANSFER for incoming internal transfers as well, and that
 * direction is discarded before the row is stored — without "Received from someone else" the
 * user would be forced to label money arriving as money spent.
 *
 * "Always do this" defaults to on: transfers recur to the same handful of people, so
 * remembering is the expected behaviour rather than an opt-in. Forgetting is offered here too,
 * because there is no rules-management screen and a rule would otherwise be write-only.
 */
@Composable
private fun TransferClassificationSheet(
    recipientName: String?,
    current: TransferClassification?,
    hasExistingRule: Boolean,
    onSelect: (TransferClassification, Boolean) -> Unit,
    onForgetRule: () -> Unit
) {
    var remember by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(bottom = 28.dp)) {
        Text(
            text = "Transfer type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp)
        )
        Text(
            text = "An unclassified transfer counts toward neither Spent nor Received.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
        )

        TransferOptionRow(
            option = TransferClassification.EXTERNAL,
            supporting = "Counts toward Spent",
            selected = current == TransferClassification.EXTERNAL,
            onClick = { onSelect(TransferClassification.EXTERNAL, remember) }
        )
        TransferOptionRow(
            option = TransferClassification.OWN_ACCOUNT,
            supporting = "Not spending — shown separately in Analytics",
            selected = current == TransferClassification.OWN_ACCOUNT,
            onClick = { onSelect(TransferClassification.OWN_ACCOUNT, remember) }
        )
        TransferOptionRow(
            option = TransferClassification.EXTERNAL_IN,
            supporting = "Counts toward Received",
            selected = current == TransferClassification.EXTERNAL_IN,
            onClick = { onSelect(TransferClassification.EXTERNAL_IN, remember) }
        )

        if (recipientName != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (hasExistingRule) {
                TextButton(
                    onClick = onForgetRule,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5555))
                ) {
                    Text("Forget rule for $recipientName")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { remember = !remember }
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = remember, onCheckedChange = { remember = it })
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Always do this for $recipientName",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferOptionRow(
    option: TransferClassification,
    supporting: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(option.label) },
        supportingContent = { Text(supporting) },
        trailingContent = {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Attaches a row to a loan. Which directions are offered depends on what the row can be:
 * an INCOME row can only be a repayment, an EXPENSE only money lent out, and a TRANSFER that
 * hasn't been given a direction yet — the parsers discard it — can be either.
 */
@Composable
private fun LoanLinkSheet(
    expense: Expense,
    pendingLoans: List<LentItem>,
    onStartLoan: () -> Unit,
    onLink: (LentItem, Boolean) -> Unit
) {
    val canBeLentOut = when (expense.transactionType) {
        TransactionType.INCOME -> false
        TransactionType.TRANSFER -> expense.transferClassification != TransferClassification.EXTERNAL_IN
        else -> true
    }
    val canBeRepayment = when (expense.transactionType) {
        TransactionType.INCOME -> true
        TransactionType.TRANSFER -> expense.transferClassification != TransferClassification.EXTERNAL
        else -> false
    }
    val person = expense.merchantName?.takeIf { it.isNotBlank() }

    Column(modifier = Modifier.padding(bottom = 28.dp)) {
        Text(
            text = "Link to loan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp)
        )
        Text(
            text = "A linked transaction counts toward neither Spent nor Received. A loan can be lent " +
                "and repaid in several transactions, and in different currencies.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
        )

        if (canBeLentOut) {
            if (person != null) {
                ListItem(
                    headlineContent = { Text("Start a new loan to $person") },
                    supportingContent = { Text("This transaction is the money lent out") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = onStartLoan)
                )
            }
            pendingLoans.forEach { loan ->
                ListItem(
                    headlineContent = { Text("Lent out — add to ${loan.personName}'s loan") },
                    supportingContent = { Text("${loan.currencyCode} ${"%.2f".format(loan.amount)} · another instalment lent") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onLink(loan, false) }
                )
            }
        }
        if (canBeRepayment) {
            pendingLoans.forEach { loan ->
                ListItem(
                    headlineContent = { Text("Repayment of ${loan.personName}'s loan") },
                    supportingContent = { Text("${loan.currencyCode} ${"%.2f".format(loan.amount)} lent") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onLink(loan, true) }
                )
            }
            if (pendingLoans.isEmpty()) {
                Text(
                    text = "No pending loans. Add one from Settings → Loans & Lending (with the amount " +
                        "originally lent), then come back to link this repayment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}

