package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.Expense
import kotlin.math.abs

/**
 * One auto-debit (SIP, loan EMI) can arrive as TWO bank messages — HDFC sends
 * "UPDATE: INR 15,000 debited … Info: ACH D- Groww-<ref>" and, separately,
 * "PAYMENT ALERT! INR 15,000 deducted … towards Groww UMRN: …". Their text differs, and so can
 * the payee's name ("INDIANESIGN" vs "INDIAN CLEARING CORP" for the same mandate), so neither the
 * exact-body nor the amount+merchant+day dedup catches the pair. Both were being saved: 4 rows,
 * SAR 4,138 counted twice (Jan, May 2026).
 *
 * Rule: a mandate/auto-debit message is a duplicate when another mandate debit for the same
 * amount and currency is already recorded within [WINDOW_MS].
 */
object MandateDebitMatcher {

    private val mandateWording = Regex("""(?i)\bACH\s*D-|\bUMRN\b|\bNACH\b""")
    private const val WINDOW_MS = 2L * 24 * 60 * 60 * 1000

    fun isMandateDebit(body: String?): Boolean = body != null && mandateWording.containsMatchIn(body)

    fun findTwin(
        amount: Double,
        currencyCode: String,
        atMillis: Long,
        body: String?,
        existing: List<Expense>
    ): Expense? {
        if (!isMandateDebit(body)) return null
        return existing.firstOrNull {
            !it.isDeleted && isMandateDebit(it.rawSmsBody) && it.currencyCode == currencyCode &&
                abs(it.amount - amount) < 0.01 && abs(it.date.toEpochMilliseconds() - atMillis) <= WINDOW_MS
        }
    }
}
