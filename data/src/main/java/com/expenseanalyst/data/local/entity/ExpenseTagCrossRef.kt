package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "expense_tags",
    primaryKeys = ["expense_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tag_id"])]
)
data class ExpenseTagCrossRef(
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long
)
