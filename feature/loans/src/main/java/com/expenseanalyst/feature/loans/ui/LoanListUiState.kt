package com.expenseanalyst.feature.loans.ui

import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.model.LentStatus

data class LoanListUiState(
    val pendingItems: List<LentItem> = emptyList(),
    val settledItems: List<LentItem> = emptyList(),
    val showSettled: Boolean = false,
    val isLoading: Boolean = true
) {
    val displayedItems: List<LentItem>
        get() = if (showSettled) settledItems else pendingItems

    val totalPendingAmount: Double
        get() = pendingItems.sumOf { it.amount }
}
