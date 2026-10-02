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
    UNCLASSIFIED_TRANSFER("Transfer type"),

    /**
     * An incoming payment that matches pending reimbursable expenses (ReimbursementMatcher).
     * The owner chose to be asked, not auto-linked; resolved after capture by linking it or by
     * dismissing the suggestion (ExpenseDao.linkReimbursement / dropReviewReason).
     */
    POSSIBLE_REIMBURSEMENT("Reimbursement?")
}

object NeedsReviewEvaluator {

    private val GENERIC_CATEGORY_NAMES = setOf("Other", "Misc")

    fun evaluate(
        merchantName: String?,
        categoryName: String,
        paymentMethod: PaymentMethod,
        accountLastFour: String?,
        transactionType: TransactionType = TransactionType.EXPENSE,
        transferClassification: TransferClassification? = null,
        /**
         * Whether the account is known even without digits in the message — true when the bank
         * has exactly one account in the app (STC: its messages never carry the user's own
         * account digits, so every STC row was flagged "Account" although it was filed right).
         */
        accountIdentified: Boolean = accountLastFour != null
    ): List<ReviewReason> = buildList {
        if (merchantName.isNullOrBlank()) add(ReviewReason.MISSING_MERCHANT)
        // A PAYMENT (card bill, BNPL purchase) counts toward no total, so its category changes
        // nothing — flagging it put every card payment in Review as "Misc".
        if (categoryName in GENERIC_CATEGORY_NAMES && transactionType != TransactionType.PAYMENT) {
            add(ReviewReason.GENERIC_CATEGORY)
        }
        if (paymentMethod == PaymentMethod.OTHER) add(ReviewReason.UNKNOWN_PAYMENT_METHOD)
        if (!accountIdentified) add(ReviewReason.UNRESOLVED_ACCOUNT)
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

    /**
     * Reasons a payment no longer needs once the user has linked it to a bill: the bill says what
     * the money was for, so a generic category is moot, and the method is settled by the link
     * (ExpenseDao.linkBillPayment fills it in from the account). A missing merchant or an
     * unresolved account is not answered by the link and stays.
     */
    val RESOLVED_BY_BILL_LINK: Set<ReviewReason> =
        setOf(ReviewReason.GENERIC_CATEGORY, ReviewReason.UNKNOWN_PAYMENT_METHOD)

    fun removeAll(raw: String?, reasons: Set<ReviewReason>): List<ReviewReason> =
        decode(raw).filterNot { it in reasons }

    fun encode(reasons: List<ReviewReason>): String = reasons.joinToString(",") { it.name }

    fun decode(raw: String?): List<ReviewReason> =
        raw?.takeIf { it.isNotBlank() }?.split(",")
            ?.mapNotNull { runCatching { ReviewReason.valueOf(it) }.getOrNull() }
            ?: emptyList()
}
