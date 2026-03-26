package com.expenseanalyst.domain.model

import kotlinx.datetime.Instant

data class CurrencyRate(
    val currencyCode: String,
    val rateToBase: Double,
    val lastUpdated: Instant
)
