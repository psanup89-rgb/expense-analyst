package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MandateDebitMatcherTest {

    private val day = 24L * 60 * 60 * 1000
    private val t0 = 1_780_000_000_000L
    private val ach = "UPDATE: INR 15,000.00 debited from HDFC Bank XX1234 on 02-MAY-26. Info: ACH D- Groww-ABC123XYZ. Avl bal:INR 1.00"
    private val alert = "PAYMENT ALERT! INR 15000.00 deducted from HDFC Bank A/C No 1234 towards Groww UMRN: HDFC0000"

    private fun saved(amount: Double, body: String, at: Long) = Expense(
        id = 1, amount = amount, currencyCode = "INR", homeAmount = amount / 22, exchangeRate = null,
        description = "", category = Category(1, "Investments", "x", "#000000", true, 0),
        paymentMethod = PaymentMethod.NET_BANKING, transactionType = TransactionType.EXPENSE,
        date = Instant.fromEpochMilliseconds(at), merchantName = "Groww", sourceType = SourceType.SMS_AUTO,
        rawSmsBody = body
    )

    @Test
    fun `the alert for an auto-debit already recorded is its twin`() {
        assertNotNull(MandateDebitMatcher.findTwin(15000.0, "INR", t0 + 19 * 3_600_000L, alert, listOf(saved(15000.0, ach, t0))))
        // and the other way round
        assertNotNull(MandateDebitMatcher.findTwin(15000.0, "INR", t0, ach, listOf(saved(15000.0, alert, t0 + day))))
    }

    @Test
    fun `different amount, a later month or a non-mandate debit are not twins`() {
        assertNull(MandateDebitMatcher.findTwin(5000.0, "INR", t0, alert, listOf(saved(15000.0, ach, t0))))
        assertNull(MandateDebitMatcher.findTwin(15000.0, "INR", t0 + 30 * day, alert, listOf(saved(15000.0, ach, t0))))
        assertNull(MandateDebitMatcher.findTwin(15000.0, "INR", t0, "Rs.15000 debited at Amazon", listOf(saved(15000.0, ach, t0))))
    }
}
