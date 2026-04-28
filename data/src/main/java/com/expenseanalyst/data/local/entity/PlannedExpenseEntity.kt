package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planned_expenses")
data class PlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val month: Int,
    val year: Int,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long
)
