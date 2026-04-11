package com.expenseanalyst.feature.expenses.ui

import com.expenseanalyst.domain.model.BillStatus

data class EditBillUiState(
    val billerName: String = "",
    val totalDue: String = "",
    val minimumDue: String = "",
    val currencyCode: String = "SAR",
    val dueDateMillis: Long? = null,
    val status: BillStatus = BillStatus.PENDING,
    val reference: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)
