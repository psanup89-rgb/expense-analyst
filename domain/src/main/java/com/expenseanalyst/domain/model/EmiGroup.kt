package com.expenseanalyst.domain.model

import kotlinx.datetime.Instant

data class EmiGroup(
    val id: Long = 0,
    val totalAmount: Double,
    val currencyCode: String,
    val numberOfInstallments: Int,
    val installmentAmount: Double,
    val interestRate: Double?,
    val startDate: Instant,
    val description: String,
    val category: Category,
    val paymentMethod: PaymentMethod,
    val paidCount: Int = 0
)
