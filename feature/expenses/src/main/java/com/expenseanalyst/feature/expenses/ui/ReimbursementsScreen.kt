package com.expenseanalyst.feature.expenses.ui

import com.expenseanalyst.core.theme.expenseColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.domain.model.Expense

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReimbursementsScreen(
    onBack: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: ReimbursementsViewModel = hiltViewModel(),
    /** Inside the Dues tab: the host draws the title and tabs, so no top bar or back arrow. */
    embedded: Boolean = false
) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val reimbursed by viewModel.reimbursed.collectAsStateWithLifecycle()
    val homeCurrencyCode by viewModel.homeCurrencyCode.collectAsStateWithLifecycle()
    val paybacks by viewModel.paybacks.collectAsStateWithLifecycle()
    val picking by viewModel.picking.collectAsStateWithLifecycle()
    val candidates by viewModel.paymentCandidates.collectAsStateWithLifecycle()

    picking?.let { expense ->
        PaymentPickerSheet(
            expense = expense,
            candidates = candidates,
            onPick = viewModel::linkTo,
            onPaidOutside = viewModel::markPaidOutsideApp,
            onDismiss = viewModel::cancelPicking
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!embedded) TopAppBar(
                title = {
                    Column {
                        Text("Reimbursements", style = MaterialTheme.typography.titleLarge)
                        if (pending.isNotEmpty()) {
                            Text(
                                "${pending.size} pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (pending.isEmpty() && reimbursed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.RequestQuote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Nothing marked reimbursable",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Mark an expense as reimbursable when adding or editing it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (pending.isNotEmpty()) {
                    item { SectionHeader("Pending") }
                    items(pending, key = { "pending-${it.id}" }) { expense ->
                        ReimbursementCard(
                            expense = expense,
                            homeCurrencyCode = homeCurrencyCode,
                            isReimbursed = false,
                            onClick = { onExpenseClick(expense.id) },
                            onToggle = { viewModel.startMarkReimbursed(expense) }
                        )
                    }
                }
                if (reimbursed.isNotEmpty()) {
                    item { SectionHeader("Reimbursed") }
                    items(reimbursed, key = { "reimbursed-${it.id}" }) { expense ->
                        ReimbursementCard(
                            expense = expense,
                            homeCurrencyCode = homeCurrencyCode,
                            isReimbursed = true,
                            paidBy = expense.reimbursedById?.let { paybacks[it] },
                            onClick = { onExpenseClick(expense.id) },
                            onToggle = { viewModel.undoReimbursed(expense) }
                        )
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun ReimbursementCard(
    expense: Expense,
    homeCurrencyCode: String,
    isReimbursed: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    paidBy: Expense? = null
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.RequestQuote,
                contentDescription = null,
                tint = if (isReimbursed) MaterialTheme.colorScheme.primary else MaterialTheme.expenseColors.review,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchantName?.takeIf { it.isNotBlank() } ?: "Unknown merchant",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val reimbursedDate = expense.reimbursedDate
                val dateLabel = if (isReimbursed && paidBy != null) {
                    "Paid back by ${paidBy.merchantName ?: "payment"} · ${DateTimeUtil.formatDateHeader(paidBy.date)}"
                } else if (isReimbursed && reimbursedDate != null) {
                    "Reimbursed ${DateTimeUtil.formatDateHeader(reimbursedDate)}"
                } else {
                    DateTimeUtil.formatDateHeader(expense.date)
                }
                Text(
                    text = "${expense.category.name} · $dateLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${CurrencyFormatter.format(expense.amount, expense.currencyCode)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.expenseColors.spend
                )
                expense.homeAmount?.takeIf { expense.currencyCode != homeCurrencyCode }?.let { ha ->
                    Text(
                        text = CurrencyFormatter.format(ha, homeCurrencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isReimbursed) Icons.Default.Replay else Icons.Default.CheckCircle,
                    contentDescription = if (isReimbursed) "Undo — move back to pending" else "Mark reimbursed",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * "Which payment repaid this?" — recent incoming payments, closest amount first, plus "paid back
 * outside the app" for cash or anything the app never saw.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentPickerSheet(
    expense: Expense,
    candidates: List<Expense>,
    onPick: (Long) -> Unit,
    onPaidOutside: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Which payment repaid this?", style = MaterialTheme.typography.titleMedium)
            Text(
                "${expense.merchantName ?: "Expense"} · ${CurrencyFormatter.format(expense.amount, expense.currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            if (candidates.isEmpty()) {
                Text(
                    "No incoming payments since this expense.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(candidates, key = { it.id }) { payment ->
                    val exact = kotlin.math.abs(payment.amount - expense.amount) < 0.01 &&
                        payment.currencyCode == expense.currencyCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(payment.id) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                payment.merchantName ?: "Payment",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                DateTimeUtil.formatDateHeader(payment.date) +
                                    (if (payment.reimbursementCover != null) " · already repays other items" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "+${CurrencyFormatter.format(payment.amount, payment.currencyCode)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.expenseColors.received
                            )
                            if (exact) {
                                Text(
                                    "Same amount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onPaidOutside, modifier = Modifier.fillMaxWidth()) {
                Text("Paid back outside the app")
            }
        }
    }
}
