package com.expenseanalyst.domain.usecase

import com.expenseanalyst.domain.repository.CurrencyRepository
import java.util.Locale
import javax.inject.Inject

class SetHomeCurrencyUseCase @Inject constructor(
    private val repository: CurrencyRepository
) {
    suspend operator fun invoke(currencyCode: String) {
        repository.setHomeCurrency(currencyCode.uppercase(Locale.US))
    }
}
