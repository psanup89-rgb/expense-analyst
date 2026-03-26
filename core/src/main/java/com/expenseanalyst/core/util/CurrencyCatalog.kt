package com.expenseanalyst.core.util

import java.util.Currency
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val displayName: String,
    val symbol: String
)

object CurrencyCatalog {
    val all: List<CurrencyOption> by lazy {
        Currency.getAvailableCurrencies()
            .map { currency ->
                CurrencyOption(
                    code = currency.currencyCode,
                    displayName = currency.getDisplayName(Locale.getDefault()),
                    symbol = runCatching { currency.getSymbol(Locale.getDefault()) }
                        .getOrDefault(currency.currencyCode)
                )
            }
            .sortedBy { it.code }
    }
}
