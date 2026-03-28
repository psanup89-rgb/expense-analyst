package com.expenseanalyst.domain.model

import kotlinx.datetime.Instant

data class Expense(
    val id: Long = 0,
    val amount: Double,
    val currencyCode: String,
    val homeAmount: Double?,
    val exchangeRate: Double?,
    val description: String,
    val category: Category,
    val paymentMethod: PaymentMethod,
    val transactionType: TransactionType,
    val date: Instant,
    val merchantName: String?,
    val sourceType: SourceType,
    val sourceSender: String? = null,
    val emiGroupId: Long? = null,
    val emiInstallmentNumber: Int? = null,
    val note: String? = null,
    val accountId: Long? = null,
    val accountDisplayName: String? = null,
    val rawSmsBody: String? = null,
    val billId: Long? = null,
    val isDeleted: Boolean = false
)
