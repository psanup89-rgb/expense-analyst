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

class ReviewCategoryPlannerTest {

    private fun cat(id: Long, name: String, order: Int = id.toInt()) = Category(id, name, "x", "#000000", true, order)
    private val misc = cat(1, "Misc")
    private val other = cat(2, "Other")
    private val food = cat(3, "Food & Drinks")
    private val shopping = cat(4, "Shopping")
    private val bills = cat(5, "Bills")
    private val all = listOf(misc, other, food, shopping, bills)

    private fun row(
        id: Long, merchant: String?, category: Category = misc,
        reasons: List<ReviewReason> = listOf(ReviewReason.GENERIC_CATEGORY)
    ) = Expense(
        id = id, amount = 10.0, currencyCode = "SAR", homeAmount = 10.0, exchangeRate = 1.0, description = "",
        category = category, paymentMethod = PaymentMethod.CREDIT_CARD, transactionType = TransactionType.EXPENSE,
        date = Instant.fromEpochMilliseconds(1_790_000_000_000L), merchantName = merchant,
        sourceType = SourceType.NOTIFICATION_AUTO, needsReview = true, reviewReasons = reasons
    )

    @Test
    fun `the button is for rows that ask for a category`() {
        assertTrue(ReviewCategoryPlanner.needsCategory(row(1, "Noon")))
        assertFalse(ReviewCategoryPlanner.needsCategory(row(1, "Noon", reasons = listOf(ReviewReason.UNRESOLVED_ACCOUNT))))
    }

    @Test
    fun `a bank's own stand-in name is never remembered as a merchant rule`() {
        val standIn = row(1, "HDFC Bank", reasons = listOf(ReviewReason.MISSING_MERCHANT, ReviewReason.GENERIC_CATEGORY))
        assertFalse(ReviewCategoryPlanner.canRememberMerchant(standIn))
        assertNull(ReviewCategoryPlanner.merchantPattern(standIn))
        assertFalse(ReviewCategoryPlanner.canRememberMerchant(row(1, "  ")))
        assertEquals("noon", ReviewCategoryPlanner.merchantPattern(row(1, "  NOON ")))
    }

    @Test
    fun `same merchant rows wait for a category, ignoring case, itself and rows that don't ask`() {
        val picked = row(1, "Noon")
        val rows = listOf(
            picked, row(2, "NOON"), row(3, "noon"), row(4, "Careem"),
            row(5, "Noon", reasons = listOf(ReviewReason.UNRESOLVED_ACCOUNT)),
            row(6, "Noon", reasons = listOf(ReviewReason.MISSING_MERCHANT, ReviewReason.GENERIC_CATEGORY))
        )
        assertEquals(listOf(2L, 3L), ReviewCategoryPlanner.sameMerchantRows(picked, rows).map { it.id })
        // no safe merchant → nothing to batch
        val standIn = row(7, "HDFC Bank", reasons = listOf(ReviewReason.MISSING_MERCHANT, ReviewReason.GENERIC_CATEGORY))
        assertTrue(ReviewCategoryPlanner.sameMerchantRows(standIn, rows + standIn).isEmpty())
    }

    @Test
    fun `offered categories skip Misc and Other, merchant's usual ones first`() {
        val picked = row(1, "Noon")
        val history = listOf(
            row(10, "Noon", shopping, emptyList()), row(11, "NOON", shopping, emptyList()),
            row(12, "Noon", food, emptyList()), row(13, "Careem", bills, emptyList())
        )
        assertEquals(
            listOf("Shopping", "Food & Drinks", "Bills"),
            ReviewCategoryPlanner.offeredCategories(picked, all, history).map { it.name }
        )
        // no history → the user's own order
        assertEquals(
            listOf("Food & Drinks", "Shopping", "Bills"),
            ReviewCategoryPlanner.offeredCategories(picked, all, emptyList()).map { it.name }
        )
    }
}
