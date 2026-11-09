package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
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

class BillPaymentTwinsTest {

    private val misc = Category(1, "Misc", "x", "#000000", true, 0)
    private val t0 = 1_793_000_000_000L
    private val minute = 60_000L

    private fun pay(
        id: Long, amount: Double, at: Long = t0, merchant: String = "BillPayments",
        type: TransactionType = TransactionType.PAYMENT, currency: String = "INR", deleted: Boolean = false
    ) = Expense(
        id = id, amount = amount, currencyCode = currency, homeAmount = amount, exchangeRate = 1.0,
        description = "", category = misc, paymentMethod = PaymentMethod.UPI, transactionType = type,
        date = Instant.fromEpochMilliseconds(at), merchantName = merchant,
        sourceType = SourceType.NOTIFICATION_AUTO, isDeleted = deleted
    )

    @Test
    fun `the two messages of one card payment are twins`() {
        // IDFC: UPI debit from savings at 23:59, the card's "BillPayments" 15 minutes later
        val debit = pay(1, 7313.64, merchant = "IDFC Bank Credit Card Bill")
        val credit = pay(2, 7313.64, at = t0 + 15 * minute)
        assertTrue(BillPaymentTwins.areTwins(debit, credit))
        assertEquals(listOf(debit), BillPaymentTwins.distinct(listOf(credit, debit)))
    }

    @Test
    fun `not twins - other amount, currency, type, far apart, deleted, or itself`() {
        val p = pay(1, 299.0)
        assertFalse(BillPaymentTwins.areTwins(p, pay(2, 300.0)))
        assertFalse(BillPaymentTwins.areTwins(p, pay(2, 299.0, currency = "SAR")))
        assertFalse(BillPaymentTwins.areTwins(p, pay(2, 299.0, type = TransactionType.EXPENSE)))
        assertFalse(BillPaymentTwins.areTwins(p, pay(2, 299.0, at = t0 + 3L * 24 * 60 * minute)))
        assertFalse(BillPaymentTwins.areTwins(p, pay(2, 299.0, deleted = true)))
        assertFalse(BillPaymentTwins.areTwins(p, p))
    }

    @Test
    fun `a bill paid by one payment reported twice settles once, not as double`() {
        val a = pay(1, 600.0)
        val b = pay(2, 600.0, at = t0 + minute)
        // Without twin collapsing 600 + 600 would settle a 1,200 bill
        assertEquals(BillStatus.PARTIAL, BillSettlement.status(1200.0, "INR", listOf(a, b), "INR", emptyMap()))
        assertEquals(BillStatus.SETTLED, BillSettlement.status(600.0, "INR", listOf(a, b), "INR", emptyMap()))
    }

    private fun bill(id: Long, total: Double, currency: String = "INR", name: String = "IDFC FIRST Bank") = Bill(
        id = id, billerName = name, totalDue = total, currencyCode = currency,
        status = BillStatus.PENDING, sourceType = SourceType.SMS_AUTO, createdAtMillis = 0L
    )

    @Test
    fun `a payment of exactly one open bill's total links even when the payee name differs`() {
        val bills = listOf(bill(15, 7313.64), bill(13, 299.0, name = "OneCard"))
        assertEquals(15L, BillMatcher.findMatchingOpenBill(7313.64, "BillPayments", bills, "INR")?.id)
        assertEquals(13L, BillMatcher.findMatchingOpenBill(299.0, "Federal One Credit card", bills, "INR")?.id)
        // other currency, or two bills of that amount — no guess
        assertNull(BillMatcher.findMatchingOpenBill(299.0, "BillPayments", bills, "SAR"))
        assertNull(BillMatcher.findMatchingOpenBill(299.0, "BillPayments", bills + bill(16, 299.0, name = "Axis"), "INR"))
        // without a currency the old strict rule applies
        assertNull(BillMatcher.findMatchingOpenBill(7313.64, "BillPayments", bills))
    }
}
