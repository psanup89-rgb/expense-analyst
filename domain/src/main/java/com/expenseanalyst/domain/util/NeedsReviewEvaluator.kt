package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification

enum class ReviewReason(val label: String) {
    MISSING_MERCHANT("Merchant"),
    GENERIC_CATEGORY("Category"),
    UNKNOWN_PAYMENT_METHOD("Payment method"),
    UNRESOLVED_ACCOUNT("Account"),

    /**
     * A TRANSFER whose [com.expenseanalyst.domain.model.TransferClassification] is still null, so
     * it counts toward neither Spent nor Received. Unlike the other reasons this one can be
     * resolved after capture, which is why ExpenseDao.classifyTransfer recomputes the persisted
     * reason list on that explicit user mutation.
     */
    UNCLASSIFIED_TRANSFER("Transfer type")
}

object NeedsReviewEvaluator {

    private val GENERIC_CATEGORY_NAMES = setOf("Other", "Misc")

    fun evaluate(
        merchantName: String?,
        categoryName: String,
        paymentMethod: PaymentMethod,
        accountLastFour: String?,
        transactionType: TransactionType = TransactionType.EXPENSE,
        transferClassification: TransferClassification? = null
    ): List<ReviewReason> = buildList {
        if (merchantName.isNullOrBlank()) add(ReviewReason.MISSING_MERCHANT)
        if (categoryName in GENERIC_CATEGORY_NAMES) add(ReviewReason.GENERIC_CATEGORY)
        if (paymentMethod == PaymentMethod.OTHER) add(ReviewReason.UNKNOWN_PAYMENT_METHOD)
        if (accountLastFour == null) add(ReviewReason.UNRESOLVED_ACCOUNT)
        if (transactionType == TransactionType.TRANSFER && transferClassification == null) {
            add(ReviewReason.UNCLASSIFIED_TRANSFER)
        }
    }

    /**
     * The persisted reason list with [reason] removed. Used when a reason is resolved *after*
     * capture — currently only [ReviewReason.UNCLASSIFIED_TRANSFER], which unlike the others can
     * be fixed by the user later. Lives here rather than inline in ExpenseDao so the
     * "does the row stay flagged?" decision is unit-testable without a Room harness.
     */
    fun remove(raw: String?, reason: ReviewReason): List<ReviewReason> =
        decode(raw).filterNot { it == reason }

    fun encode(reasons: List<ReviewReason>): String = reasons.joinToString(",") { it.name }

    fun decode(raw: String?): List<ReviewReason> =
        raw?.takeIf { it.isNotBlank() }?.split(",")
            ?.mapNotNull { runCatching { ReviewReason.valueOf(it) }.getOrNull() }
            ?: emptyList()
}
