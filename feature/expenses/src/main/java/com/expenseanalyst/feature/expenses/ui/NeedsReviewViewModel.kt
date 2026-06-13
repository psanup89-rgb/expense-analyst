package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NeedsReviewViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
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

    fun markAllReviewed() {
        viewModelScope.launch {
            expenseRepository.clearAllNeedsReview()
        }
    }
}
