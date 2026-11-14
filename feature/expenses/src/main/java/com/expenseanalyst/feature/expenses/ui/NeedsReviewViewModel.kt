package com.expenseanalyst.feature.expenses.ui

import com.expenseanalyst.domain.util.ReviewCategoryPlanner
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.Category
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.TransferRecipientRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.expenseanalyst.domain.util.BnplPlan
import com.expenseanalyst.domain.util.BnplPlans
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.expenseanalyst.domain.util.ReimbursementMatcher
import javax.inject.Inject

@HiltViewModel
class NeedsReviewViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val transferRecipientRuleRepository: TransferRecipientRuleRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantRuleRepository: MerchantRuleRepository,
    currencyRepository: CurrencyRepository
) : ViewModel() {

    val expenses: StateFlow<List<com.expenseanalyst.domain.model.Expense>> =
        expenseRepository.getNeedsReviewExpenses()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val homeCurrencyCode: StateFlow<String> = currencyRepository.getHomeCurrency()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "SAR"
        )

    /** Tabby/Tamara plans, for linking an unmatched charge from its Review card. */
    val bnplPlans: StateFlow<List<BnplPlan>> = combine(
        expenseRepository.getExpenses(),
        expenseRepository.getBnplChargeLinks()
    ) { expenses, links -> BnplPlans.build(expenses, links) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The charge whose "Link to plans" sheet is open, if any. */
    private val _linkingBnplCharge = MutableStateFlow<com.expenseanalyst.domain.model.Expense?>(null)
    val linkingBnplCharge: StateFlow<com.expenseanalyst.domain.model.Expense?> = _linkingBnplCharge.asStateFlow()

    fun startBnplLink(charge: com.expenseanalyst.domain.model.Expense) { _linkingBnplCharge.value = charge }
    fun dismissBnplLink() { _linkingBnplCharge.value = null }

    /** Each ticked plan gets one instalment paid by this charge; the Review question is answered. */
    fun linkBnplCharge(chargeId: Long, purchaseIds: List<Long>) {
        _linkingBnplCharge.value = null
        if (purchaseIds.isEmpty()) return
        viewModelScope.launch { expenseRepository.linkBnplChargeToPlans(chargeId, purchaseIds) }
    }

    /** What the "Set category" sheet shows for the row being categorised. */
    data class CategoryPick(
        val expense: Expense,
        val categories: List<Category>,
        /** Other Review rows from the same merchant that also wait for a category. */
        val sameMerchantCount: Int,
        /** False when the merchant is the bank's stand-in name — no rule is safe to save. */
        val canRemember: Boolean
    )

    private val _categoryPick = MutableStateFlow<CategoryPick?>(null)
    val categoryPick: StateFlow<CategoryPick?> = _categoryPick.asStateFlow()

    fun startCategoryPick(expense: Expense) {
        viewModelScope.launch {
            val categories = categoryRepository.getCategories().first()
            val offered = ReviewCategoryPlanner.offeredCategories(expense, categories, expenseRepository.getExpensesSnapshot())
            _categoryPick.value = CategoryPick(
                expense = expense,
                categories = offered,
                sameMerchantCount = ReviewCategoryPlanner.sameMerchantRows(expense, expenses.value).size,
                canRemember = ReviewCategoryPlanner.canRememberMerchant(expense)
            )
        }
    }

    fun dismissCategoryPick() { _categoryPick.value = null }

    /**
     * Sets [category] on the row (and, when asked, on the other Review rows from the same merchant)
     * and — owner's choice — remembers the merchant as a rule so future expenses arrive already
     * categorised. The rule keeps its existing tags (saveRule's tagIds = null).
     */
    fun applyCategory(category: Category, remember: Boolean, alsoSameMerchant: Boolean) {
        val pick = _categoryPick.value ?: return
        _categoryPick.value = null
        viewModelScope.launch {
            val rows = buildList {
                add(pick.expense)
                if (alsoSameMerchant) addAll(ReviewCategoryPlanner.sameMerchantRows(pick.expense, expenses.value))
            }
            rows.forEach { expenseRepository.setCategoryFromReview(it.id, category.id) }
            if (remember) {
                ReviewCategoryPlanner.merchantPattern(pick.expense)?.let { pattern ->
                    merchantRuleRepository.saveRule(pattern, category.id, category.name)
                }
            }
        }
    }

    fun markReviewed(expenseId: Long) {
        viewModelScope.launch {
            val expense = expenseRepository.getExpenseById(expenseId).first() ?: return@launch
            expenseRepository.updateExpense(expense.copy(needsReview = false))
        }
    }

    /**
     * Bulk path for clearing the unclassified-transfer backlog without opening each row. Always
     * remembers the recipient: this is where repeat recipients pay off most, and the per-row
     * override on the detail screen remains available if one needs to differ.
     */
    fun classifyTransfer(expenseId: Long, classification: TransferClassification) {
        viewModelScope.launch {
            expenseRepository.classifyTransfer(expenseId, classification)
            val expense = expenseRepository.getExpenseById(expenseId).first()
            expense?.merchantName?.takeIf { it.isNotBlank() }?.let { name ->
                transferRecipientRuleRepository.saveRule(name, classification)
            }
        }
    }

    /**
     * "Reimbursement?" → Link: links this payment to the expenses ReimbursementMatcher suggests
     * right now (the suggestion is recomputed, since pending items may have changed since
     * capture). Nothing to link any more → the flag is simply dropped.
     */
    fun linkSuggestedReimbursement(paymentId: Long) {
        viewModelScope.launch {
            val all = expenseRepository.getExpensesSnapshot()
            val payment = all.find { it.id == paymentId } ?: return@launch
            val match = ReimbursementMatcher.suggest(
                payment.amount, payment.currencyCode, payment.date.toEpochMilliseconds(), all
            )
            if (match.isNullOrEmpty()) {
                expenseRepository.dismissReimbursementSuggestion(paymentId)
            } else {
                expenseRepository.linkReimbursement(paymentId, match.map { it.id })
            }
        }
    }

    fun dismissReimbursementSuggestion(paymentId: Long) {
        viewModelScope.launch { expenseRepository.dismissReimbursementSuggestion(paymentId) }
    }

    fun markAllReviewed() {
        viewModelScope.launch {
            expenseRepository.clearAllNeedsReview()
        }
    }
}
