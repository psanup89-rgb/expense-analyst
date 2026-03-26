package com.expenseanalyst.feature.emi.ui

import com.expenseanalyst.domain.model.EmiGroup

data class EmiListUiState(
    val activeGroups: List<EmiGroup> = emptyList(),
    val completedGroups: List<EmiGroup> = emptyList(),
    val isLoading: Boolean = true,
    val showCompleted: Boolean = false
)
