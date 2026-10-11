package com.expenseanalyst.feature.emi.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.util.BnplPlan
import com.expenseanalyst.domain.util.BnplPlans
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BnplPlanDetailUiState(
    val plan: BnplPlan? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class BnplPlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val purchaseId: Long = checkNotNull(savedStateHandle["purchaseId"])

    val uiState: StateFlow<BnplPlanDetailUiState> = combine(
        expenseRepository.getExpenses(),
        expenseRepository.getBnplChargeLinks()
    ) { expenses, links ->
        BnplPlanDetailUiState(
            plan = BnplPlans.build(expenses, links).find { it.purchase.id == purchaseId },
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BnplPlanDetailUiState())

    /**
     * Takes this plan off a charge linked by hand. The charge keeps its other plans; with none
     * left it goes back to Review to be linked again (ExpenseDao.linkBnplChargeToPlans).
     */
    fun unlink(chargeId: Long) {
        viewModelScope.launch {
            val rest = expenseRepository.getBnplChargeLinksSnapshot()
                .filter { it.chargeId == chargeId && it.purchaseId != purchaseId }
                .map { it.purchaseId }
            expenseRepository.linkBnplChargeToPlans(chargeId, rest)
        }
    }
}
