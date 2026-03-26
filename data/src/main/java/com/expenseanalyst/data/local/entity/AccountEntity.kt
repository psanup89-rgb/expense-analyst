package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["bank_name", "last_four"], name = "idx_accounts_bank_last4", unique = true)
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "bank_name") val bankName: String,
    @ColumnInfo(name = "last_four") val lastFour: String?,
    @ColumnInfo(name = "account_type") val accountType: String,
    @ColumnInfo(name = "display_name") val displayName: String
)
