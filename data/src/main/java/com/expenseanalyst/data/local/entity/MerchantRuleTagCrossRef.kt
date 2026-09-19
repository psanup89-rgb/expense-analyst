package com.expenseanalyst.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "merchant_rule_tags",
    primaryKeys = ["rule_id", "tag_id"],
    foreignKeys = [
        ForeignKey(entity = MerchantRuleEntity::class, parentColumns = ["id"], childColumns = ["rule_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tag_id"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index(value = ["tag_id"])]
)
data class MerchantRuleTagCrossRef(
    @ColumnInfo(name = "rule_id") val ruleId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long
)
