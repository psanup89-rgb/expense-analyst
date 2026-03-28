package com.expenseanalyst.feature.notification.service

import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.feature.notification.parser.ParsedBillStatement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes a parsed bill statement: creates a new PENDING Bill or updates
 * an existing open bill with fresh statement data (total due, due date, etc.).
 */
@Singleton
class BillStatementManager @Inject constructor(
    private val billRepository: BillRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun process(statement: ParsedBillStatement) {
        scope.launch {
            val existing = billRepository.findOpenBillByBiller(
                billerName = statement.billerName,
                accountId = null
            )
            if (existing == null) {
                billRepository.saveBill(
                    Bill(
                        billerName = statement.billerName,
                        accountId = null,
                        totalDue = statement.totalDue,
                        minimumDue = statement.minimumDue,
                        currencyCode = statement.currencyCode,
                        dueDateMillis = statement.dueDateMillis,
                        statementPeriodStart = statement.statementPeriodStart,
                        statementPeriodEnd = statement.statementPeriodEnd,
                        status = BillStatus.PENDING,
                        sourceType = SourceType.SMS_AUTO,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            } else {
                // Refresh existing open bill with latest statement data
                billRepository.updateBill(
                    existing.copy(
                        totalDue = statement.totalDue ?: existing.totalDue,
                        minimumDue = statement.minimumDue ?: existing.minimumDue,
                        dueDateMillis = statement.dueDateMillis ?: existing.dueDateMillis,
                        statementPeriodStart = statement.statementPeriodStart ?: existing.statementPeriodStart,
                        statementPeriodEnd = statement.statementPeriodEnd ?: existing.statementPeriodEnd
                    )
                )
            }
        }
    }
}
