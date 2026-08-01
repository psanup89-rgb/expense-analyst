package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.PaymentMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NeedsReviewEvaluatorTest {

    private fun evaluate(
        merchantName: String? = "Swiggy",
        categoryName: String = "Food",
        paymentMethod: PaymentMethod = PaymentMethod.UPI,
        accountLastFour: String? = "1234"
    ) = NeedsReviewEvaluator.evaluate(merchantName, categoryName, paymentMethod, accountLastFour)

    @Test
    fun `no reasons when every field is resolved`() {
        assertTrue(evaluate().isEmpty())
    }

    @Test
    fun `flags missing merchant`() {
        assertEquals(listOf(ReviewReason.MISSING_MERCHANT), evaluate(merchantName = null))
        assertEquals(listOf(ReviewReason.MISSING_MERCHANT), evaluate(merchantName = "  "))
    }

    @Test
    fun `flags generic category`() {
        assertEquals(listOf(ReviewReason.GENERIC_CATEGORY), evaluate(categoryName = "Other"))
        assertEquals(listOf(ReviewReason.GENERIC_CATEGORY), evaluate(categoryName = "Misc"))
    }

    @Test
    fun `flags unknown payment method`() {
        assertEquals(
            listOf(ReviewReason.UNKNOWN_PAYMENT_METHOD),
            evaluate(paymentMethod = PaymentMethod.OTHER)
        )
    }

    @Test
    fun `flags unresolved account`() {
        assertEquals(listOf(ReviewReason.UNRESOLVED_ACCOUNT), evaluate(accountLastFour = null))
    }

    @Test
    fun `combines multiple reasons in a stable order`() {
        val reasons = evaluate(merchantName = null, categoryName = "Other", accountLastFour = null)
        assertEquals(
            listOf(ReviewReason.MISSING_MERCHANT, ReviewReason.GENERIC_CATEGORY, ReviewReason.UNRESOLVED_ACCOUNT),
            reasons
        )
    }

    @Test
    fun `encode decode round trip`() {
        val reasons = listOf(ReviewReason.MISSING_MERCHANT, ReviewReason.UNRESOLVED_ACCOUNT)
        assertEquals(reasons, NeedsReviewEvaluator.decode(NeedsReviewEvaluator.encode(reasons)))
    }

    @Test
    fun `encode decode round trip for empty list`() {
        assertTrue(NeedsReviewEvaluator.decode(NeedsReviewEvaluator.encode(emptyList())).isEmpty())
    }

    @Test
    fun `decode handles null blank and malformed input without throwing`() {
        assertTrue(NeedsReviewEvaluator.decode(null).isEmpty())
        assertTrue(NeedsReviewEvaluator.decode("").isEmpty())
        assertTrue(NeedsReviewEvaluator.decode("NOT_A_REAL_REASON").isEmpty())
        assertEquals(
            listOf(ReviewReason.MISSING_MERCHANT),
            NeedsReviewEvaluator.decode("MISSING_MERCHANT,GARBAGE")
        )
    }
}
