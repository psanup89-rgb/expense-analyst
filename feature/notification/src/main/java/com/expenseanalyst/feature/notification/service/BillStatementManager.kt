package com.expenseanalyst.feature.notification.service

import android.content.Context
import com.expenseanalyst.domain.model.PendingNotification
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.feature.notification.parser.ParsedBillStatement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes a parsed bill statement by enqueueing it to the pending inbox
 * as a BILL-type item. The user then confirms whether to create a new bill
 * or update an existing open bill for that biller.
 *
 * If an open (PENDING/PARTIAL) bill already exists for the biller, its id
 * is stored in [linkedBillId] so the inbox UI can offer "Update Bill" instead
 * of "Add as New Bill".
 */
@Singleton
class BillStatementManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billRepository: BillRepository,
    private val pendingRepository: PendingNotificationRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private companion object {
        /**
         * Slightly longer than a billing cycle: collapses the same statement re-sent over
         * weeks, while still letting next month's genuinely separate bill through even when
         * it happens to be for an identical amount.
         */
        const val REMINDER_WINDOW_MILLIS = 35L * 24 * 60 * 60 * 1000
    }

    fun process(statement: ParsedBillStatement) {
        scope.launch {
            // Re-sent reminder for a statement already queued? Drop it. Utilities re-send the
            // same bill every few days, which is how 6 real Saudi Energy bills became 17 rows.
            val alreadyQueued = pendingRepository.findRecentBillByBillerAndAmount(
                billerName = statement.billerName,
                amount = statement.totalDue ?: 0.0,
                sinceMillis = System.currentTimeMillis() - REMINDER_WINDOW_MILLIS
            )
            if (alreadyQueued != null) return@launch

            val existing = billRepository.findOpenBillByBiller(
                billerName = statement.billerName,
                accountId = null
            )
            val pendingId = pendingRepository.save(
                PendingNotification(
                    amount = statement.totalDue ?: 0.0,
                    currencyCode = statement.currencyCode,
                    merchantName = statement.billerName,
                    bankName = statement.billerName,
                    accountLast4 = null,
                    transactionType = "BILL",
                    detectedAtMillis = System.currentTimeMillis(),
                    rawBody = statement.rawBody,
                    pendingType = "BILL",
                    billerName = statement.billerName,
                    dueDateMillis = statement.dueDateMillis,
                    linkedBillId = existing?.id
                )
            )
            // A bill is not auto-saved, so without this the detection is silent and the
            // Pending Bill Statements queue grows unseen.
            TransactionAlertNotification.postForBill(
                context = context,
                pendingId = pendingId,
                billerName = statement.billerName,
                amount = statement.totalDue ?: 0.0,
                currencyCode = statement.currencyCode
            )
        }
    }
}
