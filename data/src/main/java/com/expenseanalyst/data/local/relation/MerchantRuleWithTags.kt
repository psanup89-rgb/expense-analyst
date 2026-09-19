package com.expenseanalyst.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.expenseanalyst.data.local.entity.MerchantRuleEntity
import com.expenseanalyst.data.local.entity.MerchantRuleTagCrossRef
import com.expenseanalyst.data.local.entity.TagEntity

data class MerchantRuleWithTags(
    @Embedded val rule: MerchantRuleEntity,
    @Relation(
        parentColumn = "id", entityColumn = "id",
        associateBy = Junction(value = MerchantRuleTagCrossRef::class, parentColumn = "rule_id", entityColumn = "tag_id")
    )
    val tags: List<TagEntity> = emptyList()
)
