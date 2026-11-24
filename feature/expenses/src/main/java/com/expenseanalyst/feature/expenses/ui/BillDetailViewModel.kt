package com.expenseanalyst.feature.expenses.ui

import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import com.expenseanalyst.domain.util.BillPaymentPicker
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.util.BillPaymentTwins
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.usecase.LinkBillPaymentUseCase
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
    private val linkBillPayment: LinkBillPaymentUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val billId: Long = checkNotNull(savedStateHandle["billId"])

    val uiState = combine(
        billRepository.getBillById(billId),
        expenseRepository.getExpensesByBillId(billId)
    ) { bill, payments ->
        val totalPaid = BillPaymentTwins.distinct(payments).sumOf { it.homeAmount ?: it.amount }
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

    /** [millis] is the date picker's UTC midnight, the same form statement parsers store. */
    fun setDueDate(millis: Long) {
        viewModelScope.launch {
            val bill = billRepository.getBillById(billId).first() ?: return@launch
            billRepository.updateBill(bill.copy(dueDateMillis = millis))
        }
    }

    /** "Mark as paid": the expenses that could have paid this bill (BillPaymentPicker). */
    private val _candidates = MutableStateFlow<List<Expense>?>(null)
    val candidates: StateFlow<List<Expense>?> = _candidates.asStateFlow()

    /** Set when the picked expense doesn't cover the bill — the owner is asked: close it fully, or partial? */
    private val _shortPayment = MutableStateFlow<Expense?>(null)
    val shortPayment: StateFlow<Expense?> = _shortPayment.asStateFlow()

    fun startMarkAsPaid() {
        viewModelScope.launch {
            val bill = billRepository.getBillById(billId).first() ?: return@launch
            _candidates.value = BillPaymentPicker.candidates(
                bill, expenseRepository.getExpensesSnapshot(), System.currentTimeMillis()
            )
        }
    }

    fun dismissMarkAsPaid() { _candidates.value = null }

    /** An expense is mandatory: the bill is closed by linking the payment that paid it. */
    fun pickPayment(expense: Expense) {
        _candidates.value = null
        viewModelScope.launch {
            if (linkBillPayment.wouldSettle(expense.id, billId)) {
                linkBillPayment.link(expense.id, billId)
            } else {
                _shortPayment.value = expense
            }
        }
    }

    fun resolveShortPayment(closeFully: Boolean) {
        val expense = _shortPayment.value ?: return
        _shortPayment.value = null
        viewModelScope.launch { linkBillPayment.link(expense.id, billId, closeFully = closeFully) }
    }

    fun cancelShortPayment() { _shortPayment.value = null }

    fun unlinkPayment(expenseId: Long) {
        viewModelScope.launch { linkBillPayment.unlink(expenseId) }
    }
}
