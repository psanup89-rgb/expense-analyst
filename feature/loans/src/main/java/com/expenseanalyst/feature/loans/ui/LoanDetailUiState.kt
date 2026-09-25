package com.expenseanalyst.feature.loans.ui

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.LentItem

data class LoanDetailUiState(
    val item: LentItem? = null,
    /** Transactions linked to this loan, oldest first — both money lent out and repayments. */
    val legs: List<Expense> = emptyList(),
    val isLoading: Boolean = true,
    val showSettleDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showReminderPicker: Boolean = false,
    val isSaving: Boolean = false,
    val navigateBack: Boolean = false,
    val error: String? = null
)
