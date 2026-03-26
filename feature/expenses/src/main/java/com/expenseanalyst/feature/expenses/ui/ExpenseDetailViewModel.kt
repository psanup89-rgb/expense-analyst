package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.MerchantRule
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.usecase.GetExpenseByIdUseCase
import com.expenseanalyst.domain.usecase.SoftDeleteExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseDetailUiState(
    val expense: Expense? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showEmiSheet: Boolean = false,
    val showRuleDialog: Boolean = false,
    val categories: List<Category> = emptyList(),
    val existingRule: MerchantRule? = null,
    val ruleSaved: Boolean = false
)

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val softDeleteExpenseUseCase: SoftDeleteExpenseUseCase,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val expenseId: Long = checkNotNull(savedStateHandle["expenseId"])
    private val _ui = MutableStateFlow(ExpenseDetailUiState())

    val uiState: StateFlow<ExpenseDetailUiState> = combine(
        getExpenseByIdUseCase(expenseId),
        merchantRuleRepository.getRules(),
        categoryRepository.getCategories(),
        _ui
    ) { expense, rules, categories, ui ->
        val ruleSearchText = expense?.let {
            it.merchantName?.takeIf { m -> m.isNotBlank() } ?: it.description.takeIf { d -> d.isNotBlank() }
        }
        val existingRule = ruleSearchText?.let { text ->
            rules.firstOrNull { text.lowercase().contains(it.merchantPattern.lowercase()) }
        }
        ui.copy(
            expense = expense,
            isLoading = expense == null && !ui.isDeleted,
            categories = categories,
            existingRule = existingRule
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseDetailUiState()
    )

    fun showDeleteConfirm() = _ui.value.let { _ui.value = it.copy(showDeleteConfirm = true) }
    fun dismissDeleteConfirm() = _ui.value.let { _ui.value = it.copy(showDeleteConfirm = false) }
    fun showEmiSheet() = _ui.value.let { _ui.value = it.copy(showEmiSheet = true) }
    fun dismissEmiSheet() = _ui.value.let { _ui.value = it.copy(showEmiSheet = false) }
    fun showRuleDialog() = _ui.value.let { _ui.value = it.copy(showRuleDialog = true) }
    fun dismissRuleDialog() = _ui.value.let { _ui.value = it.copy(showRuleDialog = false) }

    fun deleteExpense() {
        viewModelScope.launch {
            softDeleteExpenseUseCase(expenseId)
            _ui.value = _ui.value.copy(isDeleted = true, showDeleteConfirm = false)
        }
    }

    fun saveRule(merchantPattern: String, category: Category) {
        viewModelScope.launch {
            merchantRuleRepository.saveRule(
                merchantPattern = merchantPattern,
                categoryId = category.id,
                categoryName = category.name
            )
            _ui.value = _ui.value.copy(showRuleDialog = false, ruleSaved = true)
        }
    }

    fun deleteRule() {
        viewModelScope.launch {
            uiState.first().existingRule?.let { merchantRuleRepository.deleteRule(it.id) }
        }
    }

    fun clearRuleSaved() {
        _ui.value = _ui.value.copy(ruleSaved = false)
    }
}
