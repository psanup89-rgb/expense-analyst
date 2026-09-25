package com.expenseanalyst.feature.loans.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.LentRepository
import com.expenseanalyst.domain.util.SpendClassifier
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
class LoanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val lentRepository: LentRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val loanId: Long = checkNotNull(savedStateHandle["loanId"])

    private val _uiState = MutableStateFlow(LoanDetailUiState())
    val uiState: StateFlow<LoanDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            expenseRepository.getExpensesByLoan(loanId).collect { legs ->
                _uiState.update { it.copy(legs = legs) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            val item = lentRepository.getLentItemById(loanId)
            _uiState.update { it.copy(item = item, isLoading = false) }
        }
    }

    fun showSettleDialog() = _uiState.update { it.copy(showSettleDialog = true) }
    fun hideSettleDialog() = _uiState.update { it.copy(showSettleDialog = false) }
    fun showDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = true) }
    fun hideDeleteDialog() = _uiState.update { it.copy(showDeleteDialog = false) }
    fun showReminderPicker() = _uiState.update { it.copy(showReminderPicker = true) }
    fun hideReminderPicker() = _uiState.update { it.copy(showReminderPicker = false) }

    fun markSettled() {
        val item = _uiState.value.item ?: return
        _uiState.update { it.copy(isSaving = true, showSettleDialog = false) }
        viewModelScope.launch {
            // Settling used to fabricate an INCOME row in the Refund category for the full amount.
            // That was wrong twice over: it subtracted money from Spent that was never in Spent
            // (Refund income nets against spending), and it silently did nothing when no category
            // named "Refund" existed. Repayments are now real transactions the user links to the
            // loan from their own detail screen, and linked legs stay out of both totals. This
            // just records the loan as closed, using what was actually repaid when it is known.
            val repaid = uiState.value.legs
                .filter { SpendClassifier.isLoanRepayment(it) }
                .sumOf { SpendClassifier.homeValue(it) }
            val settled = item.copy(
                status = LentStatus.SETTLED,
                settledDateMillis = System.currentTimeMillis(),
                settledAmount = if (repaid > 0.0) repaid else item.amount,
                reminderDatetimeMillis = null
            )
            lentRepository.updateLentItem(settled)
            LentReminderScheduler.cancel(context, loanId)
            _uiState.update { it.copy(item = settled, isSaving = false) }
        }
    }

    fun setReminder(reminderAtMillis: Long) {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            val updated = item.copy(reminderDatetimeMillis = reminderAtMillis)
            lentRepository.updateLentItem(updated)
            LentReminderScheduler.schedule(context, loanId, reminderAtMillis)
            _uiState.update { it.copy(item = updated, showReminderPicker = false) }
        }
    }

    fun clearReminder() {
        val item = _uiState.value.item ?: return
        viewModelScope.launch {
            val updated = item.copy(reminderDatetimeMillis = null)
            lentRepository.updateLentItem(updated)
            LentReminderScheduler.cancel(context, loanId)
            _uiState.update { it.copy(item = updated) }
        }
    }

    fun delete() {
        _uiState.update { it.copy(isSaving = true, showDeleteDialog = false) }
        viewModelScope.launch {
            LentReminderScheduler.cancel(context, loanId)
            lentRepository.softDeleteLentItem(loanId)
            _uiState.update { it.copy(navigateBack = true) }
        }
    }
}
