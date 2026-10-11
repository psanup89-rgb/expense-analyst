package com.expenseanalyst.feature.emi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.usecase.GetEmiGroupsUseCase
import com.expenseanalyst.domain.util.BnplPlans
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class EmiListViewModel @Inject constructor(
    private val getEmiGroupsUseCase: GetEmiGroupsUseCase,
    expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _showCompleted = MutableStateFlow(false)

    private val bnplPlans = combine(
        expenseRepository.getExpenses(),
        expenseRepository.getBnplChargeLinks()
    ) { expenses, links -> BnplPlans.build(expenses, links) }

    val uiState = combine(
        getEmiGroupsUseCase.active(),
        getEmiGroupsUseCase.completed(),
        bnplPlans,
        _showCompleted
    ) { active, completed, plans, showCompleted ->
        EmiListUiState(
            activeGroups = active,
            completedGroups = completed,
            activeBnpl = plans.filterNot { it.isComplete },
            completedBnpl = plans.filter { it.isComplete },
            isLoading = false,
            showCompleted = showCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EmiListUiState(isLoading = true)
    )

    fun toggleShowCompleted() = _showCompleted.update { !it }
}
