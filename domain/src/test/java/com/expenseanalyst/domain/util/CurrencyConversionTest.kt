package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CurrencyConversionTest {

    private val category = Category(id = 1, name = "Food", iconName = "food", colorHex = "#000000", isDefault = true, sortOrder = 0)

    private fun makeExpense(
        amount: Double,
        currency: String,
        homeAmount: Double? = null,
        exchangeRate: Double? = null
    ) = Expense(
        id = 1L,
        amount = amount,
        currencyCode = currency,
        homeAmount = homeAmount,
        exchangeRate = exchangeRate,
        description = "test",
        category = category,
        paymentMethod = PaymentMethod.CASH,
        transactionType = TransactionType.EXPENSE,
        date = Instant.fromEpochMilliseconds(0),
        merchantName = null,
        sourceType = SourceType.MANUAL
    )

    private fun rate(code: String, rateToBase: Double) =
        CurrencyRate(currencyCode = code, rateToBase = rateToBase, lastUpdated = Instant.fromEpochMilliseconds(0))

    @Test
    fun `same currency returns amount as homeAmount with rate 1`() {
        val expense = makeExpense(500.0, "INR")
        val result = CurrencyConversion.resolve(expense, "INR", emptyMap())
        assertEquals(500.0, result.homeAmount)
        assertEquals(1.0, result.exchangeRate)
    }

    @Test
    fun `converts USD to INR using rates`() {
        val expense = makeExpense(100.0, "USD")
        // USD rate to base = 1.0 (USD is base), INR rate to base = 83.0 (83 INR per 1 USD)
        val rates = mapOf(
            "USD" to rate("USD", 1.0),
            "INR" to rate("INR", 83.0)
        )
        val result = CurrencyConversion.resolve(expense, "INR", rates)
        // homeRate / sourceRate = 83.0 / 1.0 = 83.0
        assertEquals(8300.0, result.homeAmount!!, 0.1)
        assertEquals(83.0, result.exchangeRate!!, 0.01)
    }

    @Test
    fun `converts SAR to INR using rates`() {
        val expense = makeExpense(100.0, "SAR")
        // SAR rate to base = 0.267 (SAR per USD), INR rate to base = 83.0
        val rates = mapOf(
            "SAR" to rate("SAR", 0.267),
            "INR" to rate("INR", 83.0)
        )
        val result = CurrencyConversion.resolve(expense, "INR", rates)
        val expectedRate = 83.0 / 0.267
        assertEquals(100.0 * expectedRate, result.homeAmount!!, 1.0)
    }

    @Test
    fun `falls back to stored homeAmount when rates unavailable`() {
        val expense = makeExpense(100.0, "USD", homeAmount = 8200.0, exchangeRate = 82.0)
        val result = CurrencyConversion.resolve(expense, "INR", emptyMap())
        assertEquals(8200.0, result.homeAmount)
        assertEquals(82.0, result.exchangeRate)
    }

    @Test
    fun `needsSync returns false when amounts match`() {
        val expense = makeExpense(100.0, "USD", homeAmount = 8300.0, exchangeRate = 83.0)
        val rates = mapOf(
            "USD" to rate("USD", 1.0),
            "INR" to rate("INR", 83.0)
        )
        val needsSync = CurrencyConversion.needsSync(expense, "INR", rates)
        assertEquals(false, needsSync)
    }

    @Test
    fun `needsSync returns true when rates have changed significantly`() {
        val expense = makeExpense(100.0, "USD", homeAmount = 8200.0, exchangeRate = 82.0)
        val rates = mapOf(
            "USD" to rate("USD", 1.0),
            "INR" to rate("INR", 83.5)
        )
        val needsSync = CurrencyConversion.needsSync(expense, "INR", rates)
        assertEquals(true, needsSync)
    }

    @Test
    fun `resolve returns null homeAmount when rates missing and no stored value`() {
        val expense = makeExpense(100.0, "USD")
        val result = CurrencyConversion.resolve(expense, "INR", emptyMap())
        assertNull(result.homeAmount)
        assertNull(result.exchangeRate)
    }
}
