package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> = repository.getAccounts()
}
