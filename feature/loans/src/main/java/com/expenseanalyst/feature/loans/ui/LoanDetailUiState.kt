package com.expenseanalyst.feature.loans.ui

import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.util.LoanBalance

data class LoanDetailUiState(
    val item: LentItem? = null,
    /** Transactions linked to this loan, oldest first — both money lent out and repayments. */
    val legs: List<Expense> = emptyList(),
    val ratesByCode: Map<String, CurrencyRate> = emptyMap(),
    /** Lent / repaid / remaining, in the loan's currency. Null until the loan has loaded. */
    val balance: LoanBalance? = null,
    val isLoading: Boolean = true,
    val showSettleDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showReminderPicker: Boolean = false,
    val isSaving: Boolean = false,
    val navigateBack: Boolean = false,
    val error: String? = null
) {
    /** Repaid in full but not yet closed — the screen offers to settle it; it never settles itself. */
    val awaitingSettle: Boolean
        get() = item?.status == LentStatus.PENDING && balance?.isFullyRepaid == true
}
