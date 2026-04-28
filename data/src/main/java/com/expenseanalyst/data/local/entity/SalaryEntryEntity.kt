package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "salary_entries",
    indices = [Index(value = ["month", "year"], unique = true)]
)
data class SalaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val month: Int,
    val year: Int,
    @ColumnInfo(name = "source_expense_id") val sourceExpenseId: Long? = null,
    @ColumnInfo(name = "is_confirmed", defaultValue = "1") val isConfirmed: Boolean = true,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long
)
