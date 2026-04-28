package com.expenseanalyst.feature.budget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.PlannedExpense
import com.expenseanalyst.domain.model.SalaryEntry
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.BudgetRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        _uiState.value = _uiState.value.copy(month = now.monthNumber, year = now.year)
    }

    fun onAuthenticated() {
        _uiState.value = _uiState.value.copy(isAuthenticated = true, authRequired = false)
        loadData()
    }

    fun onAuthNotRequired() {
        _uiState.value = _uiState.value.copy(isAuthenticated = true, authRequired = false)
        loadData()
    }

    fun navigateMonth(delta: Int) {
        val state = _uiState.value
        var m = state.month + delta
        var y = state.year
        if (m < 1) { m = 12; y-- }
        if (m > 12) { m = 1; y++ }
        _uiState.value = state.copy(month = m, year = y, isLoading = true)
        loadData()
    }

    private fun loadData() {
        val state = _uiState.value
        viewModelScope.launch {
            val homeCurrency = currencyRepository.getHomeCurrency().first()
            val categories = categoryRepository.getCategories().first()
            val (startMillis, endMillis) = monthRange(state.month, state.year)

            combine(
                budgetRepository.getSalary(state.month, state.year),
                budgetRepository.getPlannedExpenses(state.month, state.year),
                budgetRepository.getIncomeTransactions(startMillis, endMillis),
                expenseRepository.getExpensesByDateRange(
                    Instant.fromEpochMilliseconds(startMillis),
                    Instant.fromEpochMilliseconds(endMillis)
                )
            ) { salary, planned, income, allExpenses ->
                val actual = allExpenses.filter { it.transactionType == TransactionType.EXPENSE }
                val plannedCategoryIds = planned.map { it.categoryId }.toSet()

                val comparisons = planned.groupBy { it.categoryId }.map { (catId, items) ->
                    val cat = categories.find { it.id == catId }
                    val plannedTotal = items.sumOf { it.amount }
                    val actualTotal = actual.filter { it.category.id == catId }
                        .sumOf { it.homeAmount ?: it.amount }
                    CategoryComparison(
                        categoryId = catId,
                        categoryName = cat?.name ?: "Unknown",
                        categoryIconName = cat?.iconName ?: "more_horiz",
                        categoryColorHex = cat?.colorHex ?: "#9E9E9E",
                        plannedAmount = plannedTotal,
                        actualAmount = actualTotal
                    )
                }

                val unplanned = actual.filter { it.category.id !in plannedCategoryIds }
                val showCarry = planned.isEmpty() && salary == null

                state.copy(
                    salary = salary,
                    plannedExpenses = planned,
                    categories = categories,
                    actualExpenses = actual,
                    incomeTransactions = income,
                    homeCurrency = homeCurrency,
                    categoryComparisons = comparisons,
                    unplannedExpenses = unplanned,
                    totalPlanned = planned.sumOf { it.amount },
                    totalActual = actual.sumOf { it.homeAmount ?: it.amount },
                    showCarryForwardPrompt = showCarry,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    // Salary
    fun showSalaryDialog() {
        _uiState.value = _uiState.value.copy(
            showSalaryDialog = true,
            salaryInput = _uiState.value.salary?.amount?.toBigDecimal()?.toPlainString() ?: ""
        )
    }
    fun dismissSalaryDialog() { _uiState.value = _uiState.value.copy(showSalaryDialog = false) }
    fun onSalaryInputChange(v: String) { _uiState.value = _uiState.value.copy(salaryInput = v) }
    fun saveSalary() {
        val state = _uiState.value
        val amount = state.salaryInput.toDoubleOrNull() ?: return
        viewModelScope.launch {
            budgetRepository.saveSalary(SalaryEntry(
                id = state.salary?.id ?: 0,
                amount = amount,
                currencyCode = state.homeCurrency,
                month = state.month, year = state.year
            ))
            _uiState.value = state.copy(showSalaryDialog = false)
        }
    }

    // Income auto-detect
    fun showIncomeSheet() { _uiState.value = _uiState.value.copy(showIncomeSheet = true) }
    fun dismissIncomeSheet() { _uiState.value = _uiState.value.copy(showIncomeSheet = false) }
    fun selectIncomeAsSalary(expense: com.expenseanalyst.domain.model.Expense) {
        val state = _uiState.value
        viewModelScope.launch {
            budgetRepository.saveSalary(SalaryEntry(
                id = state.salary?.id ?: 0,
                amount = expense.homeAmount ?: expense.amount,
                currencyCode = state.homeCurrency,
                month = state.month, year = state.year,
                sourceExpenseId = expense.id
            ))
            _uiState.value = state.copy(showIncomeSheet = false)
        }
    }

    // Salary history
    fun showSalaryHistory() {
        viewModelScope.launch {
            val history = budgetRepository.getSalaryHistory().first()
            _uiState.value = _uiState.value.copy(showSalaryHistory = true, salaryHistory = history)
        }
    }
    fun dismissSalaryHistory() { _uiState.value = _uiState.value.copy(showSalaryHistory = false) }

    // Planned expenses
    fun showAddPlannedSheet(existing: PlannedExpense? = null) {
        _uiState.value = _uiState.value.copy(
            showAddPlannedSheet = true,
            editingPlannedExpense = existing,
            plannedDescription = existing?.description ?: "",
            plannedAmount = existing?.amount?.toBigDecimal()?.toPlainString() ?: "",
            plannedCategoryId = existing?.categoryId
        )
    }
    fun dismissPlannedSheet() { _uiState.value = _uiState.value.copy(showAddPlannedSheet = false) }
    fun onPlannedDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(plannedDescription = v) }
    fun onPlannedAmountChange(v: String) { _uiState.value = _uiState.value.copy(plannedAmount = v) }
    fun onPlannedCategoryChange(id: Long) { _uiState.value = _uiState.value.copy(plannedCategoryId = id) }

    fun savePlannedExpense() {
        val state = _uiState.value
        val amount = state.plannedAmount.toDoubleOrNull() ?: return
        val catId = state.plannedCategoryId ?: return
        if (state.plannedDescription.isBlank()) return
        viewModelScope.launch {
            val item = PlannedExpense(
                id = state.editingPlannedExpense?.id ?: 0,
                description = state.plannedDescription.trim(),
                amount = amount, currencyCode = state.homeCurrency,
                categoryId = catId, month = state.month, year = state.year
            )
            if (state.editingPlannedExpense != null) budgetRepository.updatePlannedExpense(item)
            else budgetRepository.savePlannedExpense(item)
            _uiState.value = state.copy(showAddPlannedSheet = false)
        }
    }

    fun deletePlannedExpense(id: Long) {
        viewModelScope.launch { budgetRepository.softDeletePlannedExpense(id) }
    }

    // Carry forward
    fun carryForward() {
        val state = _uiState.value
        var prevM = state.month - 1; var prevY = state.year
        if (prevM < 1) { prevM = 12; prevY-- }
        viewModelScope.launch {
            budgetRepository.carryForward(prevM, prevY, state.month, state.year)
            _uiState.value = state.copy(showCarryForwardPrompt = false)
        }
    }
    fun dismissCarryForward() { _uiState.value = _uiState.value.copy(showCarryForwardPrompt = false) }

    private fun monthRange(month: Int, year: Int): Pair<Long, Long> {
        val tz = TimeZone.currentSystemDefault()
        val start = LocalDate(year, month, 1).atStartOfDayIn(tz).toEpochMilliseconds()
        val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        val end = nextMonth.atStartOfDayIn(tz).toEpochMilliseconds()
        return start to end
    }
}
