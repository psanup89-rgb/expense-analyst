package com.expenseanalyst.domain.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * "Same amount + same merchant + same calendar day" — the rule live capture uses to drop a second
 * message about a transaction it has already saved (PendingNotificationManager's soft-dupe check).
 *
 * Banks often send two differently worded messages for one move of money (Al Rajhi does for
 * transfers), so matching the message text alone lets the second one through. SMS Import used to
 * apply this key only to rows saved without a message body; every other row was matched on its
 * exact text, so an import re-added the twin of a transfer live capture had rightly dropped
 * (Oct 2026: a SAR 4,000 loan transfer was counted again as Spent).
 *
 * Null when there is no merchant — without one the key would match unrelated rows.
 */
object SameTransactionKey {

    fun of(
        amount: Double,
        merchant: String?,
        epochMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String? {
        val name = merchant?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        val day = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date
        return "${"%.2f".format(amount)}|$name|$day"
    }
}
