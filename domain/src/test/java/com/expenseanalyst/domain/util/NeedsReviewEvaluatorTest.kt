package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NeedsReviewEvaluatorTest {

    private fun evaluate(
        merchantName: String? = "Swiggy",
        categoryName: String = "Food",
        paymentMethod: PaymentMethod = PaymentMethod.UPI,
        accountLastFour: String? = "1234",
        transactionType: TransactionType = TransactionType.EXPENSE,
        transferClassification: TransferClassification? = null
    ) = NeedsReviewEvaluator.evaluate(
        merchantName,
        categoryName,
        paymentMethod,
        accountLastFour,
        transactionType,
        transferClassification
    )

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
    fun `flags an unclassified transfer`() {
        assertEquals(
            listOf(ReviewReason.UNCLASSIFIED_TRANSFER),
            evaluate(transactionType = TransactionType.TRANSFER, transferClassification = null)
        )
    }

    @Test
    fun `does not flag a classified transfer`() {
        assertTrue(
            evaluate(
                transactionType = TransactionType.TRANSFER,
                transferClassification = TransferClassification.EXTERNAL
            ).isEmpty()
        )
    }

    @Test
    fun `the transfer flag is scoped to transfers only`() {
        assertTrue(evaluate(transactionType = TransactionType.EXPENSE, transferClassification = null).isEmpty())
        assertTrue(evaluate(transactionType = TransactionType.INCOME, transferClassification = null).isEmpty())
    }

    @Test
    fun `remove drops only the named reason and keeps the rest`() {
        // The case that decides whether a classified transfer stays in Needs Review: it must,
        // when it was also flagged for something else.
        val raw = NeedsReviewEvaluator.encode(
            listOf(ReviewReason.MISSING_MERCHANT, ReviewReason.UNCLASSIFIED_TRANSFER, ReviewReason.UNRESOLVED_ACCOUNT)
        )
        assertEquals(
            listOf(ReviewReason.MISSING_MERCHANT, ReviewReason.UNRESOLVED_ACCOUNT),
            NeedsReviewEvaluator.remove(raw, ReviewReason.UNCLASSIFIED_TRANSFER)
        )
    }

    @Test
    fun `remove leaves nothing when it was the only reason`() {
        val raw = NeedsReviewEvaluator.encode(listOf(ReviewReason.UNCLASSIFIED_TRANSFER))
        assertTrue(NeedsReviewEvaluator.remove(raw, ReviewReason.UNCLASSIFIED_TRANSFER).isEmpty())
    }

    @Test
    fun `remove tolerates a null or malformed reason string`() {
        assertTrue(NeedsReviewEvaluator.remove(null, ReviewReason.UNCLASSIFIED_TRANSFER).isEmpty())
        assertEquals(
            listOf(ReviewReason.MISSING_MERCHANT),
            NeedsReviewEvaluator.remove("MISSING_MERCHANT,GARBAGE", ReviewReason.UNCLASSIFIED_TRANSFER)
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
