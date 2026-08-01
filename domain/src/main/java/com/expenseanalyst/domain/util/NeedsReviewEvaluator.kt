package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.PaymentMethod

enum class ReviewReason(val label: String) {
    MISSING_MERCHANT("Merchant"),
    GENERIC_CATEGORY("Category"),
    UNKNOWN_PAYMENT_METHOD("Payment method"),
    UNRESOLVED_ACCOUNT("Account")
}

object NeedsReviewEvaluator {

    private val GENERIC_CATEGORY_NAMES = setOf("Other", "Misc")

    fun evaluate(
        merchantName: String?,
        categoryName: String,
        paymentMethod: PaymentMethod,
        accountLastFour: String?
    ): List<ReviewReason> = buildList {
        if (merchantName.isNullOrBlank()) add(ReviewReason.MISSING_MERCHANT)
        if (categoryName in GENERIC_CATEGORY_NAMES) add(ReviewReason.GENERIC_CATEGORY)
        if (paymentMethod == PaymentMethod.OTHER) add(ReviewReason.UNKNOWN_PAYMENT_METHOD)
        if (accountLastFour == null) add(ReviewReason.UNRESOLVED_ACCOUNT)
    }

    fun encode(reasons: List<ReviewReason>): String = reasons.joinToString(",") { it.name }

    fun decode(raw: String?): List<ReviewReason> =
        raw?.takeIf { it.isNotBlank() }?.split(",")
            ?.mapNotNull { runCatching { ReviewReason.valueOf(it) }.getOrNull() }
            ?: emptyList()
}
