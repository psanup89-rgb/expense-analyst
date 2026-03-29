package com.expenseanalyst.feature.analytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val timeZone = TimeZone.currentSystemDefault()

    private val today: LocalDate get() = Clock.System.now().toLocalDateTime(timeZone).date
    private val _selectedMonth = MutableStateFlow(today.let { LocalDate(it.year, it.month, 1) })
    private val _drillDownFilter = MutableStateFlow<DrillDownFilter?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val expenses = _selectedMonth.flatMapLatest { firstOfMonth ->
        val start = firstOfMonth.atStartOfDayIn(timeZone)
        val end = firstOfMonth.plus(1, DateTimeUnit.MONTH).atStartOfDayIn(timeZone)
        expenseRepository.getExpensesByDateRange(start, end)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val prevMonthExpenses = _selectedMonth.flatMapLatest { firstOfMonth ->
        val prevFirst = firstOfMonth.minus(1, DateTimeUnit.MONTH)
        val start = prevFirst.atStartOfDayIn(timeZone)
        val end = firstOfMonth.atStartOfDayIn(timeZone)
        expenseRepository.getExpensesByDateRange(start, end)
    }

    val uiState = combine(
        _selectedMonth,
        expenses,
        prevMonthExpenses,
        currencyRepository.getHomeCurrency(),
        _drillDownFilter
    ) { selectedMonth, expList, prevList, homeCurrency, drillDown ->

        val active = expList.filter { !it.isDeleted }

        // Totals
        val totalExpense = active
            .filter { it.transactionType == TransactionType.EXPENSE }
            .sumOf { it.homeAmount ?: it.amount }

        val totalIncome = active
            .filter { it.transactionType == TransactionType.INCOME }
            .sumOf { it.homeAmount ?: it.amount }

        val prevMonthExpense = prevList
            .filter { !it.isDeleted && it.transactionType == TransactionType.EXPENSE }
            .sumOf { it.homeAmount ?: it.amount }

        // Days in month for avg
        val daysInMonth = selectedMonth.plus(1, DateTimeUnit.MONTH)
            .minus(1, DateTimeUnit.DAY).dayOfMonth
        val avgDailySpend = if (daysInMonth > 0) totalExpense / daysInMonth else 0.0

        // Category breakdown (EXPENSE only)
        val categoryTotals = active
            .filter { it.transactionType == TransactionType.EXPENSE }
            .groupBy { it.category.id }
            .map { (_, items) ->
                val cat = items.first().category
                val total = items.sumOf { it.homeAmount ?: it.amount }
                Triple(cat, total, items.size)
            }
            .sortedByDescending { it.second }

        val categoryBreakdown = categoryTotals.map { (cat, total, _) ->
            CategorySpend(
                categoryName = cat.name,
                iconName = cat.iconName,
                colorHex = cat.colorHex,
                amount = total,
                percentage = if (totalExpense > 0) (total / totalExpense * 100f).toFloat() else 0f
            )
        }

        // Daily spend (EXPENSE per day-of-month)
        val dailyMap = active
            .filter { it.transactionType == TransactionType.EXPENSE }
            .groupBy { it.date.toLocalDateTime(timeZone).date.dayOfMonth }
            .mapValues { (_, items) -> items.sumOf { it.homeAmount ?: it.amount } }

        val dailySpend = (1..daysInMonth).map { day ->
            DailySpend(day = day, amount = dailyMap[day] ?: 0.0)
        }

        // Top 5 merchants (EXPENSE only, non-null merchants)
        val merchantTotals = active
            .filter { it.transactionType == TransactionType.EXPENSE && !it.merchantName.isNullOrBlank() }
            .groupBy { it.merchantName!! }
            .map { (name, items) ->
                MerchantSpend(
                    name = name,
                    amount = items.sumOf { it.homeAmount ?: it.amount },
                    count = items.size
                )
            }
            .sortedByDescending { it.amount }
            .take(5)

        // Month label
        val monthName = selectedMonth.month.name
            .lowercase().replaceFirstChar { it.uppercase() }
        val monthLabel = "$monthName ${selectedMonth.year}"

        // Can go to next month?
        val currentFirst = today.let { LocalDate(it.year, it.month, 1) }
        val canGoNext = selectedMonth < currentFirst

        // Drill-down
        val drillDownExpenses = when (drillDown) {
            is DrillDownFilter.Spent -> active
                .filter { it.transactionType == TransactionType.EXPENSE }
                .sortedByDescending { it.date }
            is DrillDownFilter.Income -> active
                .filter { it.transactionType == TransactionType.INCOME }
                .sortedByDescending { it.date }
            is DrillDownFilter.ByCategory -> active
                .filter {
                    it.transactionType == TransactionType.EXPENSE &&
                        it.category.name == drillDown.categoryName
                }
                .sortedByDescending { it.date }
            is DrillDownFilter.ByMerchant -> active
                .filter {
                    it.transactionType == TransactionType.EXPENSE &&
                        it.merchantName == drillDown.merchantName
                }
                .sortedByDescending { it.date }
            null -> emptyList()
        }
        val drillDownTitle = when (drillDown) {
            is DrillDownFilter.Spent -> "All Expenses"
            is DrillDownFilter.Income -> "All Income"
            is DrillDownFilter.ByCategory -> drillDown.categoryName
            is DrillDownFilter.ByMerchant -> drillDown.merchantName
            null -> null
        }

        AnalyticsUiState(
            isLoading = false,
            homeCurrencyCode = homeCurrency,
            selectedMonthLabel = monthLabel,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            prevMonthExpense = prevMonthExpense,
            avgDailySpend = avgDailySpend,
            categoryBreakdown = categoryBreakdown,
            dailySpend = dailySpend,
            topMerchants = merchantTotals,
            canGoNext = canGoNext,
            drillDownTitle = drillDownTitle,
            drillDownExpenses = drillDownExpenses
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState()
    )

    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.minus(1, DateTimeUnit.MONTH)
    }

    fun nextMonth() {
        val currentFirst = today.let { LocalDate(it.year, it.month, 1) }
        val next = _selectedMonth.value.plus(1, DateTimeUnit.MONTH)
        if (next <= currentFirst) {
            _selectedMonth.value = next
        }
    }

    fun setDrillDown(filter: DrillDownFilter) {
        _drillDownFilter.value = filter
    }

    fun dismissDrillDown() {
        _drillDownFilter.value = null
    }
}
