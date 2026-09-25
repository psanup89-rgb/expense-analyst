package com.expenseanalyst.data.mapper

import com.expenseanalyst.data.local.entity.ExpenseEntity
import com.expenseanalyst.data.local.relation.ExpenseWithCategory
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.model.TransferClassification
import com.expenseanalyst.domain.util.NeedsReviewEvaluator
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
    accountLastFour = account?.lastFour,
    rawSmsBody = expense.rawSmsBody,
    billId = expense.billId,
    isDeleted = expense.isDeleted,
    needsReview = expense.needsReview,
    reviewReasons = NeedsReviewEvaluator.decode(expense.needsReviewReasons),
    isReimbursable = expense.isReimbursable,
    reimbursedDate = expense.reimbursedDateMillis?.let { Instant.fromEpochMilliseconds(it) },
    refundOriginalExpenseId = expense.refundOriginalExpenseId,
    // Decoded tolerantly rather than with a bare valueOf (unlike the enums above): this column
    // is user-set and nullable, so an unrecognised value must degrade to "unclassified" instead
    // of crashing the whole list. Same posture as NeedsReviewEvaluator.decode.
    transferClassification = expense.transferClassification
        ?.let { runCatching { TransferClassification.valueOf(it) }.getOrNull() },
    loanId = expense.loanId
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
    updatedAtUtcMillis = updatedAt,
    needsReview = needsReview,
    needsReviewReasons = NeedsReviewEvaluator.encode(reviewReasons),
    isReimbursable = isReimbursable,
    reimbursedDateMillis = reimbursedDate?.toEpochMilliseconds(),
    refundOriginalExpenseId = refundOriginalExpenseId,
    // MUST stay in sync with toDomain above. repairExpenseConversions() and markReviewed() both
    // round-trip every row through this mapper via the full-row updateExpense, so an omission
    // here silently reverts every classification the user has made on the next app launch.
    transferClassification = transferClassification?.name,
    // Same rule as transferClassification above: omitting this here silently unlinks every loan
    // leg on the next full-row updateExpense (repairExpenseConversions runs on every launch).
    loanId = loanId
)
