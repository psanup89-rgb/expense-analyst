package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.MerchantRule
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.usecase.GetExpenseByIdUseCase
import com.expenseanalyst.domain.usecase.SoftDeleteExpenseUseCase
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
    val showLinkBillSheet: Boolean = false,
    val openBills: List<Bill> = emptyList(),
    val linkedBillName: String? = null,
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
    private val currencyRepository: CurrencyRepository
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
            _ui
        ) { bills, homeCurrency, ui -> Triple(bills, homeCurrency, ui) }
    ) { (expense, rules, categories), (bills, homeCurrency, ui) ->
        val ruleSearchText = expense?.let {
            it.merchantName?.takeIf { m -> m.isNotBlank() } ?: it.description.takeIf { d -> d.isNotBlank() }
        }
        val existingRule = ruleSearchText?.let { text ->
            rules.firstOrNull { text.lowercase().contains(it.merchantPattern.lowercase()) }
        }
        val openBills = bills.filter { it.status != BillStatus.SETTLED && !it.isDeleted }
        val linkedBillName = expense?.billId?.let { bid -> bills.find { it.id == bid }?.billerName }
        ui.copy(
            expense = expense,
            isLoading = expense == null && !ui.isDeleted,
            categories = categories,
            existingRule = existingRule,
            openBills = openBills,
            linkedBillName = linkedBillName,
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
    fun showRuleDialog() = _ui.update { it.copy(showRuleDialog = true) }
    fun dismissRuleDialog() = _ui.update { it.copy(showRuleDialog = false) }
    fun showLinkBillSheet() = _ui.update { it.copy(showLinkBillSheet = true) }
    fun dismissLinkBillSheet() = _ui.update { it.copy(showLinkBillSheet = false) }

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
