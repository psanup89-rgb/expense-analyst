package com.expenseanalyst.feature.expenses.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.core.util.categoryIconVector
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.util.ReceivedBreakdown
import com.expenseanalyst.domain.util.SpendBreakdown

@Composable
fun ExpenseListScreen(
    onAddExpense: () -> Unit,
    onImportFromSms: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    onViewAnalytics: () -> Unit = {},
    onReviewUnclassified: () -> Unit = {},
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.pendingDeleteId) {
        if (uiState.pendingDeleteId != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Expense deleted",
                actionLabel = "UNDO"
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
        }
    }

    ExpenseListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddExpense = onAddExpense,
        onImportFromSms = onImportFromSms,
        onExpenseClick = onExpenseClick,
        onViewAnalytics = onViewAnalytics,
        onCategoryFilter = viewModel::selectCategory,
        onPaymentMethodFilter = viewModel::selectPaymentMethod,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onDeleteExpense = viewModel::deleteExpense,
        onPrevMonth = viewModel::prevMonth,
        onNextMonth = viewModel::nextMonth,
        onAllMonths = viewModel::selectAllMonths,
        onReviewUnclassified = onReviewUnclassified
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseListContent(
    uiState: ExpenseListUiState,
    snackbarHostState: SnackbarHostState,
    onAddExpense: () -> Unit,
    onImportFromSms: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    onViewAnalytics: () -> Unit = {},
    onCategoryFilter: (Long?) -> Unit,
    onPaymentMethodFilter: (PaymentMethod?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAllMonths: () -> Unit,
    onReviewUnclassified: () -> Unit
) {
    var showAddOptions by remember { mutableStateOf(false) }
    // Ephemeral UI state, same as showAddOptions — the figures themselves already live on the
    // UiState, so the sheet only needs to know which one to render.
    var breakdownSheet by remember { mutableStateOf<BreakdownKind?>(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Title
                item {
                    Text(
                        text = "Expense Analyst",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                }

                // Search bar
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        placeholder = { Text("Search expenses...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                }

                // Monthly summary card with navigation
                item {
                    MonthlySummaryCard(
                        selectedYearMonth = uiState.selectedYearMonth,
                        canGoNext = uiState.canGoNext,
                        totalDebit = uiState.monthTotalDebit,
                        totalCredit = uiState.monthTotalCredit,
                        currencyCode = uiState.homeCurrencyCode,
                        onPrev = onPrevMonth,
                        onNext = onNextMonth,
                        onAllMonths = onAllMonths,
                        onViewAnalytics = onViewAnalytics,
                        onSpentClick = { breakdownSheet = BreakdownKind.SPENT },
                        onReceivedClick = { breakdownSheet = BreakdownKind.RECEIVED },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Category filter — single scrollable row
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedCategoryId == null,
                                onClick = { onCategoryFilter(null) },
                                label = { Text("All", style = MaterialTheme.typography.labelLarge) },
                                leadingIcon = if (uiState.selectedCategoryId == null) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedCategoryId == null,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                        items(uiState.categories) { cat ->
                            FilterChip(
                                selected = uiState.selectedCategoryId == cat.id,
                                onClick = { onCategoryFilter(cat.id) },
                                label = { Text(cat.name, style = MaterialTheme.typography.labelLarge) },
                                leadingIcon = {
                                    Icon(categoryIconVector(cat.iconName), null, Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedCategoryId == cat.id,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                // Payment method filter — single scrollable row
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedPaymentMethod == null,
                                onClick = { onPaymentMethodFilter(null) },
                                label = { Text("All Methods", style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedPaymentMethod == null,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                        items(PaymentMethod.entries) { method ->
                            FilterChip(
                                selected = uiState.selectedPaymentMethod == method,
                                onClick = { onPaymentMethodFilter(if (uiState.selectedPaymentMethod == method) null else method) },
                                label = { Text(method.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.selectedPaymentMethod == method,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                if (uiState.groups.isEmpty()) {
                    item { EmptyExpenseState(modifier = Modifier.padding(top = 80.dp)) }
                } else {
                    uiState.groups.forEach { group ->
                        item(key = group.header) {
                            DateGroupHeader(
                                header = group.header,
                                total = group.daySpendTotal,
                                currencyCode = uiState.homeCurrencyCode,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(group.expenses, key = { it.id }) { expense ->
                            SwipeToDeleteExpenseCard(
                                expense = expense,
                                homeCurrencyCode = uiState.homeCurrencyCode,
                                onClick = { onExpenseClick(expense.id) },
                                onDelete = { onDeleteExpense(expense.id) },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddOptions = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add expense", modifier = Modifier.size(28.dp))
        }

        if (showAddOptions) {
            AddExpenseOptionsSheet(
                onDismiss = { showAddOptions = false },
                onEnterManually = { showAddOptions = false; onAddExpense() },
                onImportFromSms = { showAddOptions = false; onImportFromSms() }
            )
        }

        breakdownSheet?.let { kind ->
            TotalsBreakdownSheet(
                kind = kind,
                monthLabel = uiState.selectedYearMonth?.label ?: "All months",
                currencyCode = uiState.homeCurrencyCode,
                spend = uiState.spendBreakdown,
                received = uiState.receivedBreakdown,
                onDismiss = { breakdownSheet = null },
                onReviewUnclassified = { breakdownSheet = null; onReviewUnclassified() }
            )
        }
    }
    } // end Scaffold
}

@Composable
private fun MonthlySummaryCard(
    selectedYearMonth: YearMonth?,
    canGoNext: Boolean,
    totalDebit: Double,
    totalCredit: Double,
    currencyCode: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onAllMonths: () -> Unit,
    onViewAnalytics: () -> Unit = {},
    onSpentClick: () -> Unit,
    onReceivedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            // Month navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous month",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = selectedYearMonth?.label ?: "All Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedYearMonth != null) {
                        Text(
                            text = "All months",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(onClick = onAllMonths)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    enabled = canGoNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next month",
                        modifier = Modifier.size(18.dp),
                        tint = if (canGoNext) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSpentClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Spent  ⓘ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(totalDebit, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5555)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onReceivedClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Received  ⓘ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (totalCredit > 0) CurrencyFormatter.format(totalCredit, currencyCode) else "—",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (totalCredit > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onViewAnalytics) {
                    Text(
                        "View Analytics →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DateGroupHeader(
    header: String,
    total: Double,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = header.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        if (total > 0) {
            Text(
                text = CurrencyFormatter.format(total, currencyCode),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    homeCurrencyCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = transactionAmountStyle(expense)
    val amountColor = style.color
    val amountPrefix = style.prefix

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIconVector(expense.category.iconName),
                    contentDescription = expense.category.name,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.merchantName ?: expense.description,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${expense.category.name} · ${DateTimeUtil.formatTime(expense.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Transfers carry a qualifier here ("Sent", "Own transfer", "Tap to
                    // classify"), since the amount alone can no longer say which kind it is.
                    style.note?.let { note ->
                        Text(
                            text = " · $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = style.noteColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${CurrencyFormatter.format(expense.amount, expense.currencyCode)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = expense.accountDisplayName ?: expense.paymentMethod.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val homeAmount = expense.homeAmount
                if (expense.currencyCode != homeCurrencyCode && homeAmount != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(homeAmount, homeCurrencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteExpenseCard(
    expense: Expense,
    homeCurrencyCode: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    Color(0xFFFF5555) else MaterialTheme.colorScheme.surfaceContainerLow,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = Color.White, modifier = Modifier.size(24.dp))
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        content = {
            ExpenseCard(
                expense = expense,
                homeCurrencyCode = homeCurrencyCode,
                onClick = onClick
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseOptionsSheet(
    onDismiss: () -> Unit,
    onEnterManually: () -> Unit,
    onImportFromSms: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Add Expense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            ListItem(
                headlineContent = { Text("Enter manually") },
                supportingContent = { Text("Fill in the details yourself") },
                leadingContent = {
                    Icon(Icons.Default.Edit, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.clickable(onClick = onEnterManually)
            )
            ListItem(
                headlineContent = { Text("Import from SMS") },
                supportingContent = { Text("Pick a bank message and auto-fill") },
                leadingContent = {
                    Icon(Icons.Default.Sms, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.clickable(onClick = onImportFromSms)
            )
        }
    }
}

@Composable
private fun EmptyExpenseState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "₿",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "No expenses yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap + to add your first expense",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

internal enum class BreakdownKind { SPENT, RECEIVED }

/**
 * Explains how the Spent or Received figure on the summary card was arrived at.
 *
 * Every number comes straight from the breakdown already computed for the card, so the sheet
 * can never disagree with the figure it is explaining. The "Not counted" section is the point
 * of the whole thing: it makes own-account and still-unclassified transfers visible and
 * quantified instead of silently absent, and gives the unclassified ones a route to the Review
 * screen — which is how the pre-existing backlog gets discovered, since old rows were
 * deliberately not backfilled into the review queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TotalsBreakdownSheet(
    kind: BreakdownKind,
    monthLabel: String,
    currencyCode: String,
    spend: SpendBreakdown,
    received: ReceivedBreakdown,
    onDismiss: () -> Unit,
    onReviewUnclassified: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text(
                text = if (kind == BreakdownKind.SPENT) "How Spent was calculated"
                else "How Received was calculated",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (kind == BreakdownKind.SPENT) {
                BreakdownRow("Expenses", spend.expenseCount, spend.expenseTotal, "+", currencyCode)
                BreakdownRow(
                    "Transfers to others", spend.externalTransferCount,
                    spend.externalTransferTotal, "+", currencyCode
                )
                BreakdownRow("Refunds", spend.refundCount, spend.refundTotal, "\u2212", currencyCode)
                BreakdownTotal("Spent", spend.net, currencyCode)

                if (spend.ownTransferCount > 0 || spend.unclassifiedTransferCount > 0 || spend.loanOutCount > 0) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "NOT COUNTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    if (spend.loanOutCount > 0) {
                        BreakdownRow(
                            "Loans lent out", spend.loanOutCount,
                            spend.loanOutTotal, "", currencyCode, muted = true,
                            caption = "Money lent, not spent"
                        )
                    }
                    if (spend.ownTransferCount > 0) {
                        BreakdownRow(
                            "Own-account transfers", spend.ownTransferCount,
                            spend.ownTransferTotal, "", currencyCode, muted = true,
                            caption = "Moved between your accounts"
                        )
                    }
                    if (spend.unclassifiedTransferCount > 0) {
                        BreakdownRow(
                            "Unclassified transfers", spend.unclassifiedTransferCount,
                            spend.unclassifiedTransferTotal, "", currencyCode, muted = true,
                            caption = "Not yet marked as yours or someone else's"
                        )
                        TextButton(onClick = onReviewUnclassified) { Text("Review") }
                    }
                }
            } else {
                BreakdownRow("Income", received.incomeCount, received.incomeTotal, "+", currencyCode)
                BreakdownRow(
                    "Transfers received", received.transferInCount,
                    received.transferInTotal, "+", currencyCode
                )
                BreakdownTotal("Received", received.net, currencyCode)

                if (received.refundCount > 0 || received.loanRepaidCount > 0) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "NOT COUNTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    BreakdownRow(
                        "Loan repayments", received.loanRepaidCount, received.loanRepaidTotal, "",
                        currencyCode, muted = true, caption = "Your own money coming back, not income"
                    )
                    BreakdownRow(
                        "Refunds", received.refundCount, received.refundTotal, "",
                        currencyCode, muted = true, caption = "Netted out of Spent instead"
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    count: Int,
    amount: Double,
    sign: String,
    currencyCode: String,
    muted: Boolean = false,
    caption: String? = null
) {
    if (count == 0) return
    val color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$label ($count)", style = MaterialTheme.typography.bodyMedium, color = color)
            caption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = (if (sign.isEmpty()) "" else "$sign ") +
                CurrencyFormatter.format(amount, currencyCode),
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun BreakdownTotal(label: String, amount: Double, currencyCode: String) {
    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            text = CurrencyFormatter.format(amount, currencyCode),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
