package com.expenseanalyst.feature.budget.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PlannedExpense
import com.expenseanalyst.domain.model.SalaryEntry
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun PlannedExpenseCard(
    item: PlannedExpense, categories: List<Category>,
    onEdit: () -> Unit, onDelete: () -> Unit
) {
    val cat = categories.find { it.id == item.categoryId }
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.description, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(cat?.name ?: "Uncategorized", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(CurrencyFormatter.format(item.amount, item.currencyCode),
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Delete", Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun SummaryCard(state: BudgetUiState) {
    val diff = state.totalPlanned - state.totalActual
    val diffColor = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Planned", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(CurrencyFormatter.format(state.totalPlanned, state.homeCurrency),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Actual", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(CurrencyFormatter.format(state.totalActual, state.homeCurrency),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (diff >= 0) "Savings" else "Over", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(CurrencyFormatter.format(kotlin.math.abs(diff), state.homeCurrency),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = diffColor)
            }
        }
    }
}

@Composable
internal fun ComparisonRow(comp: CategoryComparison, currency: String) {
    val barColor = if (comp.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(comp.categoryName, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f))
            Text("${CurrencyFormatter.format(comp.actualAmount, currency)} / ${CurrencyFormatter.format(comp.plannedAmount, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { comp.progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
internal fun UnplannedExpenseRow(expense: Expense, currency: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.merchantName ?: "Unknown", style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(expense.category.name, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(CurrencyFormatter.format(expense.homeAmount ?: expense.amount, currency),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
