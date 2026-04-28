package com.expenseanalyst.feature.budget.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PlannedExpense
import com.expenseanalyst.domain.model.SalaryEntry
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        if (activity != null && BiometricHelper.canAuthenticate(context)) {
            BiometricHelper.authenticate(activity,
                onSuccess = { viewModel.onAuthenticated() },
                onError = { }
            )
        } else {
            viewModel.onAuthNotRequired()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        if (!uiState.isAuthenticated) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("Authentication required", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = {
                        if (activity != null) BiometricHelper.authenticate(activity,
                            onSuccess = { viewModel.onAuthenticated() }, onError = { })
                    }) { Text("Retry") }
                }
            }
        } else if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            BudgetContent(uiState, viewModel, Modifier.padding(padding))
        }
    }
}

@Composable
private fun BudgetContent(state: BudgetUiState, vm: BudgetViewModel, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {

        // Month navigation
        item { MonthNavigator(state.month, state.year, onPrev = { vm.navigateMonth(-1) }, onNext = { vm.navigateMonth(1) }) }

        // Salary card
        item { SalaryCard(state, vm) }

        // Carry forward prompt
        if (state.showCarryForwardPrompt) {
            item { CarryForwardBanner(onCarry = { vm.carryForward() }, onDismiss = { vm.dismissCarryForward() }) }
        }

        // Planned expenses header + list
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Planned Expenses", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.showAddPlannedSheet() }) {
                    Icon(Icons.Default.Add, "Add planned expense")
                }
            }
        }
        if (state.plannedExpenses.isEmpty()) {
            item {
                Text("No planned expenses for this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(state.plannedExpenses, key = { it.id }) { item ->
                PlannedExpenseCard(item, state.categories,
                    onEdit = { vm.showAddPlannedSheet(item) },
                    onDelete = { vm.deletePlannedExpense(item.id) })
            }
        }

        // Planned vs Actual
        if (state.categoryComparisons.isNotEmpty()) {
            item { Text("Planned vs Actual", style = MaterialTheme.typography.titleMedium) }
            item { SummaryCard(state) }
            items(state.categoryComparisons) { comp -> ComparisonRow(comp, state.homeCurrency) }
        }

        // Unplanned expenses
        if (state.unplannedExpenses.isNotEmpty()) {
            item {
                Text("Unplanned Expenses", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error)
            }
            items(state.unplannedExpenses, key = { it.id }) { expense ->
                UnplannedExpenseRow(expense, state.homeCurrency)
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }

    // Dialogs and sheets
    if (state.showSalaryDialog) SalaryDialog(state, vm)
    if (state.showIncomeSheet) IncomeBottomSheet(state, vm)
    if (state.showAddPlannedSheet) AddPlannedExpenseSheet(state, vm)
    if (state.showSalaryHistory) SalaryHistorySheet(state, vm)
}
