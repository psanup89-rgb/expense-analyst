package com.expenseanalyst.feature.notification.service

import com.expenseanalyst.domain.model.PendingNotification
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.feature.notification.parser.ParsedBillStatement
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
    private val billRepository: BillRepository,
    private val pendingRepository: PendingNotificationRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun process(statement: ParsedBillStatement) {
        scope.launch {
            val existing = billRepository.findOpenBillByBiller(
                billerName = statement.billerName,
                accountId = null
            )
            pendingRepository.save(
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
        }
    }
}
