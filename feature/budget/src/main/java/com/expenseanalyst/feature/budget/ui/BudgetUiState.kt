package com.expenseanalyst.feature.budget.ui

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PlannedExpense
import com.expenseanalyst.domain.model.SalaryEntry

data class BudgetUiState(
    val isAuthenticated: Boolean = false,
    val authRequired: Boolean = true,
    val month: Int = 0,
    val year: Int = 0,
    val salary: SalaryEntry? = null,
    val plannedExpenses: List<PlannedExpense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val actualExpenses: List<Expense> = emptyList(),
    val incomeTransactions: List<Expense> = emptyList(),
    val homeCurrency: String = "SAR",

    // Planned vs Actual
    val categoryComparisons: List<CategoryComparison> = emptyList(),
    val unplannedExpenses: List<Expense> = emptyList(),
    val totalPlanned: Double = 0.0,
    val totalActual: Double = 0.0,

    // Carry forward
    val showCarryForwardPrompt: Boolean = false,

    // Salary dialog
    val showSalaryDialog: Boolean = false,
    val salaryInput: String = "",
    val showIncomeSheet: Boolean = false,

    // Add/Edit planned expense
    val showAddPlannedSheet: Boolean = false,
    val editingPlannedExpense: PlannedExpense? = null,
    val plannedDescription: String = "",
    val plannedAmount: String = "",
    val plannedCategoryId: Long? = null,

    // Salary history
    val showSalaryHistory: Boolean = false,
    val salaryHistory: List<SalaryEntry> = emptyList(),

    val isLoading: Boolean = true
)

data class CategoryComparison(
    val categoryId: Long,
    val categoryName: String,
    val categoryIconName: String,
    val categoryColorHex: String,
    val plannedAmount: Double,
    val actualAmount: Double
) {
    val progress: Float get() = if (plannedAmount > 0) (actualAmount / plannedAmount).toFloat().coerceIn(0f, 2f) else 0f
    val isOverBudget: Boolean get() = actualAmount > plannedAmount
}
