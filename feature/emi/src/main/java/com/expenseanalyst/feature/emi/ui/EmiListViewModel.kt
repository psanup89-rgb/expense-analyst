package com.expenseanalyst.feature.emi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.usecase.GetEmiGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class EmiListViewModel @Inject constructor(
    private val getEmiGroupsUseCase: GetEmiGroupsUseCase
) : ViewModel() {

    private val _showCompleted = MutableStateFlow(false)

    val uiState = combine(
        getEmiGroupsUseCase.active(),
        getEmiGroupsUseCase.completed(),
        _showCompleted
    ) { active, completed, showCompleted ->
        EmiListUiState(
            activeGroups = active,
            completedGroups = completed,
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
