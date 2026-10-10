package com.expenseanalyst.feature.loans.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.LentRepository
import com.expenseanalyst.domain.util.LoanBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanListViewModel @Inject constructor(
    private val lentRepository: LentRepository,
    private val expenseRepository: ExpenseRepository,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanListUiState())
    val uiState: StateFlow<LoanListUiState> = _uiState.asStateFlow()

    init {
        observeItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            combine(
                lentRepository.getLentItems(),
                expenseRepository.getLoanLegs(),
                currencyRepository.getRates(),
                currencyRepository.getHomeCurrency()
            ) { items, legs, rates, home ->
                val ratesByCode = rates.associateBy { it.currencyCode }
                val balances = items.associate { it.id to LoanBalance.of(it, legs, ratesByCode) }
                val pending = items.filter { it.status == LentStatus.PENDING }
                val settled = items.filter { it.status == LentStatus.SETTLED }
                val totalHome = pending.sumOf { loan ->
                    balances.getValue(loan.id).remainingInHome(loan, home, ratesByCode)
                }
                _uiState.update {
                    it.copy(
                        pendingItems = pending,
                        settledItems = settled,
                        balances = balances,
                        totalPendingHome = totalHome,
                        homeCurrency = home,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun toggleShowSettled() {
        _uiState.update { it.copy(showSettled = !it.showSettled) }
    }
}
