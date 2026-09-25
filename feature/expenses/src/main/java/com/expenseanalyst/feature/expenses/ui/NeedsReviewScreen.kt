package com.expenseanalyst.feature.expenses.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.util.ReviewReason
import com.expenseanalyst.domain.util.SpendClassifier
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsReviewScreen(
    onExpenseClick: (Long) -> Unit,
    viewModel: NeedsReviewViewModel = hiltViewModel()
) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val homeCurrencyCode by viewModel.homeCurrencyCode.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Needs Review", style = MaterialTheme.typography.titleLarge)
                        if (expenses.isNotEmpty()) {
                            Text(
                                "${expenses.size} expense${if (expenses.size == 1) "" else "s"} need attention",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    if (expenses.isNotEmpty()) {
                        TextButton(onClick = viewModel::markAllReviewed) {
                            Text("Mark all done", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "All caught up",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "No expenses need review",
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
                items(expenses, key = { it.id }) { expense ->
                    NeedsReviewCard(
                        expense = expense,
                        homeCurrencyCode = homeCurrencyCode,
                        onClick = { onExpenseClick(expense.id) },
                        onMarkDone = { viewModel.markReviewed(expense.id) },
                        onClassifyTransfer = { viewModel.classifyTransfer(expense.id, it) }
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun NeedsReviewCard(
    expense: Expense,
    homeCurrencyCode: String,
    onClick: () -> Unit,
    onMarkDone: () -> Unit,
    onClassifyTransfer: (TransferClassification) -> Unit
) {
    val style = transactionAmountStyle(expense)
    val amountColor = style.color
    val amountPrefix = style.prefix

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
                Icons.Default.RateReview,
                contentDescription = null,
                tint = Color(0xFFF57C00),
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchantName?.takeIf { it.isNotBlank() } ?: "Unknown merchant",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (expense.merchantName.isNullOrBlank())
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${expense.category.name} · ${DateTimeUtil.formatDateHeader(expense.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val reasonLabels = expense.reviewReasons.map { it.label }
                    .ifEmpty {
                        // A pre-classification transfer has no persisted reason; say why it's here.
                        if (SpendClassifier.isUnclassifiedTransfer(expense)) {
                            listOf(ReviewReason.UNCLASSIFIED_TRANSFER.label)
                        } else {
                            emptyList()
                        }
                    }
                if (reasonLabels.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Missing: ${reasonLabels.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF57C00),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyFormatter.format(expense.amount, expense.currencyCode)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                expense.homeAmount?.takeIf { expense.currencyCode != homeCurrencyCode }?.let { ha ->
                    Text(
                        text = CurrencyFormatter.format(ha, homeCurrencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Hidden for an unclassified transfer: clearing needs_review wouldn't remove it
            // from this list (it qualifies structurally, not by the flag), so the control would
            // silently do nothing. Classifying is the resolution — see the buttons below.
            if (!SpendClassifier.isUnclassifiedTransfer(expense)) {
                IconButton(onClick = onMarkDone, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Mark as reviewed",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Inline classification for unclassified transfers, so the backlog can be cleared from
        // this screen instead of opening every row. "Mark done" above stays as-is: it clears the
        // flag without classifying, and the row correctly stays out of every total.
        // Keyed on the row's actual state, not on the persisted reason list: rows captured
        // before classification existed carry no reason but still need classifying.
        if (SpendClassifier.isUnclassifiedTransfer(expense)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, end = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onClassifyTransfer(TransferClassification.EXTERNAL) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("To someone", style = MaterialTheme.typography.labelMedium)
                }
                FilledTonalButton(
                    onClick = { onClassifyTransfer(TransferClassification.OWN_ACCOUNT) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("My account", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
