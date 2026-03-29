package com.expenseanalyst.feature.settings.ui

import com.expenseanalyst.domain.model.Category

data class CategoryManagementUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingCategory: Category? = null,
    val dialogName: String = "",
    val dialogIconName: String = "more_horiz",
    val isSaving: Boolean = false
)
