package com.expenseanalyst.feature.notification.parser

data class ParsedTransaction(
    val amount: Double,
    val currencyCode: String,
    val type: TransactionDirection,
    val merchant: String?,
    val accountLast4: String?,
    val referenceNumber: String?,
    val bankName: String,
    val rawBody: String? = null,
    /** PaymentMethod enum name (e.g. "APPLE_PAY") when a specific payment method is detected. */
    val paymentMethodName: String? = null
)

enum class TransactionDirection { DEBIT, CREDIT, PAYMENT, TRANSFER }
