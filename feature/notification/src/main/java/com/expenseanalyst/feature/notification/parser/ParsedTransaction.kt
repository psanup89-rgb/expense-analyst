package com.expenseanalyst.feature.notification.parser

data class ParsedTransaction(
    val amount: Double,
    val currencyCode: String,
    val type: TransactionDirection,
    val merchant: String?,
    val accountLast4: String?,
    val referenceNumber: String?,
    val bankName: String
)

enum class TransactionDirection { DEBIT, CREDIT, PAYMENT }
