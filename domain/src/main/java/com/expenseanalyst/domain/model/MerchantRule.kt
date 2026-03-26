package com.expenseanalyst.domain.model

data class MerchantRule(
    val id: Long = 0,
    val merchantPattern: String,
    val categoryId: Long,
    val categoryName: String,
    val createdAt: Long
)
