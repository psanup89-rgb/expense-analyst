package com.expenseanalyst.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val formState = MutableStateFlow(CategoryManagementUiState())

    val uiState = combine(
        categoryRepository.getCategories(),
        formState
    ) { categories, state ->
        state.copy(
            categories = categories.sortedBy { it.sortOrder },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryManagementUiState()
    )

    fun showAddDialog() {
        formState.update {
            it.copy(
                showAddDialog = true,
                showEditDialog = false,
                editingCategory = null,
                dialogName = "",
                dialogIconName = "more_horiz"
            )
        }
    }

    fun showEditDialog(category: Category) {
        formState.update {
            it.copy(
                showEditDialog = true,
                showAddDialog = false,
                editingCategory = category,
                dialogName = category.name,
                dialogIconName = category.iconName
            )
        }
    }

    fun dismissDialog() {
        formState.update {
            it.copy(
                showAddDialog = false,
                showEditDialog = false,
                editingCategory = null,
                dialogName = "",
                dialogIconName = "more_horiz"
            )
        }
    }

    fun onNameChange(name: String) {
        formState.update { it.copy(dialogName = name) }
    }

    fun onIconChange(iconName: String) {
        formState.update { it.copy(dialogIconName = iconName) }
    }

    fun saveCategory() {
        val current = formState.value
        val name = current.dialogName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            formState.update { it.copy(isSaving = true) }

            runCatching {
                if (current.showEditDialog && current.editingCategory != null) {
                    categoryRepository.updateCategory(
                        current.editingCategory.copy(
                            name = name,
                            iconName = current.dialogIconName
                        )
                    )
                } else {
                    val maxSortOrder = uiState.value.categories.maxOfOrNull { it.sortOrder } ?: -1
                    categoryRepository.addCategory(
                        Category(
                            name = name,
                            iconName = current.dialogIconName,
                            colorHex = "#9E9E9E",
                            isDefault = false,
                            sortOrder = maxSortOrder + 1
                        )
                    )
                }
            }

            formState.update {
                it.copy(
                    isSaving = false,
                    showAddDialog = false,
                    showEditDialog = false,
                    editingCategory = null,
                    dialogName = "",
                    dialogIconName = "more_horiz"
                )
            }
        }
    }

    fun deleteCategory(category: Category) {
        if (category.isDefault) return
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}
