package com.expenseanalyst.domain.model

data class PendingNotification(
    val id: Long = 0,
    val amount: Double,
    val currencyCode: String,
    val merchantName: String?,
    val bankName: String,
    val accountLast4: String?,
    val transactionType: String, // mirrors TransactionDirection.name: DEBIT | CREDIT | PAYMENT
    val detectedAtMillis: Long,
    val rawBody: String? = null,
    val paymentMethod: String? = null,  // PaymentMethod enum name, e.g. "APPLE_PAY"
    val isPossibleDuplicate: Boolean = false,
    val pendingType: String = "TRANSACTION",  // "TRANSACTION" | "BILL"
    val billerName: String? = null,
    val dueDateMillis: Long? = null,
    val linkedBillId: Long? = null
)
