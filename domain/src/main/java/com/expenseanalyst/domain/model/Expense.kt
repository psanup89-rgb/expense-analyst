package com.expenseanalyst.domain.model

import com.expenseanalyst.domain.util.ReviewReason
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
    val tags: List<Tag> = emptyList(),
    val accountId: Long? = null,
    val accountDisplayName: String? = null,
    val accountLastFour: String? = null,
    val rawSmsBody: String? = null,
    val billId: Long? = null,
    val isDeleted: Boolean = false,
    val needsReview: Boolean = false,
    val reviewReasons: List<ReviewReason> = emptyList(),
    /**
     * Whether this expense is expected to be paid back (e.g. work expense claim). Independent
     * of [TransactionType] and [Category] — see domain/util/NeedsReviewEvaluator.kt for the
     * precedent of a plain status flag living directly on Expense rather than encoded as a
     * category or type. Does NOT net out of any totals: the reimbursement itself arrives as
     * its own separately-detected INCOME transaction.
     */
    val isReimbursable: Boolean = false,
    /** Null while pending; set when the user manually marks this reimbursed. */
    val reimbursedDate: Instant? = null,
    /**
     * Set on a Refund-category INCOME expense that was auto-matched to the original EXPENSE
     * it refunds (same amount + currency, within [domain/util/RefundMatcher.kt]'s window).
     * Points at the original expense's id. Null on every non-refund expense, and on a refund
     * whose original purchase couldn't be found. See RefundMatcher's KDoc for the matching rule.
     */
    val refundOriginalExpenseId: Long? = null,
    /**
     * Only meaningful when [transactionType] is [TransactionType.TRANSFER]. Null means the user
     * hasn't said whether this transfer went to their own account or to someone else, and an
     * unclassified transfer is excluded from every total — see
     * [com.expenseanalyst.domain.util.SpendClassifier].
     */
    val transferClassification: TransferClassification? = null,
    /**
     * Set when this row is one leg of a tracked loan — money lent out, or a repayment coming back.
     * A loan leg counts toward neither Spent nor Received, whatever its [transactionType]: lending
     * isn't spending and repayment isn't income. Many rows can share one loan (a loan may be lent
     * in several transfers and repaid in several), which is why the link lives on the expense row
     * rather than as a single `linkedExpenseId` on the loan. See
     * [com.expenseanalyst.domain.util.SpendClassifier.isLoanLeg].
     */
    val loanId: Long? = null
)
