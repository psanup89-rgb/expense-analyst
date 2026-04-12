package com.expenseanalyst.feature.expenses.ui

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Expense

data class BillWithPayments(
    val bill: Bill,
    val payments: List<Expense>,
    val totalPaid: Double
)

data class BillsUiState(
    val pendingBills: List<BillWithPayments> = emptyList(),
    val settledBills: List<BillWithPayments> = emptyList(),
    val isLoading: Boolean = true,
    val showAddBillSheet: Boolean = false,
    val newBillerName: String = "",
    val newReference: String = "",
    val newTotalDue: String = "",
    val newCurrencyCode: String = "SAR",
    val newMinimumDue: String = "",
    val newDueDateMillis: Long? = null,
    val newStatus: BillStatus = BillStatus.PENDING,
    val isSavingBill: Boolean = false
)
