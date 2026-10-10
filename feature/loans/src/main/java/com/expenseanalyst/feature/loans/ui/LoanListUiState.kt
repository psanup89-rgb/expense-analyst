package com.expenseanalyst.feature.loans.ui

import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.util.LoanBalance

data class LoanListUiState(
    val pendingItems: List<LentItem> = emptyList(),
    val settledItems: List<LentItem> = emptyList(),
    /** Each loan's balance, keyed by loan id. */
    val balances: Map<Long, LoanBalance> = emptyMap(),
    /** What is still owed across all pending loans, in [homeCurrency]. */
    val totalPendingHome: Double = 0.0,
    val homeCurrency: String = "",
    val showSettled: Boolean = false,
    val isLoading: Boolean = true
) {
    val displayedItems: List<LentItem>
        get() = if (showSettled) settledItems else pendingItems
}
