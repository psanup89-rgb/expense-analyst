package com.expenseanalyst.feature.emi.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.EmiRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.usecase.GetEmiGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmiDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEmiGroupsUseCase: GetEmiGroupsUseCase,
    private val expenseRepository: ExpenseRepository,
    private val emiRepository: EmiRepository
) : ViewModel() {

    private val emiGroupId: Long = checkNotNull(savedStateHandle["emiGroupId"])
    private val _ui = MutableStateFlow(EmiDetailUiState())

    val uiState = combine(
        getEmiGroupsUseCase.byId(emiGroupId),
        expenseRepository.getExpensesByEmiGroup(emiGroupId),
        _ui
    ) { group, installments, ui ->
        ui.copy(
            group = group,
            installments = installments.sortedBy { it.emiInstallmentNumber },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EmiDetailUiState(isLoading = true)
    )

    fun showCancelConfirm() = _ui.update { it.copy(showCancelConfirm = true) }
    fun dismissCancelConfirm() = _ui.update { it.copy(showCancelConfirm = false) }

    fun cancelRemainingInstallments() {
        viewModelScope.launch {
            _ui.update { it.copy(isCancelling = true, showCancelConfirm = false) }
            emiRepository.cancelRemainingInstallments(emiGroupId)
            _ui.update { it.copy(isCancelling = false, isDone = true) }
        }
    }
}
