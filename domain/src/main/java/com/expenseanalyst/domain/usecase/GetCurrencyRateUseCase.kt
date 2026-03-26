package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.model.CurrencyRate
import com.expenseanalyst.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrencyRateUseCase @Inject constructor(
    private val repository: CurrencyRepository
) {
    operator fun invoke(currencyCode: String): Flow<CurrencyRate?> = repository.getRate(currencyCode)
}
