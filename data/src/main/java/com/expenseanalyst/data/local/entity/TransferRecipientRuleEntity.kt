package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Remembered transfer classification per recipient. The unique index declaration here must match
 * MIGRATION_26_27's `CREATE UNIQUE INDEX` exactly — name and columns — or Room's schema
 * validation throws on database open.
 */
@Entity(
    tableName = "transfer_recipient_rules",
    indices = [
        Index(value = ["recipient_key"], name = "idx_transfer_recipient_rules_key", unique = true)
    ]
)
data class TransferRecipientRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipient_key") val recipientKey: String,
    @ColumnInfo(name = "recipient_display_name") val recipientDisplayName: String,
    val classification: String,
    @ColumnInfo(name = "created_at_utc_millis") val createdAtUtcMillis: Long
)
