package com.expenseanalyst.feature.expenses.ui

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.util.ReceivedBreakdown
import com.expenseanalyst.domain.util.SpendBreakdown

data class YearMonth(val year: Int, val month: Int) {
    val label: String = "${MONTH_NAMES[month]} $year"

    companion object {
        private val MONTH_NAMES = arrayOf(
            "", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }
}

data class ExpenseListUiState(
    val groups: List<ExpenseGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val homeCurrencyCode: String = "INR",
    val selectedCategoryId: Long? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val searchQuery: String = "",
    val selectedYearMonth: YearMonth? = null,
    val canGoNext: Boolean = false,
    val monthTotalDebit: Double = 0.0,
    val monthTotalCredit: Double = 0.0,
    /** Components of [monthTotalDebit], for the "how Spent was calculated" sheet. */
    val spendBreakdown: SpendBreakdown = SpendBreakdown(),
    /** Components of [monthTotalCredit], for the "how Received was calculated" sheet. */
    val receivedBreakdown: ReceivedBreakdown = ReceivedBreakdown(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val accountDisplayNames: Map<Long, String> = emptyMap(),
    val pendingDeleteId: Long? = null
)

data class ExpenseGroup(
    val header: String,
    val expenses: List<Expense>,
    /** Sum of rows that count as spending — see SpendClassifier.isSpend. */
    val daySpendTotal: Double
)
