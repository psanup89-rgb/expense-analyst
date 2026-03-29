package com.expenseanalyst.feature.expenses.ui

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.Expense

data class BillDetailUiState(
    val bill: Bill? = null,
    val payments: List<Expense> = emptyList(),
    val totalPaid: Double = 0.0,
    val isLoading: Boolean = true
)
