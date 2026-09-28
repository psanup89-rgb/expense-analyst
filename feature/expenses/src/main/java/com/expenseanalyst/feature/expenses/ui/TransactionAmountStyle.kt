package com.expenseanalyst.feature.expenses.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.expenseanalyst.core.theme.expenseColors
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.util.SpendClassifier

/**
 * How a transaction's amount is coloured and signed. Previously duplicated verbatim in
 * [ExpenseCard], [NeedsReviewCard] and ExpenseDetailScreen's content, all three of which
 * assumed every non-INCOME, non-PAYMENT row was an expense — so a transfer rendered as a red
 * `-SAR 4,000.00`, indistinguishable from real spending, while contributing nothing to any
 * total. Lives here rather than in `:core` because `:core` has no dependency on `:domain` and
 * all three call sites are in this module.
 *
 * [note] is a short qualifier appended to the subtitle line; null when none is needed.
 */
data class TransactionAmountStyle(
    val color: Color,
    val prefix: String,
    val note: String?,
    val noteColor: Color?
)


private const val TRANSFER_ARROWS = "⇄"

@Composable
@ReadOnlyComposable
fun transactionAmountStyle(expense: Expense): TransactionAmountStyle {
    val colors = MaterialTheme.expenseColors
    val income = colors.received
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val spend = colors.spend
    val payment = colors.payment
    val transferGrey = colors.transfer
    val review = colors.review

    // A loan leg counts toward neither total, so it must not read as spending (red) or income
    // (green) — that is the same mismatch that made unclassified transfers misleading. The sign
    // still says which way the money moved.
    if (SpendClassifier.isLoanLeg(expense)) {
        val sign = if (SpendClassifier.isLoanRepayment(expense)) "+" else "-"
        return TransactionAmountStyle(transferGrey, sign, "Loan", null)
    }

    // A linked reimbursement is out of both totals too (only a payment's difference counts),
    // so it gets the same neutral treatment as a loan leg.
    if (SpendClassifier.isReimbursedExpense(expense)) {
        return TransactionAmountStyle(transferGrey, "-", "Reimbursed", null)
    }
    if (SpendClassifier.isReimbursementPayback(expense)) {
        return TransactionAmountStyle(transferGrey, "+", "Reimbursement", null)
    }

    return when (expense.transactionType) {
        TransactionType.INCOME ->
            TransactionAmountStyle(income, "+", null, null)

        TransactionType.PAYMENT ->
            TransactionAmountStyle(payment, "-", null, null)

        TransactionType.TRANSFER -> when (expense.transferClassification) {
            TransferClassification.EXTERNAL ->
                TransactionAmountStyle(spend, "-", "Sent", null)
            TransferClassification.EXTERNAL_IN ->
                TransactionAmountStyle(income, "+", "Received", null)
            TransferClassification.OWN_ACCOUNT ->
                TransactionAmountStyle(transferGrey, TRANSFER_ARROWS, "Own transfer", null)
            // Unclassified: neutral, and explicitly labelled, because this row counts toward
            // nothing at all until the user says which kind of transfer it is.
            null ->
                TransactionAmountStyle(muted, TRANSFER_ARROWS, "Tap to classify", review)
        }

        TransactionType.EXPENSE ->
            TransactionAmountStyle(spend, "-", null, null)
    }
}
