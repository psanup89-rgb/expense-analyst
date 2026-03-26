package com.expenseanalyst.core.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    private val indianLocale = Locale("en", "IN")

    fun format(amount: Double, currencyCode: String): String {
        return try {
            when (currencyCode) {
                "INR" -> formatIndian(amount)
                else -> formatGeneric(amount, currencyCode)
            }
        } catch (e: Exception) {
            "$currencyCode ${String.format("%.2f", amount)}"
        }
    }

    private fun formatIndian(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(indianLocale)
        formatter.currency = Currency.getInstance("INR")
        formatter.maximumFractionDigits = 2
        return formatter.format(amount)
    }

    private fun formatGeneric(amount: Double, currencyCode: String): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        return try {
            formatter.currency = Currency.getInstance(currencyCode)
            formatter.maximumFractionDigits = 2
            formatter.format(amount)
        } catch (e: Exception) {
            "$currencyCode ${String.format("%.2f", amount)}"
        }
    }

    fun formatWithSign(amount: Double, currencyCode: String, isCredit: Boolean): String {
        val formatted = format(kotlin.math.abs(amount), currencyCode)
        return if (isCredit) "+$formatted" else "-$formatted"
    }
}
