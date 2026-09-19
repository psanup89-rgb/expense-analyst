package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RefundMatcherTest {

    private val category = Category(id = 1, name = "Shopping", iconName = "shopping_bag", colorHex = "#000000", isDefault = true, sortOrder = 0)
    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun expense(
        id: Long,
        amount: Double,
        currencyCode: String = "SAR",
        daysAgo: Long = 0,
        type: TransactionType = TransactionType.EXPENSE,
        isDeleted: Boolean = false,
        refundOriginalExpenseId: Long? = null
    ) = Expense(
        id = id,
        amount = amount,
        currencyCode = currencyCode,
        homeAmount = amount,
        exchangeRate = 1.0,
        description = "",
        category = category,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        transactionType = type,
        date = Instant.fromEpochMilliseconds(now - daysAgo * day),
        merchantName = "Merchant",
        sourceType = SourceType.MANUAL,
        isDeleted = isDeleted,
        refundOriginalExpenseId = refundOriginalExpenseId
    )

    @Test
    fun `matches an expense with the same amount and currency within the window`() {
        val original = expense(id = 1, amount = 250.0, daysAgo = 10)
        val match = RefundMatcher.findMatch(250.0, "SAR", now, listOf(original))
        assertEquals(1L, match?.id)
    }

    @Test
    fun `does not match outside the 90-day window`() {
        val original = expense(id = 1, amount = 250.0, daysAgo = 91)
        assertNull(RefundMatcher.findMatch(250.0, "SAR", now, listOf(original)))
    }

    @Test
    fun `does not match a different currency`() {
        val original = expense(id = 1, amount = 250.0, currencyCode = "USD", daysAgo = 10)
        assertNull(RefundMatcher.findMatch(250.0, "SAR", now, listOf(original)))
    }

    @Test
    fun `does not match a non-EXPENSE transaction type`() {
        val income = expense(id = 1, amount = 250.0, daysAgo = 10, type = TransactionType.INCOME)
        assertNull(RefundMatcher.findMatch(250.0, "SAR", now, listOf(income)))
    }

    @Test
    fun `does not match a deleted expense`() {
        val deleted = expense(id = 1, amount = 250.0, daysAgo = 10, isDeleted = true)
        assertNull(RefundMatcher.findMatch(250.0, "SAR", now, listOf(deleted)))
    }

    @Test
    fun `picks the most recent match when multiple candidates tie on amount`() {
        val older = expense(id = 1, amount = 250.0, daysAgo = 60)
        val newer = expense(id = 2, amount = 250.0, daysAgo = 5)
        val match = RefundMatcher.findMatch(250.0, "SAR", now, listOf(older, newer))
        assertEquals(2L, match?.id)
    }

    @Test
    fun `excludes an expense already claimed by an earlier refund`() {
        val alreadyRefunded = expense(id = 1, amount = 250.0, daysAgo = 20)
        val earlierRefund = expense(id = 2, amount = 250.0, daysAgo = 15, type = TransactionType.INCOME, refundOriginalExpenseId = 1)
        assertNull(RefundMatcher.findMatch(250.0, "SAR", now, listOf(alreadyRefunded, earlierRefund)))
    }

    @Test
    fun `no candidates returns null`() {
        assertNull(RefundMatcher.findMatch(250.0, "SAR", now, emptyList()))
    }
}
