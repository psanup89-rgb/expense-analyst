package com.expenseanalyst.feature.settings.ui

import com.expenseanalyst.domain.model.Expense

data class TagDetailUiState(
    val isLoading: Boolean = true,
    val tagName: String = "",
    val expenses: List<Expense> = emptyList(),
    val homeCurrencyCode: String = "SAR",
    /** Sum of rows that count as spending (SpendClassifier.isSpend), in home currency. */
    val spentTotal: Double = 0.0,
    /** Rows shown but not in [spentTotal]: income, refunds, loans, own/unclassified transfers. */
    val notCountedCount: Int = 0,
    val tagMissing: Boolean = false
)
