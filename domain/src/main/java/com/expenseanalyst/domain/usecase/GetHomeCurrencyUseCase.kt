package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHomeCurrencyUseCase @Inject constructor(
    private val repository: CurrencyRepository
) {
    operator fun invoke(): Flow<String> = repository.getHomeCurrency()
}
