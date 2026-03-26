package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.repository.CurrencyRepository
import javax.inject.Inject

class RefreshCurrencyRatesUseCase @Inject constructor(
    private val repository: CurrencyRepository
) {
    suspend operator fun invoke(force: Boolean = false) {
        if (force || repository.isStale()) {
            repository.refreshRates()
        }
    }
}
