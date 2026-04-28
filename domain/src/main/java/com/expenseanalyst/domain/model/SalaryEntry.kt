package com.expenseanalyst.domain.model

data class SalaryEntry(
    val id: Long = 0,
    val amount: Double,
    val currencyCode: String,
    val month: Int,
    val year: Int,
    val sourceExpenseId: Long? = null,
    val isConfirmed: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)
