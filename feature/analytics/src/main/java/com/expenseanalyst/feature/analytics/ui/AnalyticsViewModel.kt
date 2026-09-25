package com.expenseanalyst.feature.analytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.util.SpendClassifier
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

        // Totals — refunds (INCOME with category=Refund) are netted out of Total Spent
        // and excluded from Total Income so the dashboard reflects real net spend (issue #12).
        // Aggregations run on SpendClassifier.isSpend rather than a local EXPENSE check, so a
        // transfer the user marked as sent to someone else counts here exactly as it does on the
        // home screen's Spent card. The previous month uses the same predicate — otherwise the
        // month-over-month delta would compare unlike quantities and report a phantom jump.
        val spend = SpendClassifier.spendBreakdown(active)
        val refundTotal = spend.refundTotal
        val totalExpense = spend.net
        val totalIncome = SpendClassifier.receivedBreakdown(active).net
        val prevMonthExpense = SpendClassifier.spendBreakdown(prevList.filter { !it.isDeleted }).net

        // Days in month for avg
        val daysInMonth = selectedMonth.plus(1, DateTimeUnit.MONTH)
            .minus(1, DateTimeUnit.DAY).dayOfMonth
        val avgDailySpend = if (daysInMonth > 0) totalExpense / daysInMonth else 0.0

        // Category breakdown (EXPENSE only)
        val categoryTotals = active
            .filter(SpendClassifier::isSpend)
            .groupBy { it.category.id }
            .map { (_, items) ->
                val cat = items.first().category
                val total = items.sumOf(SpendClassifier::homeValue)
                Triple(cat, total, items.size)
            }
            .sortedByDescending { it.second }

        // BNPL split payments (Tabby/Tamara) — deliberately excluded from totalExpense (the
        // owner's decision), but still surfaced as their own bucket so the spend is visible.
        // Rows are PAYMENT type, not EXPENSE, so they sit entirely outside categoryTotals above.
        val splitPaymentRows = active.filter {
            it.transactionType == TransactionType.PAYMENT && it.category.name == SPLIT_PAYMENTS_CATEGORY
        }
        val splitPaymentsSpend = splitPaymentRows.firstOrNull()?.let { sample ->
            CategorySpend(
                categoryName = sample.category.name,
                iconName = sample.category.iconName,
                colorHex = sample.category.colorHex,
                amount = splitPaymentRows.sumOf(SpendClassifier::homeValue),
                percentage = 0f,
                isExcludedFromTotal = true,
                drillDownFilter = DrillDownFilter.ByCategory(sample.category.name)
            )
        }

        // Transfers between the user's own accounts. Not spending, so excluded from
        // totalExpense and from the percentage denominator — same treatment as Split Payments
        // above — but surfaced as its own bucket so the money movement is visible instead of
        // vanishing. Icon and colour are hardcoded to the seeded Transfer category's rather than
        // read off a row, because own-transfer rows keep whatever category they were filed
        // under; that is also why this needs no seeded category of its own.
        val ownTransferRows = active.filter(SpendClassifier::isOwnTransfer)
        val ownTransfersSpend = ownTransferRows.takeIf { it.isNotEmpty() }?.let { rows ->
            CategorySpend(
                categoryName = OWN_TRANSFERS_BUCKET,
                iconName = "swap_horiz",
                colorHex = "#607D8B",
                amount = rows.sumOf(SpendClassifier::homeValue),
                percentage = 0f,
                isExcludedFromTotal = true,
                drillDownFilter = DrillDownFilter.OwnTransfers
            )
        }

        val categoryBreakdown = categoryTotals.map { (cat, total, _) ->
            CategorySpend(
                categoryName = cat.name,
                iconName = cat.iconName,
                colorHex = cat.colorHex,
                amount = total,
                percentage = if (totalExpense > 0) (total / totalExpense * 100f).toFloat() else 0f,
                drillDownFilter = DrillDownFilter.ByCategory(cat.name)
            )
        } + listOfNotNull(splitPaymentsSpend, ownTransfersSpend)

        // Daily spend (EXPENSE per day-of-month)
        val dailyMap = active
            .filter(SpendClassifier::isSpend)
            .groupBy { it.date.toLocalDateTime(timeZone).date.dayOfMonth }
            .mapValues { (_, items) -> items.sumOf(SpendClassifier::homeValue) }

        val dailySpend = (1..daysInMonth).map { day ->
            DailySpend(day = day, amount = dailyMap[day] ?: 0.0)
        }

        // Top 5 merchants (EXPENSE only, non-null merchants)
        val merchantTotals = active
            .filter { SpendClassifier.isSpend(it) && !it.merchantName.isNullOrBlank() }
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
                .filter(SpendClassifier::isSpend)
                .sortedByDescending { it.date }
            is DrillDownFilter.Income -> active
                .filter(SpendClassifier::isReceived)
                .sortedByDescending { it.date }
            is DrillDownFilter.OwnTransfers -> active
                .filter(SpendClassifier::isOwnTransfer)
                .sortedByDescending { it.date }
            is DrillDownFilter.ByCategory -> {
                // Split Payments rows are PAYMENT type, not EXPENSE — scoped to exactly this
                // one category name so every other category's drill-down stays EXPENSE-only
                // and therefore stays consistent with the EXPENSE-only sum shown on its bar.
                val includePayment = drillDown.categoryName == SPLIT_PAYMENTS_CATEGORY
                active.filter {
                    it.category.name == drillDown.categoryName &&
                        (SpendClassifier.isSpend(it) ||
                            (includePayment && it.transactionType == TransactionType.PAYMENT))
                }.sortedByDescending { it.date }
            }
            is DrillDownFilter.ByMerchant -> active
                .filter {
                    SpendClassifier.isSpend(it) &&
                        it.merchantName == drillDown.merchantName
                }
                .sortedByDescending { it.date }
            null -> emptyList()
        }
        val drillDownTitle = when (drillDown) {
            is DrillDownFilter.Spent -> "All Expenses"
            is DrillDownFilter.OwnTransfers -> OWN_TRANSFERS_BUCKET
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

    private companion object {
        const val REFUND_CATEGORY = "Refund"
        const val SPLIT_PAYMENTS_CATEGORY = "Split Payments"
        const val OWN_TRANSFERS_BUCKET = "Own Transfers"
    }
}
