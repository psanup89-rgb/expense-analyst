package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification

/**
 * Single source of truth for "does this row count as spending / as money received".
 *
 * This used to be hand-rolled as `transactionType == EXPENSE` independently in
 * ExpenseListViewModel, AnalyticsViewModel and BudgetViewModel. That duplication is exactly how
 * TRANSFER rows became invisible to every total while still rendering in the list as red
 * minus-amounts — the app showed money leaving and counted none of it. Keep all three on these
 * predicates.
 *
 * The rules:
 * - An EXPENSE is spending.
 * - A TRANSFER is spending only when classified [TransferClassification.EXTERNAL] — money that
 *   actually left the user's hands.
 * - A TRANSFER classified [TransferClassification.OWN_ACCOUNT] is a move between the user's own
 *   accounts and never counts.
 * - An *unclassified* TRANSFER is excluded from every total. This deliberately preserves the
 *   behaviour that predated classification, so upgrading moves no historical figure; the user
 *   opts each one in. It is surfaced instead via [ReviewReason.UNCLASSIFIED_TRANSFER] and the
 *   Spent breakdown sheet.
 * - A row linked to a loan ([Expense.loanId]) counts as neither, whatever its type: money lent
 *   out isn't spending and the repayment isn't income. This check comes first, because a loan
 *   leg can be a transfer, an expense or income depending on how the bank reported it.
 * - PAYMENT (card/bill settlement) counts as neither — it settles existing debt.
 * - Refunds are INCOME rows in the "Refund" category and net out of *Spent* rather than
 *   inflating Received (issue #12).
 */
object SpendClassifier {

    const val REFUND_CATEGORY = "Refund"

    fun isLoanLeg(e: Expense): Boolean = e.loanId != null

    /**
     * The inbound leg of a loan. Direction comes from the type/classification the link step
     * stamps on the row (linking a repayment marks a transfer EXTERNAL_IN), since a stored
     * transfer otherwise can't say which way the money moved.
     */
    fun isLoanRepayment(e: Expense): Boolean = isLoanLeg(e) &&
        (e.transactionType == TransactionType.INCOME ||
            e.transferClassification == TransferClassification.EXTERNAL_IN)

    fun isSpend(e: Expense): Boolean = if (isLoanLeg(e)) false else when (e.transactionType) {
        TransactionType.EXPENSE -> true
        TransactionType.TRANSFER -> e.transferClassification == TransferClassification.EXTERNAL
        else -> false
    }

    fun isOwnTransfer(e: Expense): Boolean =
        !isLoanLeg(e) && e.transactionType == TransactionType.TRANSFER &&
            e.transferClassification == TransferClassification.OWN_ACCOUNT

    fun isUnclassifiedTransfer(e: Expense): Boolean =
        !isLoanLeg(e) && e.transactionType == TransactionType.TRANSFER && e.transferClassification == null

    fun isRefund(e: Expense): Boolean =
        !isLoanLeg(e) && e.transactionType == TransactionType.INCOME && e.category.name == REFUND_CATEGORY

    fun isReceived(e: Expense): Boolean = if (isLoanLeg(e)) false else when (e.transactionType) {
        TransactionType.INCOME -> e.category.name != REFUND_CATEGORY
        TransactionType.TRANSFER -> e.transferClassification == TransferClassification.EXTERNAL_IN
        else -> false
    }

    /**
     * The value to aggregate, in the user's home currency.
     *
     * Falls back to the original [Expense.amount] when [Expense.homeAmount] is null. This was
     * previously inconsistent — ExpenseListViewModel used `?: 0.0` (silently dropping the row)
     * while AnalyticsViewModel used `?: amount`. The list screen gets away with it because it
     * pre-resolves conversions before aggregating; the safer fallback is used here so the two
     * agree even if that pre-resolution is ever removed.
     */
    fun homeValue(e: Expense): Double = e.homeAmount ?: e.amount

    fun spendBreakdown(expenses: List<Expense>): SpendBreakdown {
        var expenseTotal = 0.0
        var expenseCount = 0
        var externalTransferTotal = 0.0
        var externalTransferCount = 0
        var refundTotal = 0.0
        var refundCount = 0
        var ownTransferTotal = 0.0
        var ownTransferCount = 0
        var unclassifiedTransferTotal = 0.0
        var unclassifiedTransferCount = 0
        var loanOutTotal = 0.0
        var loanOutCount = 0

        for (e in expenses) {
            val value = homeValue(e)
            if (isLoanLeg(e)) {
                // Reported so the sheet can show what was set aside, but never part of `net`.
                if (!isLoanRepayment(e)) {
                    loanOutTotal += value
                    loanOutCount++
                }
                continue
            }
            when {
                e.transactionType == TransactionType.EXPENSE -> {
                    expenseTotal += value
                    expenseCount++
                }
                isSpend(e) -> {
                    externalTransferTotal += value
                    externalTransferCount++
                }
                isOwnTransfer(e) -> {
                    ownTransferTotal += value
                    ownTransferCount++
                }
                isUnclassifiedTransfer(e) -> {
                    unclassifiedTransferTotal += value
                    unclassifiedTransferCount++
                }
            }
            if (isRefund(e)) {
                refundTotal += value
                refundCount++
            }
        }

        return SpendBreakdown(
            expenseTotal = expenseTotal,
            expenseCount = expenseCount,
            externalTransferTotal = externalTransferTotal,
            externalTransferCount = externalTransferCount,
            refundTotal = refundTotal,
            refundCount = refundCount,
            ownTransferTotal = ownTransferTotal,
            ownTransferCount = ownTransferCount,
            unclassifiedTransferTotal = unclassifiedTransferTotal,
            unclassifiedTransferCount = unclassifiedTransferCount,
            loanOutTotal = loanOutTotal,
            loanOutCount = loanOutCount
        )
    }

    fun receivedBreakdown(expenses: List<Expense>): ReceivedBreakdown {
        var incomeTotal = 0.0
        var incomeCount = 0
        var transferInTotal = 0.0
        var transferInCount = 0
        var refundTotal = 0.0
        var refundCount = 0
        var loanRepaidTotal = 0.0
        var loanRepaidCount = 0

        for (e in expenses) {
            val value = homeValue(e)
            if (isLoanLeg(e)) {
                if (isLoanRepayment(e)) {
                    loanRepaidTotal += value
                    loanRepaidCount++
                }
                continue
            }
            when {
                e.transactionType == TransactionType.INCOME && !isRefund(e) -> {
                    incomeTotal += value
                    incomeCount++
                }
                isReceived(e) -> {
                    transferInTotal += value
                    transferInCount++
                }
            }
            if (isRefund(e)) {
                refundTotal += value
                refundCount++
            }
        }

        return ReceivedBreakdown(
            incomeTotal = incomeTotal,
            incomeCount = incomeCount,
            transferInTotal = transferInTotal,
            transferInCount = transferInCount,
            refundTotal = refundTotal,
            refundCount = refundCount,
            loanRepaidTotal = loanRepaidTotal,
            loanRepaidCount = loanRepaidCount
        )
    }
}

/**
 * The components of the Spent figure, each with its own count so the breakdown sheet can show
 * "Expenses (23)" without recomputing anything. [ownTransferTotal] and
 * [unclassifiedTransferTotal] are reported but deliberately excluded from [net] — they exist so
 * the user can see what is *not* counted, which is the whole point of the breakdown.
 */
data class SpendBreakdown(
    val expenseTotal: Double = 0.0,
    val expenseCount: Int = 0,
    val externalTransferTotal: Double = 0.0,
    val externalTransferCount: Int = 0,
    val refundTotal: Double = 0.0,
    val refundCount: Int = 0,
    val ownTransferTotal: Double = 0.0,
    val ownTransferCount: Int = 0,
    val unclassifiedTransferTotal: Double = 0.0,
    val unclassifiedTransferCount: Int = 0,
    val loanOutTotal: Double = 0.0,
    val loanOutCount: Int = 0
) {
    val net: Double =
        (expenseTotal + externalTransferTotal - refundTotal).coerceAtLeast(0.0)
}

/**
 * The components of the Received figure. [refundTotal] is reported but excluded from [net]:
 * refunds reduce Spent instead of inflating Received, and surfacing that in the breakdown is
 * what finally makes the behaviour visible to the user.
 */
data class ReceivedBreakdown(
    val incomeTotal: Double = 0.0,
    val incomeCount: Int = 0,
    val transferInTotal: Double = 0.0,
    val transferInCount: Int = 0,
    val refundTotal: Double = 0.0,
    val refundCount: Int = 0,
    val loanRepaidTotal: Double = 0.0,
    val loanRepaidCount: Int = 0
) {
    val net: Double = incomeTotal + transferInTotal
}
