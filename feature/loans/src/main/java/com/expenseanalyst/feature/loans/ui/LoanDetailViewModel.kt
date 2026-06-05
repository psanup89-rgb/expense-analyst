package com.expenseanalyst.feature.loans.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.LentStatus
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.LentRepository
import com.expenseanalyst.feature.loans.service.LentReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val lentRepository: LentRepository,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val loanId: Long = checkNotNull(savedStateHandle["loanId"])

    private val _uiState = MutableStateFlow(LoanDetailUiState())
    val uiState: StateFlow<LoanDetailUiState> = _uiState.asStateFlow()

    init {
        load()
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
            val refundCategory = categoryRepository.getCategories()
                .firstOrNull()
                ?.find { it.name == "Refund" }

            val settlementExpenseId = refundCategory?.let { category ->
                val settlement = Expense(
                    amount = item.amount,
                    currencyCode = item.currencyCode,
                    homeAmount = item.homeAmount,
                    exchangeRate = null,
                    description = "Repayment from ${item.personName}",
                    category = category,
                    paymentMethod = PaymentMethod.CASH,
                    transactionType = TransactionType.INCOME,
                    date = Clock.System.now(),
                    merchantName = item.personName,
                    sourceType = SourceType.MANUAL
                )
                expenseRepository.addExpense(settlement)
            }

            val settled = item.copy(
                status = LentStatus.SETTLED,
                settledDateMillis = System.currentTimeMillis(),
                settledAmount = item.amount,
                settlementExpenseId = settlementExpenseId,
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
