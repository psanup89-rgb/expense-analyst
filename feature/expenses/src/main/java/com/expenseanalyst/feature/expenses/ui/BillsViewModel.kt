package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BillsViewModel @Inject constructor(
    private val billRepository: BillRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _form = MutableStateFlow(BillsUiState())

    val uiState = combine(
        _form,
        billRepository.getBills().flatMapLatest { bills ->
            if (bills.isEmpty()) {
                flowOf(emptyList<BillWithPayments>())
            } else {
                // Combine each bill with its linked payment expenses
                val paymentFlows = bills.map { bill ->
                    expenseRepository.getExpensesByBillId(bill.id).map { payments ->
                        val totalPaid = payments.sumOf { it.homeAmount ?: it.amount }
                        BillWithPayments(bill, payments, totalPaid)
                    }
                }
                combine(paymentFlows) { it.toList() }
            }
        }
    ) { form, billsWithPayments ->
        val pending = billsWithPayments.filter { it.bill.status != BillStatus.SETTLED }
            .sortedWith(compareBy(nullsLast()) { it.bill.dueDateMillis })
        val settled = billsWithPayments.filter { it.bill.status == BillStatus.SETTLED }
            .sortedByDescending { it.bill.createdAtMillis }
        form.copy(
            pendingBills = pending,
            settledBills = settled,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BillsUiState()
    )

    fun showAddBillSheet() = _form.update { it.copy(showAddBillSheet = true) }
    fun dismissAddBillSheet() = _form.update {
        it.copy(showAddBillSheet = false, newBillerName = "", newTotalDue = "", isSavingBill = false)
    }
    fun onBillerNameChange(value: String) = _form.update { it.copy(newBillerName = value) }
    fun onTotalDueChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _form.update { it.copy(newTotalDue = filtered) }
    }
    fun onCurrencyChange(code: String) = _form.update { it.copy(newCurrencyCode = code) }

    fun saveNewBill() {
        val state = _form.value
        if (state.newBillerName.isBlank()) return
        viewModelScope.launch {
            _form.update { it.copy(isSavingBill = true) }
            try {
                billRepository.saveBill(
                    Bill(
                        billerName = state.newBillerName.trim(),
                        totalDue = state.newTotalDue.toDoubleOrNull(),
                        currencyCode = state.newCurrencyCode,
                        status = BillStatus.PENDING,
                        sourceType = SourceType.MANUAL,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
                dismissAddBillSheet()
            } catch (_: Exception) {
                _form.update { it.copy(isSavingBill = false) }
            }
        }
    }

    fun deleteBill(billId: Long) {
        viewModelScope.launch { billRepository.softDeleteBill(billId) }
    }
}
