package com.expenseanalyst.feature.loans.ui

data class AddLoanUiState(
    val loanId: Long? = null,
    val personName: String = "",
    val amountInput: String = "",
    val currencyCode: String = "SAR",
    val description: String = "",
    val lentDateMillis: Long = System.currentTimeMillis(),
    val reminderDatetimeMillis: Long? = null,
    val showDatePicker: Boolean = false,
    val showReminderPicker: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val isLoading: Boolean = false
) {
    val isValid: Boolean
        get() = personName.isNotBlank() && amountInput.toDoubleOrNull()?.let { it > 0 } == true
}
