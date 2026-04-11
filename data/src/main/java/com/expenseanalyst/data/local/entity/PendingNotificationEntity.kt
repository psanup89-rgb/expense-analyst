package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_notifications")
data class PendingNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "merchant_name") val merchantName: String?,
    @ColumnInfo(name = "bank_name") val bankName: String,
    @ColumnInfo(name = "account_last4") val accountLast4: String?,
    @ColumnInfo(name = "transaction_type") val transactionType: String,
    @ColumnInfo(name = "detected_at_millis") val detectedAtMillis: Long,
    @ColumnInfo(name = "raw_body") val rawBody: String? = null,
    @ColumnInfo(name = "payment_method") val paymentMethod: String? = null,
    @ColumnInfo(name = "is_possible_duplicate", defaultValue = "0") val isPossibleDuplicate: Boolean = false,
    @ColumnInfo(name = "pending_type", defaultValue = "TRANSACTION") val pendingType: String = "TRANSACTION",
    @ColumnInfo(name = "biller_name") val billerName: String? = null,
    @ColumnInfo(name = "due_date_millis") val dueDateMillis: Long? = null,
    @ColumnInfo(name = "linked_bill_id") val linkedBillId: Long? = null
)
