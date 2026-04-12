package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val expenseRepository: ExpenseRepository,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _form = MutableStateFlow(BillsUiState())

    init {
        viewModelScope.launch {
            val homeCurrency = currencyRepository.getHomeCurrency().first()
            _form.update { it.copy(newCurrencyCode = homeCurrency) }
        }
    }

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
    fun dismissAddBillSheet() {
        viewModelScope.launch {
            val homeCurrency = currencyRepository.getHomeCurrency().first()
            _form.update {
                it.copy(
                    showAddBillSheet = false,
                    newBillerName = "",
                    newReference = "",
                    newTotalDue = "",
                    newCurrencyCode = homeCurrency,
                    newMinimumDue = "",
                    newDueDateMillis = null,
                    newStatus = BillStatus.PENDING,
                    isSavingBill = false
                )
            }
        }
    }
    fun onBillerNameChange(value: String) = _form.update { it.copy(newBillerName = value) }
    fun onReferenceChange(value: String) = _form.update { it.copy(newReference = value) }
    fun onTotalDueChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _form.update { it.copy(newTotalDue = filtered) }
    }
    fun onCurrencyChange(code: String) = _form.update { it.copy(newCurrencyCode = code) }
    fun onMinimumDueChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
        _form.update { it.copy(newMinimumDue = filtered) }
    }
    fun onDueDateChange(millis: Long?) = _form.update { it.copy(newDueDateMillis = millis) }
    fun onStatusChange(status: BillStatus) = _form.update { it.copy(newStatus = status) }

    fun saveNewBill() {
        val state = _form.value
        if (state.newBillerName.isBlank()) return
        viewModelScope.launch {
            _form.update { it.copy(isSavingBill = true) }
            try {
                val homeCurrency = currencyRepository.getHomeCurrency().first()
                billRepository.saveBill(
                    Bill(
                        billerName = state.newBillerName.trim(),
                        reference = state.newReference.trim().ifEmpty { null },
                        totalDue = state.newTotalDue.toDoubleOrNull(),
                        currencyCode = homeCurrency,
                        minimumDue = state.newMinimumDue.toDoubleOrNull(),
                        dueDateMillis = state.newDueDateMillis,
                        status = state.newStatus,
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
