package com.expenseanalyst.feature.notification.service

import android.content.Context
import com.expenseanalyst.domain.model.PendingNotification
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.domain.util.BillSettlement
import com.expenseanalyst.feature.notification.parser.ParsedBillStatement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes a parsed bill statement into the pending inbox. Three cases, checked in order, all
 * keyed on biller + amount within [REMINDER_WINDOW_MILLIS] — billers re-send the same bill as
 * reminders ("kindly remind you…", "will be suspended in 48 hours…") with the amount unchanged:
 *
 * 1. A bill for it is already SAVED → it is a reminder. It goes on a BILL_REMINDER card that asks
 *    the user to link it to that bill (owner's choice: ask, don't auto-link). Further reminders
 *    stack on the same card instead of adding a prompt each.
 * 2. The statement is still WAITING in the inbox → count it on that card; the count moves onto
 *    the bill when the user saves it. (Previously dropped silently.)
 * 3. Otherwise → a new BILL card. If an open bill exists for the biller **in the same billing
 *    cycle** (BillSettlement.isSameCycle — due dates within a week), its id is stored in
 *    [linkedBillId] so the inbox can offer "Update Bill" instead of "Add as New Bill". Next
 *    month's statement is always a new bill, even while last month's still looks open.
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
            val amount = statement.totalDue ?: 0.0
            val since = System.currentTimeMillis() - REMINDER_WINDOW_MILLIS

            // 1. Reminder of a saved bill → the "link it?" card for that bill
            val savedBill = billRepository.findRecentBillByBillerAndAmount(statement.billerName, amount, since)
            if (savedBill != null) {
                val card = pendingRepository.findReminderCardForBill(savedBill.id)
                val cardId = if (card != null) {
                    pendingRepository.addReminder(card.id)
                    card.id
                } else {
                    pendingRepository.save(
                        PendingNotification(
                            amount = amount,
                            currencyCode = statement.currencyCode,
                            merchantName = statement.billerName,
                            bankName = statement.billerName,
                            accountLast4 = null,
                            transactionType = "BILL",
                            detectedAtMillis = System.currentTimeMillis(),
                            rawBody = statement.rawBody,
                            pendingType = "BILL_REMINDER",
                            billerName = statement.billerName,
                            dueDateMillis = statement.dueDateMillis ?: savedBill.dueDateMillis,
                            linkedBillId = savedBill.id,
                            reminderCount = 1
                        )
                    )
                }
                TransactionAlertNotification.postForBill(
                    context = context,
                    pendingId = cardId,
                    billerName = statement.billerName,
                    amount = amount,
                    currencyCode = statement.currencyCode,
                    reminderCount = (card?.reminderCount ?: 0) + 1
                )
                return@launch
            }

            // 2. Reminder of a statement still waiting in the inbox → count it there. Utilities
            // re-send the same bill every few days, which is how 6 real Saudi Energy bills once
            // became 17 rows.
            val alreadyQueued = pendingRepository.findRecentBillByBillerAndAmount(
                billerName = statement.billerName,
                amount = amount,
                sinceMillis = since
            )
            if (alreadyQueued != null) {
                pendingRepository.addReminder(alreadyQueued.id)
                return@launch
            }

            val existing = billRepository.findOpenBillByBiller(
                billerName = statement.billerName,
                accountId = null
            )?.takeIf { BillSettlement.isSameCycle(it.dueDateMillis, statement.dueDateMillis) }
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
