package com.expenseanalyst.feature.notification.service

import android.content.Context
import com.expenseanalyst.domain.model.PendingNotification
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
    private val repository: PendingNotificationRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _pending = MutableStateFlow<ParsedTransaction?>(null)
    val pending: StateFlow<ParsedTransaction?> = _pending.asStateFlow()

    /** ID of the most recently saved pending notification. Used by the in-app banner. */
    private val _lastPendingId = MutableStateFlow<Long?>(null)
    val lastPendingId: StateFlow<Long?> = _lastPendingId.asStateFlow()

    fun enqueue(transaction: ParsedTransaction) {
        _pending.value = transaction
        scope.launch {
            val savedId = repository.save(
                PendingNotification(
                    amount = transaction.amount,
                    currencyCode = transaction.currencyCode,
                    merchantName = transaction.merchant,
                    bankName = transaction.bankName,
                    accountLast4 = transaction.accountLast4,
                    transactionType = transaction.type.name,
                    detectedAtMillis = System.currentTimeMillis(),
                    rawBody = transaction.rawBody,
                    paymentMethod = transaction.paymentMethodName
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
}
