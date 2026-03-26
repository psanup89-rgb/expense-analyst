package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_rules",
    indices = [
        Index(value = ["merchant_pattern"], name = "idx_merchant_rules_pattern", unique = true)
    ]
)
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "merchant_pattern") val merchantPattern: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "created_at_utc_millis") val createdAtUtcMillis: Long
)
