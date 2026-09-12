package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class ReimbursementsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    currencyRepository: CurrencyRepository
) : ViewModel() {

    private val reimbursableExpenses: StateFlow<List<Expense>> =
        expenseRepository.getReimbursableExpenses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ordered by expense date, most recent first — same order the underlying query returns. */
    val pending: StateFlow<List<Expense>> = reimbursableExpenses
        .map { list -> list.filter { it.reimbursedDate == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Ordered by reimbursement date, most recently reimbursed first. */
    val reimbursed: StateFlow<List<Expense>> = reimbursableExpenses
        .map { list ->
            list.filter { it.reimbursedDate != null }
                .sortedByDescending { it.reimbursedDate }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val homeCurrencyCode: StateFlow<String> = currencyRepository.getHomeCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SAR")

    fun markReimbursed(expenseId: Long) {
        viewModelScope.launch {
            expenseRepository.markReimbursed(expenseId, Clock.System.now())
        }
    }

    /** Moves an item back to Pending — for correcting an accidental tap. */
    fun undoReimbursed(expenseId: Long) {
        viewModelScope.launch {
            expenseRepository.markReimbursed(expenseId, null)
        }
    }
}
