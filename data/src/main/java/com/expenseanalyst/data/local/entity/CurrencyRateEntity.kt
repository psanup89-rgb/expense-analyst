package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "rate_to_base") val rateToBase: Double,
    @ColumnInfo(name = "last_updated_utc_millis") val lastUpdatedUtcMillis: Long
)
