package com.expenseanalyst.feature.loans.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.LentRepository
import com.expenseanalyst.domain.util.CurrencyConversion
import com.expenseanalyst.feature.loans.service.LentReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddLoanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val lentRepository: LentRepository,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val loanId: Long? = savedStateHandle.get<Long>("loanId")?.takeIf { it != -1L }

    /** The loan as stored, so an edit keeps the fields this form doesn't show. */
    private var existing: LentItem? = null

    private val _uiState = MutableStateFlow(AddLoanUiState(loanId = loanId))
    val uiState: StateFlow<AddLoanUiState> = _uiState.asStateFlow()

    init {
        if (loanId != null) loadExisting(loanId)
    }

    private fun loadExisting(id: Long) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            lentRepository.getLentItemById(id)?.let { item ->
                existing = item
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
            val stored = existing
            val homeAmount = homeAmountOf(amount, state.currencyCode)
                ?: stored?.homeAmount?.takeIf { stored.amount == amount && stored.currencyCode == state.currencyCode }
            // Editing used to rebuild the loan from the form alone, which wiped its home amount and
            // the transaction it was started from, and reopened a settled loan.
            val item = stored?.copy(
                personName = state.personName.trim(),
                amount = amount,
                currencyCode = state.currencyCode,
                homeAmount = homeAmount,
                description = state.description.trim(),
                lentDateMillis = state.lentDateMillis,
                reminderDatetimeMillis = state.reminderDatetimeMillis
            ) ?: LentItem(
                id = loanId ?: 0L,
                personName = state.personName.trim(),
                amount = amount,
                currencyCode = state.currencyCode,
                homeAmount = homeAmount,
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
            val reminderAt = state.reminderDatetimeMillis
            if (reminderAt != null) {
                LentReminderScheduler.schedule(context, savedId, reminderAt)
            } else if (stored?.reminderDatetimeMillis != null) {
                LentReminderScheduler.cancel(context, savedId)
            }
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }

    private suspend fun homeAmountOf(amount: Double, currencyCode: String): Double? {
        val home = currencyRepository.getHomeCurrency().first()
        val rates = currencyRepository.getRates().first().associateBy { it.currencyCode }
        return CurrencyConversion.convert(amount, currencyCode, home, rates)
    }
}
