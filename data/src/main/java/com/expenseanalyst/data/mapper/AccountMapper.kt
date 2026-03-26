package com.expenseanalyst.data.mapper

import com.expenseanalyst.data.local.entity.AccountEntity
import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.model.AccountType

fun AccountEntity.toDomain() = Account(
    id = id,
    bankName = bankName,
    lastFour = lastFour,
    accountType = AccountType.valueOf(accountType),
    displayName = displayName
)

fun Account.toEntity() = AccountEntity(
    id = id,
    bankName = bankName,
    lastFour = lastFour,
    accountType = accountType.name,
    displayName = displayName
)
