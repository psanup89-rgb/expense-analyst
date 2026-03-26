package com.expenseanalyst.feature.emi.ui

import com.expenseanalyst.domain.model.EmiGroup
import com.expenseanalyst.domain.model.Expense

data class EmiDetailUiState(
    val group: EmiGroup? = null,
    val installments: List<Expense> = emptyList(),
    val isLoading: Boolean = true,
    val showCancelConfirm: Boolean = false,
    val isCancelling: Boolean = false,
    val isDone: Boolean = false
)
