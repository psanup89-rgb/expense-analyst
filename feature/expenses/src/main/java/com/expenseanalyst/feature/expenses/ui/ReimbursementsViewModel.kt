package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.util.ReimbursementMatcher
import com.expenseanalyst.domain.util.SpendClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import kotlin.math.abs

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

    private val allExpenses: StateFlow<List<Expense>> = expenseRepository.getExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Payments that repaid something, by id — for "Paid back by …" on reimbursed items. */
    val paybacks: StateFlow<Map<Long, Expense>> = allExpenses
        .map { list -> list.filter(SpendClassifier::isReimbursementPayback).associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _picking = MutableStateFlow<Expense?>(null)
    /** The expense whose repaying payment the user is choosing; null when no picker is open. */
    val picking: StateFlow<Expense?> = _picking.asStateFlow()

    /**
     * Incoming payments that could have repaid [picking]: income from a day before the expense
     * to [ReimbursementMatcher.WINDOW_DAYS] after, not loan repayments, closest amount first.
     * A payment already repaying other expenses stays eligible — one payment can repay a claim.
     */
    val paymentCandidates: StateFlow<List<Expense>> = combine(_picking, allExpenses) { expense, all ->
        if (expense == null) return@combine emptyList()
        val from = expense.date.toEpochMilliseconds() - DAY_MS
        val to = expense.date.toEpochMilliseconds() + ReimbursementMatcher.WINDOW_DAYS * DAY_MS
        val target = SpendClassifier.homeValue(expense)
        all.filter {
            it.transactionType == TransactionType.INCOME && !it.isDeleted && !SpendClassifier.isLoanLeg(it) &&
                it.date.toEpochMilliseconds() in from..to
        }
            .sortedWith(compareBy<Expense> { abs(SpendClassifier.homeValue(it) - target) }.thenByDescending { it.date })
            .take(MAX_CANDIDATES)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startMarkReimbursed(expense: Expense) { _picking.value = expense }

    fun cancelPicking() { _picking.value = null }

    /** Links the expense being picked to the payment that repaid it. */
    fun linkTo(paybackId: Long) {
        val expense = _picking.value ?: return
        _picking.value = null
        viewModelScope.launch { expenseRepository.linkReimbursement(paybackId, listOf(expense.id)) }
    }

    /** Repaid in cash or somewhere the app can't see: just record the date, as before. */
    fun markPaidOutsideApp() {
        val expense = _picking.value ?: return
        _picking.value = null
        viewModelScope.launch { expenseRepository.markReimbursed(expense.id, Clock.System.now()) }
    }

    /** Moves an item back to Pending — unlinking it from its payment if it had one. */
    fun undoReimbursed(expense: Expense) {
        viewModelScope.launch {
            if (expense.reimbursedById != null) {
                expenseRepository.unlinkReimbursement(expense.id)
            } else {
                expenseRepository.markReimbursed(expense.id, null)
            }
        }
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val MAX_CANDIDATES = 30
    }
}
