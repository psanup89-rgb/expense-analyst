package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReimbursementTest {

    private val day = 24L * 60 * 60 * 1000
    private val t0 = 1_780_000_000_000L
    private val misc = Category(1, "Misc", "x", "#000000", true, 0)

    private fun row(
        id: Long, amount: Double, type: TransactionType, at: Long = t0,
        reimbursable: Boolean = false, reimbursedById: Long? = null, cover: Double? = null
    ) = Expense(
        id = id, amount = amount, currencyCode = "SAR", homeAmount = amount, exchangeRate = 1.0,
        description = "", category = misc, paymentMethod = PaymentMethod.CREDIT_CARD,
        transactionType = type, date = Instant.fromEpochMilliseconds(at), merchantName = "X",
        sourceType = SourceType.MANUAL, isReimbursable = reimbursable,
        reimbursedById = reimbursedById, reimbursementCover = cover
    )

    // ── Totals ──

    @Test
    fun `a linked expense and its exact payback leave both totals`() {
        val dinner = row(1, 500.0, TransactionType.EXPENSE, reimbursable = true, reimbursedById = 9)
        val payback = row(9, 500.0, TransactionType.INCOME, cover = 500.0)
        val lunch = row(2, 40.0, TransactionType.EXPENSE)
        val rows = listOf(dinner, payback, lunch)
        assertEquals(40.0, SpendClassifier.spendBreakdown(rows).net, 0.001)
        assertEquals(0.0, SpendClassifier.receivedBreakdown(rows).net, 0.001)
        assertEquals(1, SpendClassifier.spendBreakdown(rows).reimbursedCount)
        assertEquals(1, SpendClassifier.receivedBreakdown(rows).reimbursementPaybackCount)
        assertFalse(SpendClassifier.isSpend(dinner))
        assertFalse(SpendClassifier.isReceived(payback))
    }

    @Test
    fun `only the difference counts - a shortfall as spent, an excess as received`() {
        val short = row(9, 450.0, TransactionType.INCOME, cover = 500.0)
        assertEquals(50.0, SpendClassifier.spendValue(short), 0.001)
        assertEquals(0.0, SpendClassifier.receivedValue(short), 0.001)
        assertEquals(50.0, SpendClassifier.spendBreakdown(listOf(short)).net, 0.001)

        val over = row(9, 520.0, TransactionType.INCOME, cover = 500.0)
        assertEquals(0.0, SpendClassifier.spendValue(over), 0.001)
        assertEquals(20.0, SpendClassifier.receivedValue(over), 0.001)
        assertEquals(20.0, SpendClassifier.receivedBreakdown(listOf(over)).net, 0.001)
    }

    @Test
    fun `an unlinked reimbursable expense still counts - the user is out of pocket`() {
        val pending = row(1, 500.0, TransactionType.EXPENSE, reimbursable = true)
        assertTrue(SpendClassifier.isSpend(pending))
        assertEquals(500.0, SpendClassifier.spendValue(pending), 0.001)
    }

    @Test
    fun `a payback in the Refund category is not also netted as a refund`() {
        val payback = row(9, 500.0, TransactionType.INCOME, cover = 500.0)
            .copy(category = Category(2, "Refund", "x", "#000000", true, 0))
        assertFalse(SpendClassifier.isRefund(payback))
        assertEquals(0.0, SpendClassifier.spendBreakdown(listOf(payback)).refundTotal, 0.001)
    }

    // ── Suggestions ──

    @Test
    fun `a payment matching one pending expense suggests it`() {
        val a = row(1, 500.0, TransactionType.EXPENSE, at = t0 - 10 * day, reimbursable = true)
        val b = row(2, 120.0, TransactionType.EXPENSE, at = t0 - 5 * day, reimbursable = true)
        assertEquals(listOf(a), ReimbursementMatcher.suggest(500.0, "SAR", t0, listOf(a, b)))
    }

    @Test
    fun `a payment equal to the whole pending claim suggests all of it`() {
        val a = row(1, 500.0, TransactionType.EXPENSE, at = t0 - 10 * day, reimbursable = true)
        val b = row(2, 120.0, TransactionType.EXPENSE, at = t0 - 5 * day, reimbursable = true)
        assertEquals(listOf(a, b), ReimbursementMatcher.suggest(620.0, "SAR", t0, listOf(a, b)))
    }

    @Test
    fun `no suggestion for other amounts, repaid, unticked or later expenses`() {
        val a = row(1, 500.0, TransactionType.EXPENSE, at = t0 - 10 * day, reimbursable = true)
        assertNull(ReimbursementMatcher.suggest(499.0, "SAR", t0, listOf(a)))
        assertNull(ReimbursementMatcher.suggest(500.0, "INR", t0, listOf(a)))
        assertNull(ReimbursementMatcher.suggest(500.0, "SAR", t0, listOf(a.copy(reimbursedById = 9))))
        assertNull(ReimbursementMatcher.suggest(500.0, "SAR", t0, listOf(a.copy(isReimbursable = false))))
        assertNull(ReimbursementMatcher.suggest(500.0, "SAR", t0, listOf(a.copy(date = Instant.fromEpochMilliseconds(t0 + 5 * day)))))
    }
}
