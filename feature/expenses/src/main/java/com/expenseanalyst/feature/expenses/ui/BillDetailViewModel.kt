package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillDetailViewModel @Inject constructor(
    private val billRepository: BillRepository,
    private val expenseRepository: ExpenseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val billId: Long = checkNotNull(savedStateHandle["billId"])

    val uiState = combine(
        billRepository.getBillById(billId),
        expenseRepository.getExpensesByBillId(billId)
    ) { bill, payments ->
        val totalPaid = payments.sumOf { it.homeAmount ?: it.amount }
        BillDetailUiState(
            bill = bill,
            payments = payments,
            totalPaid = totalPaid,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillDetailUiState()
    )

    fun deleteBill(onDeleted: () -> Unit) {
        viewModelScope.launch {
            billRepository.softDeleteBill(billId)
            onDeleted()
        }
    }

    fun unlinkPayment(expenseId: Long) {
        viewModelScope.launch {
            val expense = expenseRepository.getExpenseById(expenseId).first() ?: return@launch
            expenseRepository.updateExpense(expense.copy(billId = null))
            // Recalculate bill status based on remaining linked payments
            val remaining = expenseRepository.getExpensesByBillId(billId).first()
                .filter { it.id != expenseId }
            val newStatus = if (remaining.isEmpty()) BillStatus.PENDING else BillStatus.PARTIAL
            val bill = billRepository.getBillById(billId).first() ?: return@launch
            if (bill.status != BillStatus.PENDING || remaining.isNotEmpty()) {
                billRepository.updateBill(bill.copy(status = newStatus))
            }
        }
    }
}
