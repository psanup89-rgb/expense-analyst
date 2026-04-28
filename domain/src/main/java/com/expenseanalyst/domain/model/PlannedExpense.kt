package com.expenseanalyst.domain.model

data class PlannedExpense(
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val currencyCode: String,
    val categoryId: Long,
    val month: Int,
    val year: Int,
    val isDeleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
