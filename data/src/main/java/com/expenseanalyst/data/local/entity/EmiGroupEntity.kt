package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emi_groups")
data class EmiGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "total_amount") val totalAmount: Double,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "number_of_installments") val numberOfInstallments: Int,
    @ColumnInfo(name = "installment_amount") val installmentAmount: Double,
    @ColumnInfo(name = "interest_rate") val interestRate: Double?,
    @ColumnInfo(name = "start_date_utc_millis") val startDateUtcMillis: Long,
    val description: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "created_at_utc_millis") val createdAtUtcMillis: Long
)
