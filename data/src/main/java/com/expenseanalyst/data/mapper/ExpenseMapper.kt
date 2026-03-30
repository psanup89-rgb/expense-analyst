package com.expenseanalyst.data.mapper

import com.expenseanalyst.data.local.entity.ExpenseEntity
import com.expenseanalyst.data.local.relation.ExpenseWithCategory
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import kotlinx.datetime.Instant

fun ExpenseWithCategory.toDomain() = Expense(
    id = expense.id,
    amount = expense.amount,
    currencyCode = expense.currencyCode,
    homeAmount = expense.homeAmount,
    exchangeRate = expense.exchangeRate,
    description = expense.description,
    category = category.toDomain(),
    paymentMethod = PaymentMethod.valueOf(expense.paymentMethod),
    transactionType = TransactionType.valueOf(expense.transactionType),
    date = Instant.fromEpochMilliseconds(expense.dateUtcMillis),
    merchantName = expense.merchantName,
    sourceType = SourceType.valueOf(expense.sourceType),
    sourceSender = expense.sourceSender,
    emiGroupId = expense.emiGroupId,
    emiInstallmentNumber = expense.emiInstallmentNumber,
    tags = tags.map { it.toDomain() },
    accountId = expense.accountId,
    accountDisplayName = account?.displayName,
    rawSmsBody = expense.rawSmsBody,
    billId = expense.billId,
    isDeleted = expense.isDeleted
)

fun Expense.toEntity(createdAt: Long, updatedAt: Long) = ExpenseEntity(
    id = id,
    amount = amount,
    currencyCode = currencyCode,
    homeAmount = homeAmount,
    exchangeRate = exchangeRate,
    description = description,
    categoryId = category.id,
    paymentMethod = paymentMethod.name,
    transactionType = transactionType.name,
    dateUtcMillis = date.toEpochMilliseconds(),
    merchantName = merchantName,
    sourceType = sourceType.name,
    sourceSender = sourceSender,
    emiGroupId = emiGroupId,
    emiInstallmentNumber = emiInstallmentNumber,
    accountNumber = null,
    accountId = accountId,
    rawSmsBody = rawSmsBody,
    billId = billId,
    isDeleted = isDeleted,
    createdAtUtcMillis = createdAt,
    updatedAtUtcMillis = updatedAt
)
