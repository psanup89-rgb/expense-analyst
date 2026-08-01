package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"]
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["date_utc_millis"], name = "idx_expenses_date"),
        Index(value = ["category_id"], name = "idx_expenses_category"),
        Index(value = ["emi_group_id"], name = "idx_expenses_emi_group"),
        Index(value = ["is_deleted"], name = "idx_expenses_deleted"),
        Index(value = ["account_id"], name = "idx_expenses_account")
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "home_amount") val homeAmount: Double?,
    @ColumnInfo(name = "exchange_rate") val exchangeRate: Double?,
    val description: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "transaction_type") val transactionType: String,
    @ColumnInfo(name = "date_utc_millis") val dateUtcMillis: Long,
    @ColumnInfo(name = "merchant_name") val merchantName: String?,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "source_sender") val sourceSender: String?,
    @ColumnInfo(name = "emi_group_id") val emiGroupId: Long?,
    @ColumnInfo(name = "emi_installment_number") val emiInstallmentNumber: Int?,
    @ColumnInfo(name = "account_number") val accountNumber: String? = null,
    @ColumnInfo(name = "account_id") val accountId: Long? = null,
    @ColumnInfo(name = "raw_sms_body") val rawSmsBody: String? = null,
    @ColumnInfo(name = "bill_id") val billId: Long? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
    @ColumnInfo(name = "created_at_utc_millis") val createdAtUtcMillis: Long,
    @ColumnInfo(name = "updated_at_utc_millis") val updatedAtUtcMillis: Long,
    @ColumnInfo(name = "needs_review", defaultValue = "0") val needsReview: Boolean = false,
    @ColumnInfo(name = "needs_review_reasons") val needsReviewReasons: String? = null
)
