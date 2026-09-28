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
    val pendingType: String = "TRANSACTION",  // "TRANSACTION" | "BILL" | "BILL_REMINDER"
    val billerName: String? = null,
    val dueDateMillis: Long? = null,
    val linkedBillId: Long? = null,
    /**
     * BILL: reminders that arrived while the statement was still waiting here (carried onto the
     * bill when saved). BILL_REMINDER: how many reminders this one card stands for (≥ 1).
     */
    val reminderCount: Int = 0
)
