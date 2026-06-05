package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lent_items",
    indices = [
        Index(value = ["status"]),
        Index(value = ["is_deleted"])
    ]
)
data class LentItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "person_name") val personName: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "home_amount") val homeAmount: Double?,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "lent_date_millis") val lentDateMillis: Long,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "settled_amount") val settledAmount: Double?,
    @ColumnInfo(name = "settled_date_millis") val settledDateMillis: Long?,
    @ColumnInfo(name = "linked_expense_id") val linkedExpenseId: Long?,
    @ColumnInfo(name = "settlement_expense_id") val settlementExpenseId: Long?,
    @ColumnInfo(name = "reminder_datetime_millis") val reminderDatetimeMillis: Long?,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long
)
