package com.expenseanalyst.feature.loans.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.repository.LentRepository
import com.expenseanalyst.feature.loans.service.LentReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddLoanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val lentRepository: LentRepository
) : ViewModel() {

    private val loanId: Long? = savedStateHandle.get<Long>("loanId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(AddLoanUiState(loanId = loanId))
    val uiState: StateFlow<AddLoanUiState> = _uiState.asStateFlow()

    init {
        if (loanId != null) loadExisting(loanId)
    }

    private fun loadExisting(id: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            lentRepository.getLentItemById(id)?.let { item ->
                _uiState.update {
                    it.copy(
                        personName = item.personName,
                        amountInput = item.amount.toString(),
                        currencyCode = item.currencyCode,
                        description = item.description,
                        lentDateMillis = item.lentDateMillis,
                        reminderDatetimeMillis = item.reminderDatetimeMillis,
                        isLoading = false
                    )
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onPersonNameChange(v: String) = _uiState.update { it.copy(personName = v) }
    fun onAmountChange(v: String) = _uiState.update { it.copy(amountInput = v) }
    fun onCurrencyChange(v: String) = _uiState.update { it.copy(currencyCode = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onDateSelected(millis: Long) = _uiState.update { it.copy(lentDateMillis = millis, showDatePicker = false) }
    fun onReminderSelected(millis: Long?) = _uiState.update { it.copy(reminderDatetimeMillis = millis, showReminderPicker = false) }
    fun showDatePicker() = _uiState.update { it.copy(showDatePicker = true) }
    fun hideDatePicker() = _uiState.update { it.copy(showDatePicker = false) }
    fun showReminderPicker() = _uiState.update { it.copy(showReminderPicker = true) }
    fun hideReminderPicker() = _uiState.update { it.copy(showReminderPicker = false) }

    fun save() {
        val state = _uiState.value
        val amount = state.amountInput.toDoubleOrNull() ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val item = LentItem(
                id = loanId ?: 0L,
                personName = state.personName.trim(),
                amount = amount,
                currencyCode = state.currencyCode,
                description = state.description.trim(),
                lentDateMillis = state.lentDateMillis,
                status = LentStatus.PENDING,
                reminderDatetimeMillis = state.reminderDatetimeMillis
            )
            val savedId = if (loanId != null) {
                lentRepository.updateLentItem(item)
                loanId
            } else {
                lentRepository.addLentItem(item)
            }
            state.reminderDatetimeMillis?.let { reminderAt ->
                LentReminderScheduler.schedule(context, savedId, reminderAt)
            }
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
