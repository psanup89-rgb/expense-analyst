package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "biller_name") val billerName: String,
    @ColumnInfo(name = "account_id") val accountId: Long?,
    @ColumnInfo(name = "total_due") val totalDue: Double?,
    @ColumnInfo(name = "minimum_due") val minimumDue: Double?,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "due_date_millis") val dueDateMillis: Long?,
    @ColumnInfo(name = "statement_period_start_millis") val statementPeriodStart: Long?,
    @ColumnInfo(name = "statement_period_end_millis") val statementPeriodEnd: Long?,
    val status: String,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "reference") val reference: String? = null
)
