package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.usecase.LinkBillPaymentUseCase
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BillSettlementTest {

    private val misc = Category(1, "Misc", "x", "#000000", true, 0)
    private val now = Instant.fromEpochMilliseconds(1_790_000_000_000L)
    // rateToBase: units per USD
    private val rates = listOf(
        CurrencyRate("USD", 1.0, now),
        CurrencyRate("SAR", 3.75, now),
        CurrencyRate("INR", 83.0, now)
    ).associateBy { it.currencyCode }

    private fun payment(amount: Double, currency: String = "SAR", home: Double? = amount, deleted: Boolean = false) =
        Expense(
            amount = amount, currencyCode = currency, homeAmount = home, exchangeRate = null,
            description = "", category = misc, paymentMethod = PaymentMethod.NET_BANKING,
            transactionType = TransactionType.PAYMENT, date = now, merchantName = "ENBD PAYMENTS",
            sourceType = SourceType.MANUAL, isDeleted = deleted
        )

    private fun status(totalDue: Double?, currency: String, vararg p: Expense) =
        BillSettlement.status(totalDue, currency, p.toList(), "SAR", rates)

    @Test
    fun `no payments is pending, no amount due is settled by any payment`() {
        assertEquals(BillStatus.PENDING, status(100.0, "SAR"))
        assertEquals(BillStatus.SETTLED, status(null, "SAR", payment(5.0)))
    }

    @Test
    fun `a payment rounded up to the riyal settles the statement`() {
        assertEquals(BillStatus.SETTLED, status(4829.82, "SAR", payment(4830.0)))
        assertEquals(BillStatus.SETTLED, status(4191.59, "SAR", payment(4191.5)))
    }

    @Test
    fun `two part payments settle together — the second used to leave it Partial`() {
        assertEquals(BillStatus.PARTIAL, status(1000.0, "SAR", payment(600.0)))
        assertEquals(BillStatus.SETTLED, status(1000.0, "SAR", payment(600.0), payment(400.0)))
    }

    @Test
    fun `a deleted payment does not count`() {
        assertEquals(BillStatus.PENDING, status(1000.0, "SAR", payment(1000.0, deleted = true)))
        assertEquals(BillStatus.PARTIAL, status(1000.0, "SAR", payment(400.0), payment(600.0, deleted = true)))
    }

    @Test
    fun `an INR bill is compared in INR, not against the SAR home amount`() {
        // INR 41,500 paid = SAR 1,875 at home. The old check compared 1,875 to 41,500 → Partial.
        val inr = payment(41_500.0, currency = "INR", home = 1_875.0)
        assertEquals(BillStatus.SETTLED, status(41_500.0, "INR", inr))
        // A SAR payment toward an INR bill is converted through the rate table
        assertEquals(BillStatus.SETTLED, status(41_500.0, "INR", payment(1_875.0)))
        assertEquals(BillStatus.PARTIAL, status(41_500.0, "INR", payment(1_000.0)))
    }

    @Test
    fun `without rates, only the home currency can be reached`() {
        val inr = payment(83.0, currency = "INR", home = 3.75)
        assertEquals(3.75, BillSettlement.amountInCurrency(inr, "SAR", "SAR", emptyMap())!!, 0.001)
        assertNull(BillSettlement.amountInCurrency(payment(10.0), "INR", "SAR", emptyMap()))
    }

    @Test
    fun `a bill paid from a bank account is net banking`() {
        assertEquals(PaymentMethod.NET_BANKING, LinkBillPaymentUseCase.methodForBillPayment(AccountType.SAVINGS))
        assertEquals(PaymentMethod.NET_BANKING, LinkBillPaymentUseCase.methodForBillPayment(AccountType.CURRENT))
        assertEquals(PaymentMethod.CREDIT_CARD, LinkBillPaymentUseCase.methodForBillPayment(AccountType.CREDIT_CARD))
        assertNull(LinkBillPaymentUseCase.methodForBillPayment(null))
    }

    @Test
    fun `only a statement due within a week of the open bill updates it`() {
        val day = 24L * 60 * 60 * 1000
        val due = 1_790_899_200_000L
        assert(BillSettlement.isSameCycle(due, due + 2 * day))
        // next month's statement is a new bill
        assert(!BillSettlement.isSameCycle(due, due + 30 * day))
        // an open bill with no due date (Al Rajhi August) can't be told apart → new bill
        assert(!BillSettlement.isSameCycle(null, due))
        assert(!BillSettlement.isSameCycle(due, null))
    }
}
