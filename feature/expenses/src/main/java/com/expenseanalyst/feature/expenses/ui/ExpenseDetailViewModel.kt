package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.MerchantRule
import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.model.TransferRecipientRule
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.repository.LentRepository
import com.expenseanalyst.domain.repository.TagRepository
import com.expenseanalyst.domain.repository.TransferRecipientRuleRepository
import com.expenseanalyst.domain.usecase.GetExpenseByIdUseCase
import com.expenseanalyst.domain.usecase.SoftDeleteExpenseUseCase
import com.expenseanalyst.domain.util.MerchantRuleMatcher
import com.expenseanalyst.domain.util.SpendClassifier
import com.expenseanalyst.domain.util.TransferRecipientMatcher
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
    val homeCurrency: String = "SAR",
    val showLoanSheet: Boolean = false,
    /** Loans still awaiting repayment, offered as link targets. */
    val pendingLoans: List<LentItem> = emptyList(),
    /** The loan this row is already linked to, if any. */
    val linkedLoan: LentItem? = null,
    val showTransferSheet: Boolean = false,
    /** Non-null when a remembered rule already exists for this transfer's recipient. */
    val existingTransferRule: TransferRecipientRule? = null
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
    private val tagRepository: TagRepository,
    private val transferRecipientRuleRepository: TransferRecipientRuleRepository,
    private val lentRepository: LentRepository
) : ViewModel() {

    private val expenseId: Long = checkNotNull(savedStateHandle["expenseId"])
    private val _ui = MutableStateFlow(ExpenseDetailUiState())

    val uiState: StateFlow<ExpenseDetailUiState> = combine(
        combine(
            getExpenseByIdUseCase(expenseId),
            merchantRuleRepository.getRules(),
            categoryRepository.getCategories()
        ) { expense, rules, categories -> Triple(expense, rules, categories) },
        // Four sources in this arm, so it returns a data holder rather than a Triple — the
        // 3-arg combine overload is already at its limit.
        combine(
            billRepository.getBills(),
            currencyRepository.getHomeCurrency(),
            tagRepository.getAllTags(),
            transferRecipientRuleRepository.getRules(),
            lentRepository.getLentItems()
        ) { bills, homeCurrency, tags, transferRules, lentItems ->
            DetailSideData(bills, homeCurrency, tags, transferRules, lentItems)
        },
        _ui
    ) { (expense, rules, categories), side, ui ->
        val (bills, homeCurrency, allTags, transferRules, lentItems) = side
        val ruleSearchText = expense?.let {
            it.merchantName?.takeIf { m -> m.isNotBlank() } ?: it.description.takeIf { d -> d.isNotBlank() }
        }
        val existingRule = MerchantRuleMatcher.findMatch(ruleSearchText, rules)
        val openBills = bills.filter { it.status != BillStatus.SETTLED && !it.isDeleted }
        val linkedBill = expense?.billId?.let { bid -> bills.find { it.id == bid } }
        val existingTransferRule = expense
            ?.takeIf { it.transactionType == TransactionType.TRANSFER }
            ?.let { TransferRecipientMatcher.findRule(it.merchantName, transferRules) }
        ui.copy(
            expense = expense,
            isLoading = expense == null && !ui.isDeleted,
            categories = categories,
            existingRule = existingRule,
            availableTags = allTags,
            openBills = openBills,
            linkedBillName = linkedBill?.billerName,
            linkedBillId = linkedBill?.id,
            homeCurrency = homeCurrency,
            existingTransferRule = existingTransferRule,
            pendingLoans = lentItems.filter { it.status == LentStatus.PENDING },
            linkedLoan = expense?.loanId?.let { id -> lentItems.find { it.id == id } }
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

    fun showLoanSheet() = _ui.update { it.copy(showLoanSheet = true) }
    fun dismissLoanSheet() = _ui.update { it.copy(showLoanSheet = false) }

    /**
     * Starts a new loan from this row (money the user lent out) and links the row as its first
     * outgoing leg. The loan's principal is stored on the loan itself, not derived from its legs,
     * so a loan whose lending predates the app's history can still exist and receive repayments.
     */
    fun startLoanFromThisRow() {
        viewModelScope.launch {
            val expense = uiState.first().expense ?: return@launch
            val person = expense.merchantName?.takeIf { it.isNotBlank() } ?: return@launch
            val loanId = lentRepository.addLentItem(
                LentItem(
                    personName = person,
                    amount = expense.amount,
                    currencyCode = expense.currencyCode,
                    homeAmount = expense.homeAmount,
                    description = "Lent to $person",
                    lentDateMillis = expense.date.toEpochMilliseconds(),
                    linkedExpenseId = expense.id
                )
            )
            expenseRepository.linkToLoan(expense.id, loanId, isRepayment = false)
            _ui.update { it.copy(showLoanSheet = false) }
        }
    }

    /**
     * Links this row to an existing loan. An outgoing leg in the loan's own currency also raises
     * the recorded principal, so lending split across several transfers adds up; a leg in another
     * currency is linked but leaves the principal alone rather than guess a conversion.
     */
    fun linkToLoan(loan: LentItem, isRepayment: Boolean) {
        viewModelScope.launch {
            val expense = uiState.first().expense ?: return@launch
            expenseRepository.linkToLoan(expense.id, loan.id, isRepayment)
            if (!isRepayment && expense.currencyCode == loan.currencyCode) {
                val loanHome = loan.homeAmount
                val legHome = expense.homeAmount
                lentRepository.updateLentItem(
                    loan.copy(
                        amount = loan.amount + expense.amount,
                        homeAmount = if (loanHome != null && legHome != null) loanHome + legHome else loanHome
                    )
                )
            }
            _ui.update { it.copy(showLoanSheet = false) }
        }
    }

    /**
     * Mirror of [linkToLoan]: an outgoing leg in the loan's currency added its amount to the
     * principal when linked, so it must take it back off here. Without this, unlinking and
     * relinking the same transfer silently doubled the loan.
     */
    fun unlinkFromLoan() {
        viewModelScope.launch {
            val state = uiState.first()
            val expense = state.expense ?: return@launch
            val loan = state.linkedLoan
            val wasOutgoing = !SpendClassifier.isLoanRepayment(expense)
            expenseRepository.unlinkFromLoan(expense.id)
            if (loan != null && wasOutgoing && expense.currencyCode == loan.currencyCode) {
                val loanHome = loan.homeAmount
                val legHome = expense.homeAmount
                lentRepository.updateLentItem(
                    loan.copy(
                        amount = (loan.amount - expense.amount).coerceAtLeast(0.0),
                        homeAmount = if (loanHome != null && legHome != null) {
                            (loanHome - legHome).coerceAtLeast(0.0)
                        } else {
                            loanHome
                        }
                    )
                )
            }
        }
    }

    fun showTransferSheet() = _ui.update { it.copy(showTransferSheet = true) }
    fun dismissTransferSheet() = _ui.update { it.copy(showTransferSheet = false) }

    /**
     * Records the classification on this one row, and optionally remembers it for the recipient
     * so future transfers to the same name classify themselves. Remembering is forward-only —
     * rows already captured keep whatever they have, so no past total shifts underneath the
     * user. The row update is a targeted DAO write, not updateExpense, which would null
     * account_number and rewrite the tag join table.
     */
    fun classifyTransfer(classification: TransferClassification, remember: Boolean) {
        viewModelScope.launch {
            expenseRepository.classifyTransfer(expenseId, classification)
            if (remember) {
                uiState.first().expense?.merchantName?.takeIf { it.isNotBlank() }?.let { name ->
                    transferRecipientRuleRepository.saveRule(name, classification)
                }
            }
            _ui.update { it.copy(showTransferSheet = false) }
        }
    }

    fun forgetTransferRule() {
        viewModelScope.launch {
            uiState.first().existingTransferRule?.let {
                transferRecipientRuleRepository.deleteRule(it.recipientDisplayName)
            }
        }
    }
}

/** Four-field holder for one arm of [ExpenseDetailViewModel]'s combine. */
private data class DetailSideData(
    val bills: List<Bill>,
    val homeCurrency: String,
    val tags: List<Tag>,
    val transferRules: List<TransferRecipientRule>,
    val lentItems: List<LentItem>
)
