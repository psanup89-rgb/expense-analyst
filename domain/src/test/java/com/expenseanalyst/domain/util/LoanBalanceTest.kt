package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoanBalanceTest {

    private val now = Instant.fromEpochMilliseconds(1_700_000_000_000L)

    // USD base: 1 USD = 3.75 SAR = 83 INR
    private val rates = mapOf(
        "USD" to CurrencyRate("USD", 1.0, now),
        "SAR" to CurrencyRate("SAR", 3.75, now),
        "INR" to CurrencyRate("INR", 83.0, now)
    )

    private fun loan(amount: Double = 500.0, currency: String = "SAR", homeAmount: Double? = null) = LentItem(
        id = 7,
        personName = "Friend",
        amount = amount,
        currencyCode = currency,
        homeAmount = homeAmount,
        description = "",
        lentDateMillis = 0L
    )

    private fun leg(
        amount: Double,
        currency: String = "SAR",
        homeAmount: Double? = amount,
        type: TransactionType = TransactionType.INCOME,
        classification: TransferClassification? = null,
        loanId: Long? = 7
    ) = Expense(
        id = 0,
        amount = amount,
        currencyCode = currency,
        homeAmount = homeAmount,
        exchangeRate = 1.0,
        description = "",
        category = Category(id = 1, name = "Other", iconName = "x", colorHex = "#000000", isDefault = true, sortOrder = 0),
        paymentMethod = PaymentMethod.UPI,
        transactionType = type,
        date = now,
        merchantName = "Friend",
        sourceType = SourceType.MANUAL,
        transferClassification = classification,
        loanId = loanId
    )

    @Test
    fun `partial repayments leave the rest outstanding`() {
        val b = LoanBalance.of(loan(), listOf(leg(200.0), leg(100.0)), rates)
        assertEquals(300.0, b.repaid, 0.001)
        assertEquals(200.0, b.remaining, 0.001)
        assertFalse(b.isFullyRepaid)
    }

    @Test
    fun `money lent out and other loans' rows are not repayments`() {
        val b = LoanBalance.of(
            loan(),
            listOf(
                leg(500.0, type = TransactionType.EXPENSE), // the card swipe itself
                leg(300.0, type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL),
                leg(400.0, loanId = 8)
            ),
            rates
        )
        assertEquals(0.0, b.repaid, 0.001)
        assertEquals(500.0, b.remaining, 0.001)
    }

    @Test
    fun `an inbound transfer counts as a repayment`() {
        val b = LoanBalance.of(
            loan(),
            listOf(leg(500.0, type = TransactionType.TRANSFER, classification = TransferClassification.EXTERNAL_IN)),
            rates
        )
        assertTrue(b.isFullyRepaid)
    }

    @Test
    fun `overpayment is ignored - remaining stops at zero`() {
        val b = LoanBalance.of(loan(), listOf(leg(350.0), leg(250.0)), rates)
        assertEquals(600.0, b.repaid, 0.001)
        assertEquals(0.0, b.remaining, 0.001)
        assertTrue(b.isFullyRepaid)
    }

    @Test
    fun `a repayment within a cent of the principal counts as full`() {
        assertTrue(LoanBalance.of(loan(), listOf(leg(499.995)), rates).isFullyRepaid)
        assertFalse(LoanBalance.of(loan(), listOf(leg(499.90)), rates).isFullyRepaid)
    }

    @Test
    fun `a repayment in another currency is converted into the loan's`() {
        // INR 8,300 = USD 100 = SAR 375
        val b = LoanBalance.of(loan(), listOf(leg(8300.0, currency = "INR", homeAmount = 375.0)), rates)
        assertEquals(375.0, b.repaid, 0.001)
        assertEquals(125.0, b.remaining, 0.001)
    }

    @Test
    fun `without rates the loan's own home ratio converts`() {
        // Loan INR 10,000 recorded as SAR 450 at home; a SAR 225 repayment is half of it.
        val b = LoanBalance.of(
            loan(amount = 10_000.0, currency = "INR", homeAmount = 450.0),
            listOf(leg(225.0, currency = "SAR", homeAmount = 225.0)),
            emptyMap()
        )
        assertEquals(5_000.0, b.repaid, 0.001)
    }

    @Test
    fun `remaining is expressed in home currency for totals`() {
        val l = loan(amount = 8300.0, currency = "INR")
        val b = LoanBalance.of(l, emptyList(), rates)
        assertEquals(375.0, b.remainingInHome(l, "SAR", rates), 0.001)
    }

    @Test
    fun `a zero principal is never fully repaid`() {
        assertFalse(LoanBalance.of(loan(amount = 0.0), emptyList(), rates).isFullyRepaid)
    }
}
