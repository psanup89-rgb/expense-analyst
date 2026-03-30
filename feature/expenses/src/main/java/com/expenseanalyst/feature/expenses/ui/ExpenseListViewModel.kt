package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.util.CurrencyConversion
import com.expenseanalyst.domain.usecase.GetCategoriesUseCase
import com.expenseanalyst.domain.usecase.GetExpensesUseCase
import com.expenseanalyst.domain.usecase.SoftDeleteExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val currencyRepository: CurrencyRepository,
    private val expenseRepository: ExpenseRepository,
    private val softDeleteExpenseUseCase: SoftDeleteExpenseUseCase
) : ViewModel() {

    private val _pendingDeleteId = MutableStateFlow<Long?>(null)
    private var pendingDeleteJob: Job? = null

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedYearMonth = MutableStateFlow<YearMonth?>(run {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        YearMonth(now.year, now.monthNumber)
    })

    init {
        viewModelScope.launch { repairExpenseConversions() }
    }

    val uiState = combine(
        combine(getExpensesUseCase(), getCategoriesUseCase(), currencyRepository.getHomeCurrency()) { a, b, c -> Triple(a, b, c) },
        combine(currencyRepository.getRates(), _selectedCategoryId, _selectedPaymentMethod) { a, b, c -> Triple(a, b, c) },
        combine(_searchQuery, _selectedYearMonth, _pendingDeleteId) { q, ym, pid -> Triple(q, ym, pid) }
    ) { (expenses, categories, homeCurrencyCode), (rates, selectedCatId, selectedPm), (searchQuery, selectedYearMonth, pendingDeleteId) ->
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(tz)
        val nowYm = YearMonth(now.year, now.monthNumber)
        val ratesByCode = rates.associateBy { it.currencyCode }

        val active = expenses
            .filter { !it.isDeleted && it.id != pendingDeleteId }
            .map { expense ->
                val resolved = CurrencyConversion.resolve(
                    expense = expense,
                    homeCurrencyCode = homeCurrencyCode,
                    ratesByCode = ratesByCode
                )
                expense.copy(homeAmount = resolved.homeAmount, exchangeRate = resolved.exchangeRate)
            }

        // Filter by selected month (or all if null)
        val monthFiltered = if (selectedYearMonth != null) {
            active.filter {
                val d = it.date.toLocalDateTime(tz)
                d.year == selectedYearMonth.year && d.monthNumber == selectedYearMonth.month
            }
        } else {
            active
        }

        // Summary totals for selected period
        val monthDebit = monthFiltered
            .filter { it.transactionType == TransactionType.EXPENSE }
            .sumOf { it.homeAmount ?: 0.0 }
        val monthCredit = monthFiltered
            .filter { it.transactionType == TransactionType.INCOME }
            .sumOf { it.homeAmount ?: 0.0 }
        // PAYMENT type (credit card/bill payments) excluded from both totals — it's settling existing debt

        // Apply category, payment method, and search filters
        val filtered = monthFiltered
            .let { list -> if (selectedCatId != null) list.filter { it.category.id == selectedCatId } else list }
            .let { list -> if (selectedPm != null) list.filter { it.paymentMethod == selectedPm } else list }
            .let { list ->
                if (searchQuery.isBlank()) list
                else {
                    val q = searchQuery.trim().lowercase()
                    list.filter {
                        it.description.lowercase().contains(q) ||
                            it.merchantName?.lowercase()?.contains(q) == true ||
                            it.tags.any { tag -> tag.name.lowercase().contains(q) }
                    }
                }
            }

        val canGoNext = selectedYearMonth != null &&
            (selectedYearMonth.year < nowYm.year ||
                (selectedYearMonth.year == nowYm.year && selectedYearMonth.month < nowYm.month))

        val groups = filtered
            .groupBy { DateTimeUtil.formatDateHeader(it.date) }
            .entries
            .sortedByDescending { (_, list) -> list.first().date }
            .map { (header, list) ->
                ExpenseGroup(
                    header = header,
                    expenses = list.sortedByDescending { it.date },
                    dayDebitTotal = list.filter { it.transactionType == TransactionType.EXPENSE }
                        .sumOf { it.homeAmount ?: 0.0 }
                )
            }

        ExpenseListUiState(
            groups = groups,
            categories = categories,
            homeCurrencyCode = homeCurrencyCode,
            selectedCategoryId = selectedCatId,
            selectedPaymentMethod = selectedPm,
            searchQuery = searchQuery,
            selectedYearMonth = selectedYearMonth,
            canGoNext = canGoNext,
            monthTotalDebit = monthDebit,
            monthTotalCredit = monthCredit,
            isLoading = false,
            pendingDeleteId = pendingDeleteId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseListUiState(isLoading = true)
    )

    fun selectCategory(categoryId: Long?) { _selectedCategoryId.value = categoryId }
    fun selectPaymentMethod(method: PaymentMethod?) { _selectedPaymentMethod.value = method }
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun prevMonth() {
        _selectedYearMonth.update { current ->
            val ym = current ?: run {
                val n = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                YearMonth(n.year, n.monthNumber)
            }
            if (ym.month == 1) YearMonth(ym.year - 1, 12)
            else YearMonth(ym.year, ym.month - 1)
        }
    }

    fun nextMonth() {
        _selectedYearMonth.update { current ->
            if (current == null) return@update current
            val next = if (current.month == 12) YearMonth(current.year + 1, 1)
            else YearMonth(current.year, current.month + 1)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val nowYm = YearMonth(now.year, now.monthNumber)
            if (next.year > nowYm.year || (next.year == nowYm.year && next.month > nowYm.month)) current
            else next
        }
    }

    fun selectAllMonths() { _selectedYearMonth.value = null }

    fun deleteExpense(id: Long) {
        pendingDeleteJob?.cancel()
        _pendingDeleteId.value = id
        pendingDeleteJob = viewModelScope.launch {
            delay(UNDO_TIMEOUT_MS)
            softDeleteExpenseUseCase(id)
            _pendingDeleteId.value = null
        }
    }

    fun undoDelete() {
        pendingDeleteJob?.cancel()
        pendingDeleteJob = null
        _pendingDeleteId.value = null
    }

    companion object {
        private const val UNDO_TIMEOUT_MS = 4_000L
    }

    private suspend fun repairExpenseConversions() {
        if (currencyRepository.isStale()) {
            currencyRepository.refreshRates()
        }
        val homeCurrencyCode = currencyRepository.getHomeCurrency().first()
        val ratesByCode = currencyRepository.getRates().first().associateBy { it.currencyCode }
        expenseRepository.getExpensesSnapshot(includeDeleted = true).forEach { expense ->
            if (CurrencyConversion.needsSync(expense, homeCurrencyCode, ratesByCode)) {
                val resolved = CurrencyConversion.resolve(
                    expense = expense,
                    homeCurrencyCode = homeCurrencyCode,
                    ratesByCode = ratesByCode
                )
                expenseRepository.updateExpense(
                    expense.copy(homeAmount = resolved.homeAmount, exchangeRate = resolved.exchangeRate)
                )
            }
        }
    }
}
