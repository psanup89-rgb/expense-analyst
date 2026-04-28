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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expenseanalyst.core.util.CurrencyFormatter
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun SalaryDialog(state: BudgetUiState, vm: BudgetViewModel) {
    AlertDialog(
        onDismissRequest = { vm.dismissSalaryDialog() },
        title = { Text("Set Salary") },
        text = {
            OutlinedTextField(
                value = state.salaryInput,
                onValueChange = { vm.onSalaryInputChange(it) },
                label = { Text("Amount (${state.homeCurrency})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { vm.saveSalary() },
                enabled = state.salaryInput.toDoubleOrNull() != null && state.salaryInput.toDoubleOrNull()!! > 0
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = { vm.dismissSalaryDialog() }) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IncomeBottomSheet(state: BudgetUiState, vm: BudgetViewModel) {
    ModalBottomSheet(onDismissRequest = { vm.dismissIncomeSheet() }) {
        Column(Modifier.padding(16.dp)) {
            Text("Select Income Transaction", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Tap to use as this month's salary", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            if (state.incomeTransactions.isEmpty()) {
                Text("No income transactions found this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.incomeTransactions.forEach { expense ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { vm.selectIncomeAsSalary(expense) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(expense.merchantName ?: "Income",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium)
                                Text(expense.category.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(CurrencyFormatter.format(expense.homeAmount ?: expense.amount, state.homeCurrency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddPlannedExpenseSheet(state: BudgetUiState, vm: BudgetViewModel) {
    ModalBottomSheet(onDismissRequest = { vm.dismissPlannedSheet() }) {
        Column(Modifier.padding(16.dp)) {
            Text(if (state.editingPlannedExpense != null) "Edit Planned Expense" else "Add Planned Expense",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.plannedDescription,
                onValueChange = { vm.onPlannedDescriptionChange(it) },
                label = { Text("Description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.plannedAmount,
                onValueChange = { vm.onPlannedAmountChange(it) },
                label = { Text("Amount (${state.homeCurrency})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("Category", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories) { cat ->
                    FilterChip(
                        selected = state.plannedCategoryId == cat.id,
                        onClick = { vm.onPlannedCategoryChange(cat.id) },
                        label = { Text(cat.name) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            val canSave = state.plannedDescription.isNotBlank() &&
                state.plannedAmount.toDoubleOrNull() != null &&
                state.plannedAmount.toDoubleOrNull()!! > 0 &&
                state.plannedCategoryId != null
            FilledTonalButton(onClick = { vm.savePlannedExpense() }, enabled = canSave,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text("Save")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalaryHistorySheet(state: BudgetUiState, vm: BudgetViewModel) {
    ModalBottomSheet(onDismissRequest = { vm.dismissSalaryHistory() }) {
        Column(Modifier.padding(16.dp)) {
            Text("Salary History", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (state.salaryHistory.isEmpty()) {
                Text("No salary history yet", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.salaryHistory.forEach { entry ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("${Month.of(entry.month).getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${entry.year}",
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(CurrencyFormatter.format(entry.amount, entry.currencyCode),
                            style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
