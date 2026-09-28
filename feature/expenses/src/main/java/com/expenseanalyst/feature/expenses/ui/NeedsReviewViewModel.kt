package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.TransferRecipientRuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
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
