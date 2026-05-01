package com.expenseanalyst.feature.notification.service

import android.content.Context
import com.expenseanalyst.domain.model.PendingNotification
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.feature.notification.parser.ParsedTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds parsed transactions that are awaiting user confirmation.
 * The UI observes [pending] for the in-app banner.
 * Each enqueued transaction is also persisted to the DB so the user
 * can review it later from the Pending Inbox screen.
 *
 * The system tray notification is posted here (after DB save) so the
 * [pendingId] can be embedded in the notification tap intent, ensuring
 * the Add Expense screen can always load the raw SMS body.
 */
@Singleton
class PendingNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PendingNotificationRepository,
    private val expenseRepository: ExpenseRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _pending = MutableStateFlow<ParsedTransaction?>(null)
    val pending: StateFlow<ParsedTransaction?> = _pending.asStateFlow()

    /** ID of the most recently saved pending notification. Used by the in-app banner. */
    private val _lastPendingId = MutableStateFlow<Long?>(null)
    val lastPendingId: StateFlow<Long?> = _lastPendingId.asStateFlow()

    fun enqueue(transaction: ParsedTransaction) {
        _pending.value = transaction  // emit immediately; flag updated below after DB check
        scope.launch {
            // ── Dedup: skip if same SMS already in pending inbox or saved expenses ──
            val rawBody = transaction.rawBody?.trim()
            if (!rawBody.isNullOrBlank()) {
                val sixtySecondsAgo = System.currentTimeMillis() - 60_000L
                // Check 1: same raw body already in pending_notifications (recent)
                val recentDup = repository.findRecentByRawBody(rawBody, sixtySecondsAgo)
                if (recentDup != null) return@launch

                // Check 2: same raw body already saved as an expense (user already confirmed it)
                // Include NOTIFICATION_AUTO so live-notification-confirmed expenses also block re-detection.
                val bodyHash = rawBody.hashCode()
                val alreadySaved = expenseRepository.getExpensesSnapshot()
                    .any { (it.sourceType == SourceType.SMS_AUTO || it.sourceType == SourceType.NOTIFICATION_AUTO) &&
                        it.rawSmsBody?.trim()?.hashCode() == bodyHash }
                if (alreadySaved) return@launch
            }

            // ── Soft-dupe check: same amount + same merchant + same calendar day ──
            val now = System.currentTimeMillis()
            val isPossibleDuplicate = transaction.merchant != null &&
                expenseRepository.getExpensesSnapshot().any { expense ->
                    !expense.isDeleted &&
                    expense.amount == transaction.amount &&
                    expense.merchantName?.trim()?.lowercase() == transaction.merchant.trim().lowercase() &&
                    isSameCalendarDay(expense.date.toEpochMilliseconds(), now)
                }
            // Update the banner with the duplicate flag (banner may still be showing)
            if (isPossibleDuplicate && _pending.value == transaction) {
                _pending.value = transaction.copy(isPossibleDuplicate = true)
            }

            val savedId = repository.save(
                PendingNotification(
                    amount = transaction.amount,
                    currencyCode = transaction.currencyCode,
                    merchantName = transaction.merchant,
                    bankName = transaction.bankName,
                    accountLast4 = transaction.accountLast4,
                    transactionType = transaction.type.name,
                    detectedAtMillis = now,
                    rawBody = transaction.rawBody,
                    paymentMethod = transaction.paymentMethodName,
                    isPossibleDuplicate = isPossibleDuplicate
                )
            )
            _lastPendingId.value = savedId
            // Post system tray notification after save so pendingId is available in the tap intent
            TransactionAlertNotification.post(context, transaction, savedId)
        }
    }

    fun consume(): ParsedTransaction? {
        val tx = _pending.value
        _pending.value = null
        return tx
    }

    fun dismiss() {
        _pending.value = null
    }

    private fun isSameCalendarDay(millis1: Long, millis2: Long): Boolean {
        val c1 = Calendar.getInstance(); c1.timeInMillis = millis1
        val c2 = Calendar.getInstance(); c2.timeInMillis = millis2
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
