package com.expenseanalyst.feature.budget.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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
internal fun MonthNavigator(month: Int, year: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, "Previous month") }
        Text("${Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())} $year",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Next month") }
    }
}

@Composable
internal fun SalaryCard(state: BudgetUiState, vm: BudgetViewModel) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Salary", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { vm.showSalaryHistory() }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.History, "Salary history", Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            if (state.salary != null) {
                Text(CurrencyFormatter.format(state.salary.amount, state.homeCurrency),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Not set", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.showSalaryDialog() },
                    shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.salary != null) "Edit" else "Set")
                }
                if (state.incomeTransactions.isNotEmpty()) {
                    OutlinedButton(onClick = { vm.showIncomeSheet() },
                        shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Auto-detect")
                    }
                }
            }
        }
    }
}

@Composable
internal fun CarryForwardBanner(onCarry: () -> Unit, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCopy, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Copy from last month?", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text("Carry forward planned expenses", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onDismiss) { Text("Skip") }
            FilledTonalButton(onClick = onCarry) { Text("Copy") }
        }
    }
}
