package com.expenseanalyst.data.mapper

import com.expenseanalyst.data.local.entity.CurrencyRateEntity
import com.expenseanalyst.domain.model.CurrencyRate
import kotlinx.datetime.Instant

fun CurrencyRateEntity.toDomain() = CurrencyRate(
    currencyCode = currencyCode,
    rateToBase = rateToBase,
    lastUpdated = Instant.fromEpochMilliseconds(lastUpdatedUtcMillis)
)
