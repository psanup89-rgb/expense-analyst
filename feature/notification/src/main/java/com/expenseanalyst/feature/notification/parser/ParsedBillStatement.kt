package com.expenseanalyst.feature.notification.parser

/**
 * Result of parsing a bank bill/statement SMS.
 * Distinct from [ParsedTransaction] — a statement is not a debit/credit event,
 * it's a notification of an outstanding balance.
 */
data class ParsedBillStatement(
    val billerName: String,
    val totalDue: Double?,
    val minimumDue: Double?,
    val currencyCode: String,
    val dueDateMillis: Long?,
    val statementPeriodStart: Long?,
    val statementPeriodEnd: Long?,
    val accountLast4: String?,
    val bankName: String,
    val rawBody: String? = null
)
