package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.repository.AccountRepository
import javax.inject.Inject

class FindOrCreateAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(bankName: String, lastFour: String?, accountType: AccountType): Long =
        repository.findOrCreate(bankName, lastFour, accountType)
}
