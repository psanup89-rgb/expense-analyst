package com.expenseanalyst.domain.model

data class LentItem(
    val id: Long = 0,
    val personName: String,
    val amount: Double,
    val currencyCode: String,
    val homeAmount: Double? = null,
    val description: String,
    val lentDateMillis: Long,
    val status: LentStatus = LentStatus.PENDING,
    val settledAmount: Double? = null,
    val settledDateMillis: Long? = null,
    val linkedExpenseId: Long? = null,
    val settlementExpenseId: Long? = null,
    val reminderDatetimeMillis: Long? = null,
    val isDeleted: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

enum class LentStatus { PENDING, SETTLED }
