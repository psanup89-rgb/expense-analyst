package com.expenseanalyst.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.expenseanalyst.data.local.entity.AccountEntity
import com.expenseanalyst.data.local.entity.CategoryEntity
import com.expenseanalyst.data.local.entity.ExpenseEntity
import com.expenseanalyst.data.local.entity.ExpenseTagCrossRef
import com.expenseanalyst.data.local.entity.TagEntity

data class ExpenseWithCategory(
    @Embedded val expense: ExpenseEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity,
    @Relation(
        parentColumn = "account_id",
        entityColumn = "id"
    )
    val account: AccountEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExpenseTagCrossRef::class,
            parentColumn = "expense_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity> = emptyList()
)
