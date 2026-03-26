package com.expenseanalyst.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.expenseanalyst.data.local.entity.AccountEntity
import com.expenseanalyst.data.local.entity.CategoryEntity
import com.expenseanalyst.data.local.entity.ExpenseEntity

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
    val account: AccountEntity?
)
