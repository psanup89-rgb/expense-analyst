package com.expenseanalyst.data.mapper

import com.expenseanalyst.data.local.entity.LentItemEntity
import com.expenseanalyst.domain.model.LentItem
import com.expenseanalyst.domain.model.LentStatus

fun LentItemEntity.toDomain() = LentItem(
    id = id,
    personName = personName,
    amount = amount,
    currencyCode = currencyCode,
    homeAmount = homeAmount,
    description = description,
    lentDateMillis = lentDateMillis,
    status = LentStatus.valueOf(status),
    settledAmount = settledAmount,
    settledDateMillis = settledDateMillis,
    linkedExpenseId = linkedExpenseId,
    settlementExpenseId = settlementExpenseId,
    reminderDatetimeMillis = reminderDatetimeMillis,
    isDeleted = isDeleted,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun LentItem.toEntity(
    createdAt: Long = createdAtMillis,
    updatedAt: Long = updatedAtMillis
) = LentItemEntity(
    id = id,
    personName = personName,
    amount = amount,
    currencyCode = currencyCode,
    homeAmount = homeAmount,
    description = description,
    lentDateMillis = lentDateMillis,
    status = status.name,
    settledAmount = settledAmount,
    settledDateMillis = settledDateMillis,
    linkedExpenseId = linkedExpenseId,
    settlementExpenseId = settlementExpenseId,
    reminderDatetimeMillis = reminderDatetimeMillis,
    isDeleted = isDeleted,
    createdAtMillis = createdAt,
    updatedAtMillis = updatedAt
)
