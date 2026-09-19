package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.MerchantRule
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.repository.TagRepository
import com.expenseanalyst.domain.usecase.GetExpenseByIdUseCase
import com.expenseanalyst.domain.usecase.SoftDeleteExpenseUseCase
import com.expenseanalyst.domain.util.MerchantRuleMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val ruleSaved: Boolean = false,
    val availableTags: List<Tag> = emptyList(),
    val ruleSelectedTags: List<Tag> = emptyList(),
    val ruleTagSearchQuery: String = "",
    val showLinkBillSheet: Boolean = false,
    val openBills: List<Bill> = emptyList(),
    val linkedBillName: String? = null,
    val linkedBillId: Long? = null,
    val homeCurrency: String = "SAR"
)

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val softDeleteExpenseUseCase: SoftDeleteExpenseUseCase,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val categoryRepository: CategoryRepository,
    private val billRepository: BillRepository,
    private val expenseRepository: ExpenseRepository,
    private val currencyRepository: CurrencyRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val expenseId: Long = checkNotNull(savedStateHandle["expenseId"])
    private val _ui = MutableStateFlow(ExpenseDetailUiState())

    val uiState: StateFlow<ExpenseDetailUiState> = combine(
        combine(
            getExpenseByIdUseCase(expenseId),
            merchantRuleRepository.getRules(),
            categoryRepository.getCategories()
        ) { expense, rules, categories -> Triple(expense, rules, categories) },
        combine(
            billRepository.getBills(),
            currencyRepository.getHomeCurrency(),
            tagRepository.getAllTags()
        ) { bills, homeCurrency, tags -> Triple(bills, homeCurrency, tags) },
        _ui
    ) { (expense, rules, categories), (bills, homeCurrency, allTags), ui ->
        val ruleSearchText = expense?.let {
            it.merchantName?.takeIf { m -> m.isNotBlank() } ?: it.description.takeIf { d -> d.isNotBlank() }
        }
        val existingRule = MerchantRuleMatcher.findMatch(ruleSearchText, rules)
        val openBills = bills.filter { it.status != BillStatus.SETTLED && !it.isDeleted }
        val linkedBill = expense?.billId?.let { bid -> bills.find { it.id == bid } }
        ui.copy(
            expense = expense,
            isLoading = expense == null && !ui.isDeleted,
            categories = categories,
            existingRule = existingRule,
            availableTags = allTags,
            openBills = openBills,
            linkedBillName = linkedBill?.billerName,
            linkedBillId = linkedBill?.id,
            homeCurrency = homeCurrency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseDetailUiState()
    )

    fun showDeleteConfirm() = _ui.update { it.copy(showDeleteConfirm = true) }
    fun dismissDeleteConfirm() = _ui.update { it.copy(showDeleteConfirm = false) }
    fun showEmiSheet() = _ui.update { it.copy(showEmiSheet = true) }
    fun dismissEmiSheet() = _ui.update { it.copy(showEmiSheet = false) }
    fun showRuleDialog() = _ui.update {
        // existingRule lives on the derived `uiState`, not on `_ui` itself — `it.existingRule`
        // here is always the default null, which silently reset the tag picker to empty on
        // every open. Read the current computed value instead.
        it.copy(showRuleDialog = true, ruleSelectedTags = uiState.value.existingRule?.tags ?: emptyList(), ruleTagSearchQuery = "")
    }
    fun dismissRuleDialog() = _ui.update { it.copy(showRuleDialog = false) }
    fun showLinkBillSheet() = _ui.update { it.copy(showLinkBillSheet = true) }
    fun dismissLinkBillSheet() = _ui.update { it.copy(showLinkBillSheet = false) }

    fun onRuleTagSearchQueryChange(value: String) = _ui.update { it.copy(ruleTagSearchQuery = value) }
    fun onRuleTagSelect(tag: Tag) = _ui.update {
        if (it.ruleSelectedTags.any { t -> t.id == tag.id }) it
        else it.copy(ruleSelectedTags = it.ruleSelectedTags + tag, ruleTagSearchQuery = "")
    }
    fun onRuleTagRemove(tag: Tag) = _ui.update {
        it.copy(ruleSelectedTags = it.ruleSelectedTags.filter { t -> t.id != tag.id })
    }
    fun onRuleCreateTag(name: String) {
        viewModelScope.launch {
            val tag = tagRepository.createTag(name.trim())
            _ui.update { it.copy(ruleSelectedTags = it.ruleSelectedTags + tag, ruleTagSearchQuery = "") }
        }
    }

    fun linkToBill(bill: Bill) {
        viewModelScope.launch {
            val expense = uiState.first().expense ?: return@launch
            // Update the expense with the bill link
            expenseRepository.updateExpense(expense.copy(billId = bill.id))
            // Recalculate bill status based on all payments (including this one)
            val paid = (expense.homeAmount ?: expense.amount)
            val billTotalDue = bill.totalDue
            val newStatus = if (billTotalDue == null || paid >= billTotalDue) {
                BillStatus.SETTLED
            } else {
                BillStatus.PARTIAL
            }
            billRepository.updateBill(bill.copy(status = newStatus))
            _ui.update { it.copy(showLinkBillSheet = false) }
        }
    }

    fun deleteExpense() {
        viewModelScope.launch {
            softDeleteExpenseUseCase(expenseId)
            _ui.value = _ui.value.copy(isDeleted = true, showDeleteConfirm = false)
        }
    }

    fun saveRule(merchantPattern: String, category: Category, tags: List<Tag>) {
        viewModelScope.launch {
            merchantRuleRepository.saveRule(
                merchantPattern = merchantPattern,
                categoryId = category.id,
                categoryName = category.name,
                tagIds = tags.map { it.id }
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
