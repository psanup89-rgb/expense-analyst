# Skill: Analytics Drill-Down Pattern (ModalBottomSheet + Sealed Filter)

## Overview

This pattern lets a dashboard screen show drill-down details (bottom sheet with filtered expense rows) when a user taps any summary card, chart segment, or list row — without creating new screens or navigation routes.

---

## Pattern

### 1. Sealed Filter Class (in UiState file)

```kotlin
sealed class DrillDownFilter {
    data object Spent : DrillDownFilter()
    data object Income : DrillDownFilter()
    data class ByCategory(val categoryName: String) : DrillDownFilter()
    data class ByMerchant(val merchantName: String) : DrillDownFilter()
}
```

Add two fields to `UiState`:
```kotlin
val drillDownTitle: String? = null        // null = sheet closed
val drillDownExpenses: List<Expense> = emptyList()
```

### 2. ViewModel — 5th Flow + Reactive Filtering

Add a `MutableStateFlow<DrillDownFilter?>` and fold it into the existing `combine()`.
`kotlinx.coroutines.combine` supports up to 5 typed overloads — no nesting needed:

```kotlin
private val _drillDownFilter = MutableStateFlow<DrillDownFilter?>(null)

val uiState = combine(
    _selectedMonth, expenses, prevMonthExpenses,
    currencyRepository.getHomeCurrency(), _drillDownFilter
) { selectedMonth, expList, prevList, homeCurrency, drillDown ->

    val active = expList.filter { !it.isDeleted && sameMonth(it.date, selectedMonth) }

    val drillDownExpenses = when (drillDown) {
        is DrillDownFilter.Spent   -> active.filter { it.transactionType == TransactionType.EXPENSE }.sortedByDescending { it.date }
        is DrillDownFilter.Income  -> active.filter { it.transactionType == TransactionType.INCOME }.sortedByDescending { it.date }
        is DrillDownFilter.ByCategory -> active.filter { it.transactionType == TransactionType.EXPENSE && it.category.name == drillDown.categoryName }.sortedByDescending { it.date }
        is DrillDownFilter.ByMerchant -> active.filter { it.transactionType == TransactionType.EXPENSE && it.merchantName == drillDown.merchantName }.sortedByDescending { it.date }
        null -> emptyList()
    }

    val drillDownTitle = when (drillDown) {
        is DrillDownFilter.Spent      -> "All Expenses"
        is DrillDownFilter.Income     -> "All Income"
        is DrillDownFilter.ByCategory -> drillDown.categoryName
        is DrillDownFilter.ByMerchant -> drillDown.merchantName
        null                          -> null
    }

    AnalyticsUiState(
        // … existing fields …
        drillDownTitle = drillDownTitle,
        drillDownExpenses = drillDownExpenses
    )
}

fun setDrillDown(filter: DrillDownFilter) { _drillDownFilter.value = filter }
fun dismissDrillDown() { _drillDownFilter.value = null }
```

> **If you already have 5 flows**: nest two `combine()` calls. The outer combines two existing flows into a `Pair`, then the inner uses that `Pair` as one parameter.

### 3. Screen — Clickable Items + ModalBottomSheet

**Add `onClick` to clickable composables:**

```kotlin
@Composable
fun SummaryCard(…, onClick: (() -> Unit)? = null) {
    Card(modifier = Modifier.clickable(enabled = onClick != null, onClick = onClick ?: {})) { … }
}
```

**Call with lambdas:**

```kotlin
SummaryCard(label = "Spent", onClick = { viewModel.setDrillDown(DrillDownFilter.Spent) })
CategoryBar(cat = cat, onClick = { viewModel.setDrillDown(DrillDownFilter.ByCategory(cat.categoryName)) })
MerchantRow(merchant = m, onClick = { viewModel.setDrillDown(DrillDownFilter.ByMerchant(m.name)) })
```

**Show the bottom sheet (CRITICAL — capture local val first):**

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
val drillDownTitle = uiState.drillDownTitle   // ← must capture, not inline (see delegated-property-smart-cast.md)
if (drillDownTitle != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = viewModel::dismissDrillDown,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        DrillDownSheet(
            title = drillDownTitle,
            expenses = uiState.drillDownExpenses,
            currencyCode = uiState.homeCurrencyCode,
            onExpenseClick = { id -> viewModel.dismissDrillDown(); onExpenseClick(id) }
        )
    }
}
```

### 4. DrillDownSheet Composable

```kotlin
@Composable
private fun DrillDownSheet(
    title: String,
    expenses: List<Expense>,
    currencyCode: String,
    onExpenseClick: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("${expenses.size} transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyColumn { items(expenses) { expense -> DrillDownExpenseRow(expense, currencyCode, onExpenseClick) } }
        Spacer(Modifier.height(24.dp))  // gesture bar padding
    }
}
```

### 5. DrillDownExpenseRow

Each row shows: colored category circle + icon | merchant + date·category | amount

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onExpenseClick(expense.id) }
        .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    // Category color circle (36dp)
    // Merchant + "date · category" column
    // Amount right-aligned (NeonYellow for EXPENSE, NeonGreen for INCOME)
}
```

---

## Navigation Wiring

In `AppNavGraph.kt`:
```kotlin
composable(NavRoutes.ANALYTICS) {
    AnalyticsScreen(
        onBack = { navController.popBackStack() },
        onExpenseClick = { id -> navController.navigate(NavRoutes.expenseDetail(id)) }
    )
}
```

---

## Example from Codebase

`feature/analytics/ui/AnalyticsScreen.kt`, `AnalyticsViewModel.kt`, `AnalyticsUiState.kt`
