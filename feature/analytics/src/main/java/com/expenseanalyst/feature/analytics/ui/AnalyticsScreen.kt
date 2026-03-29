package com.expenseanalyst.feature.analytics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.core.theme.NeonGreen
import com.expenseanalyst.core.theme.NeonRed
import com.expenseanalyst.core.theme.NeonYellow
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.core.util.categoryIconVector
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack: () -> Unit,
    onExpenseClick: (Long) -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Month navigation
                item {
                    MonthNavRow(
                        uiState = uiState,
                        onPrev = viewModel::prevMonth,
                        onNext = viewModel::nextMonth
                    )
                }

                // 2. Summary cards
                item {
                    SummaryRow(
                        uiState = uiState,
                        onSpentClick = { viewModel.setDrillDown(DrillDownFilter.Spent) },
                        onIncomeClick = { viewModel.setDrillDown(DrillDownFilter.Income) }
                    )
                }

                // 3. Category breakdown
                if (uiState.categoryBreakdown.isNotEmpty()) {
                    item {
                        SectionCard(title = "Spending by Category") {
                            uiState.categoryBreakdown.forEachIndexed { i, cat ->
                                CategoryBar(
                                    cat = cat,
                                    currencyCode = uiState.homeCurrencyCode,
                                    onClick = {
                                        viewModel.setDrillDown(DrillDownFilter.ByCategory(cat.categoryName))
                                    }
                                )
                                if (i < uiState.categoryBreakdown.lastIndex) {
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }

                // 4. Daily spend chart
                if (uiState.dailySpend.any { it.amount > 0 }) {
                    item {
                        SectionCard(title = "Daily Spending") {
                            DailyBarChart(dailySpend = uiState.dailySpend)
                        }
                    }
                }

                // 5. Top merchants
                if (uiState.topMerchants.isNotEmpty()) {
                    item {
                        SectionCard(title = "Top Merchants") {
                            uiState.topMerchants.forEachIndexed { i, merchant ->
                                MerchantRow(
                                    rank = i + 1,
                                    merchant = merchant,
                                    currencyCode = uiState.homeCurrencyCode,
                                    onClick = {
                                        viewModel.setDrillDown(DrillDownFilter.ByMerchant(merchant.name))
                                    }
                                )
                                if (i < uiState.topMerchants.lastIndex) {
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }

                // Empty state
                if (uiState.categoryBreakdown.isEmpty() && uiState.topMerchants.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expenses recorded for this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Drill-down bottom sheet — capture title to enable smart cast
    val drillDownTitle = uiState.drillDownTitle
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
                onExpenseClick = { id ->
                    viewModel.dismissDrillDown()
                    onExpenseClick(id)
                }
            )
        }
    }
}

@Composable
private fun MonthNavRow(
    uiState: AnalyticsUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous month",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = uiState.selectedMonthLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNext, enabled = uiState.canGoNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next month",
                tint = if (uiState.canGoNext) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SummaryRow(
    uiState: AnalyticsUiState,
    onSpentClick: () -> Unit,
    onIncomeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            label = "Spent",
            value = CurrencyFormatter.format(uiState.totalExpense, uiState.homeCurrencyCode),
            valueColor = NeonYellow,
            onClick = onSpentClick,
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Income",
            value = CurrencyFormatter.format(uiState.totalIncome, uiState.homeCurrencyCode),
            valueColor = NeonGreen,
            onClick = onIncomeClick,
            modifier = Modifier.weight(1f)
        )

        val delta = if (uiState.prevMonthExpense > 0) {
            ((uiState.totalExpense - uiState.prevMonthExpense) / uiState.prevMonthExpense * 100).toInt()
        } else {
            0
        }
        val deltaText: String
        val deltaColor: Color
        val deltaIcon: androidx.compose.ui.graphics.vector.ImageVector?
        when {
            uiState.prevMonthExpense <= 0 -> {
                deltaText = "N/A"
                deltaColor = MaterialTheme.colorScheme.onSurfaceVariant
                deltaIcon = null
            }
            delta > 0 -> {
                deltaText = "+$delta%"
                deltaColor = NeonRed
                deltaIcon = Icons.AutoMirrored.Filled.TrendingUp
            }
            delta < 0 -> {
                deltaText = "$delta%"
                deltaColor = NeonGreen
                deltaIcon = Icons.AutoMirrored.Filled.TrendingDown
            }
            else -> {
                deltaText = "0%"
                deltaColor = MaterialTheme.colorScheme.onSurfaceVariant
                deltaIcon = null
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(Modifier.padding(10.dp)) {
                Text(
                    text = "vs Last",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (deltaIcon != null) {
                        Icon(
                            imageVector = deltaIcon,
                            contentDescription = null,
                            tint = deltaColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "tap to view →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CategoryBar(cat: CategorySpend, currencyCode: String, onClick: () -> Unit) {
    val barColor = try {
        val hex = if (cat.colorHex.startsWith("#")) cat.colorHex else "#${cat.colorHex}"
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        NeonGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = categoryIconVector(cat.iconName),
            contentDescription = cat.categoryName,
            tint = barColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cat.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = CurrencyFormatter.format(cat.amount, currencyCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (cat.percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${cat.percentage.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyBarChart(dailySpend: List<DailySpend>) {
    val maxAmount = dailySpend.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val totalBars = dailySpend.size
        if (totalBars == 0) return@Canvas

        val slotWidth = size.width / totalBars
        val barWidth = slotWidth * 0.6f
        val gap = slotWidth * 0.4f

        dailySpend.forEachIndexed { index, ds ->
            val barHeight = (ds.amount / maxAmount * size.height).toFloat()
            if (barHeight > 0f) {
                val left = index * slotWidth + gap / 2f
                val top = size.height - barHeight
                drawRoundRect(
                    color = NeonGreen,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        }
    }

    // X-axis day labels
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val maxDay = dailySpend.size
        listOf(1, 7, 14, 21, 28).filter { it <= maxDay }.forEach { day ->
            Text(
                text = "$day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MerchantRow(rank: Int, merchant: MerchantSpend, currencyCode: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = merchant.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${merchant.count} transaction${if (merchant.count != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = CurrencyFormatter.format(merchant.amount, currencyCode),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = NeonYellow
        )
    }
}

// ── Drill-down bottom sheet ───────────────────────────────────────────────────

@Composable
private fun DrillDownSheet(
    title: String,
    expenses: List<Expense>,
    currencyCode: String,
    onExpenseClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${expenses.size} transaction${if (expenses.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(expenses, key = { it.id }) { expense ->
                    DrillDownExpenseRow(
                        expense = expense,
                        currencyCode = currencyCode,
                        onClick = { onExpenseClick(expense.id) }
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun DrillDownExpenseRow(
    expense: Expense,
    currencyCode: String,
    onClick: () -> Unit
) {
    val catColor = try {
        val hex = if (expense.category.colorHex.startsWith("#")) expense.category.colorHex
        else "#${expense.category.colorHex}"
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        NeonGreen
    }

    val isCredit = expense.transactionType == TransactionType.INCOME
    val amountColor = if (isCredit) NeonGreen else NeonYellow
    val amountPrefix = if (isCredit) "+" else "-"
    val displayAmount = expense.homeAmount ?: expense.amount

    val primaryLabel = expense.merchantName?.takeIf { it.isNotBlank() }
        ?: expense.description.takeIf { it.isNotBlank() }
        ?: expense.category.name
    val dateLabel = DateTimeUtil.formatDateHeader(expense.date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon circle
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIconVector(expense.category.iconName),
                contentDescription = null,
                tint = catColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Text info
        Column(Modifier.weight(1f)) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$dateLabel · ${expense.category.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Amount
        Text(
            text = "$amountPrefix${CurrencyFormatter.format(displayAmount, currencyCode)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}
