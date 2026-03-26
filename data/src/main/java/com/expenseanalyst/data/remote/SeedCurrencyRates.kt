package com.expenseanalyst.data.remote

object SeedCurrencyRates {
    // Offline-safe seed rates relative to USD. A future live sync can overwrite these.
    val usdBaseRates: Map<String, Double> = linkedMapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "INR" to 83.10,
        "SAR" to 3.75,
        "AED" to 3.67,
        "QAR" to 3.64,
        "KWD" to 0.31,
        "BHD" to 0.38,
        "OMR" to 0.38,
        "EGP" to 49.40,
        "TRY" to 32.00,
        "PKR" to 278.00,
        "BDT" to 117.00,
        "NPR" to 133.00,
        "LKR" to 301.00,
        "JPY" to 150.00,
        "CNY" to 7.23,
        "HKD" to 7.82,
        "SGD" to 1.34,
        "AUD" to 1.53,
        "CAD" to 1.35,
        "CHF" to 0.90,
        "NZD" to 1.67,
        "ZAR" to 18.30,
        "SEK" to 10.40,
        "NOK" to 10.60,
        "DKK" to 6.87,
        "THB" to 35.90,
        "MYR" to 4.70,
        "IDR" to 15700.00,
        "PHP" to 56.50,
        "KRW" to 1330.00,
        "RUB" to 91.00,
        "BRL" to 5.00,
        "MXN" to 16.80
    )
}
