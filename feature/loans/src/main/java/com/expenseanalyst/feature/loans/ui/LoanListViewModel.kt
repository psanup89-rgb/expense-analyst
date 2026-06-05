package com.expenseanalyst.feature.loans.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.repository.LentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanListViewModel @Inject constructor(
    private val lentRepository: LentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanListUiState())
    val uiState: StateFlow<LoanListUiState> = _uiState.asStateFlow()

    init {
        observeItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            lentRepository.getLentItems().collect { items ->
                val pending = items.filter { it.status == LentStatus.PENDING }
                val settled = items.filter { it.status == LentStatus.SETTLED }
                _uiState.update {
                    it.copy(pendingItems = pending, settledItems = settled, isLoading = false)
                }
            }
        }
    }

    fun toggleShowSettled() {
        _uiState.update { it.copy(showSettled = !it.showSettled) }
    }
}
