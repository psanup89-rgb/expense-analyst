package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
}
