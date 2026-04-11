package com.expenseanalyst.domain.model

data class Bill(
    val id: Long = 0,
    val billerName: String,
    val accountId: Long? = null,
    val totalDue: Double? = null,
    val minimumDue: Double? = null,
    val currencyCode: String,
    val dueDateMillis: Long? = null,
    val statementPeriodStart: Long? = null,
    val statementPeriodEnd: Long? = null,
    val status: BillStatus,
    val sourceType: SourceType,
    val createdAtMillis: Long,
    val isDeleted: Boolean = false,
    val reference: String? = null
)
