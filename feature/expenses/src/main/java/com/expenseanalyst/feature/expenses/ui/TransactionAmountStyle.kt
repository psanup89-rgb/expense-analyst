package com.expenseanalyst.feature.expenses.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
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

private val SPEND_RED = Color(0xFFFF5555)
private val PAYMENT_PURPLE = Color(0xFF7C5CBF)
private val TRANSFER_GREY = Color(0xFF607D8B)
private val REVIEW_AMBER = Color(0xFFF57C00)

private const val TRANSFER_ARROWS = "⇄"

@Composable
@ReadOnlyComposable
fun transactionAmountStyle(expense: Expense): TransactionAmountStyle {
    val income = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    // A loan leg counts toward neither total, so it must not read as spending (red) or income
    // (green) — that is the same mismatch that made unclassified transfers misleading. The sign
    // still says which way the money moved.
    if (SpendClassifier.isLoanLeg(expense)) {
        val sign = if (SpendClassifier.isLoanRepayment(expense)) "+" else "-"
        return TransactionAmountStyle(TRANSFER_GREY, sign, "Loan", null)
    }

    return when (expense.transactionType) {
        TransactionType.INCOME ->
            TransactionAmountStyle(income, "+", null, null)

        TransactionType.PAYMENT ->
            TransactionAmountStyle(PAYMENT_PURPLE, "-", null, null)

        TransactionType.TRANSFER -> when (expense.transferClassification) {
            TransferClassification.EXTERNAL ->
                TransactionAmountStyle(SPEND_RED, "-", "Sent", null)
            TransferClassification.EXTERNAL_IN ->
                TransactionAmountStyle(income, "+", "Received", null)
            TransferClassification.OWN_ACCOUNT ->
                TransactionAmountStyle(TRANSFER_GREY, TRANSFER_ARROWS, "Own transfer", null)
            // Unclassified: neutral, and explicitly labelled, because this row counts toward
            // nothing at all until the user says which kind of transfer it is.
            null ->
                TransactionAmountStyle(muted, TRANSFER_ARROWS, "Tap to classify", REVIEW_AMBER)
        }

        TransactionType.EXPENSE ->
            TransactionAmountStyle(SPEND_RED, "-", null, null)
    }
}
