package com.expenseanalyst.feature.notification.service

import android.content.Context
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.AccountRepository
import com.expenseanalyst.domain.repository.AppPreferencesRepository
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.domain.util.BillMatcher
import com.expenseanalyst.domain.util.CategoryInference
import com.expenseanalyst.domain.util.CurrencyConversion
import com.expenseanalyst.domain.util.NeedsReviewEvaluator
import com.expenseanalyst.feature.notification.parser.ParsedTransaction
import com.expenseanalyst.feature.notification.parser.TransactionDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class AutoSavedEvent(
    val expenseId: Long,
    val amount: Double,
    val currencyCode: String,
    val merchant: String?,
    val needsReview: Boolean
)

/**
 * Handles detected transactions by auto-saving them directly as expenses.
 * The UI observes [lastAutoSaved] for the in-app confirmation banner.
 * BILL type pending notifications still go through the old pending inbox path.
 */
@Singleton
class PendingNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingNotificationRepository: PendingNotificationRepository,
    private val expenseRepository: ExpenseRepository,
    private val billRepository: BillRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _lastAutoSaved = MutableStateFlow<AutoSavedEvent?>(null)
    val lastAutoSaved: StateFlow<AutoSavedEvent?> = _lastAutoSaved.asStateFlow()

    fun enqueue(transaction: ParsedTransaction) {
        // Normalize: PAYMENT-type SMS without merchant defaults to "BillPayments"
        val normalized = if (transaction.type == TransactionDirection.PAYMENT &&
            transaction.merchant.isNullOrBlank()
        ) {
            transaction.copy(merchant = DEFAULT_PAYMENT_MERCHANT)
        } else {
            transaction
        }

        scope.launch {
            // ── Dedup: skip if same SMS body already saved as an expense ──
            val rawBody = normalized.rawBody?.trim()
            if (!rawBody.isNullOrBlank()) {
                val bodyHash = rawBody.hashCode()
                val alreadySaved = expenseRepository.getExpensesSnapshot()
                    .any {
                        (it.sourceType == SourceType.SMS_AUTO || it.sourceType == SourceType.NOTIFICATION_AUTO) &&
                            it.rawSmsBody?.trim()?.hashCode() == bodyHash
                    }
                if (alreadySaved) return@launch
            }

            // ── Soft-dupe check: same amount + merchant + calendar day ──
            val now = System.currentTimeMillis()
            val isDuplicate = normalized.merchant != null &&
                expenseRepository.getExpensesSnapshot().any { expense ->
                    !expense.isDeleted &&
                        expense.amount == normalized.amount &&
                        expense.merchantName?.trim()?.lowercase() == normalized.merchant.trim().lowercase() &&
                        isSameCalendarDay(expense.date.toEpochMilliseconds(), now)
                }
            if (isDuplicate) return@launch

            // ── Auto-link bills for PAYMENT type ──
            val linkedBillId: Long? = if (normalized.type == TransactionDirection.PAYMENT) {
                val openBills = billRepository.getBills().first()
                    .filter { !it.isDeleted && it.status != BillStatus.SETTLED }
                BillMatcher.findMatchingOpenBill(
                    payment = normalized.amount,
                    merchant = normalized.merchant,
                    openBills = openBills
                )?.id
            } else null

            // ── Resolve category ──
            val categories = categoryRepository.getCategories().first()
            val fallbackCategory = categories.find { it.name == "Misc" }
                ?: categories.find { it.name == "Other" }
                ?: categories.last()
            val merchantRules = merchantRuleRepository.getRules().first()
            val merchantName = normalized.merchant?.takeIf { it.isNotBlank() }
                ?: normalized.bankName.takeIf { it != "Unknown Bank" }
                ?: normalized.bankName
            val category = CategoryInference.infer(
                merchantName, normalized.bankName, categories,
                smsBody = normalized.rawBody, merchantRules = merchantRules
            ) ?: fallbackCategory

            // ── Resolve payment method ──
            val paymentMethod = normalized.paymentMethodName?.let { name ->
                runCatching { PaymentMethod.valueOf(name) }.getOrNull()
            } ?: PaymentMethod.OTHER

            // ── Resolve account ──
            val inferredAccountType = when {
                normalized.rawBody?.contains("credit card", ignoreCase = true) == true ||
                    normalized.rawBody?.contains(" CC ", ignoreCase = true) == true -> AccountType.CREDIT_CARD
                normalized.rawBody?.contains("debit card", ignoreCase = true) == true ||
                    normalized.rawBody?.contains(" DC ", ignoreCase = true) == true -> AccountType.DEBIT_CARD
                normalized.rawBody?.contains("wallet", ignoreCase = true) == true ||
                    normalized.rawBody?.contains("stc pay", ignoreCase = true) == true -> AccountType.WALLET
                else -> AccountType.SAVINGS
            }
            val resolvedAccountId = runCatching {
                accountRepository.findOrCreate(
                    bankName = normalized.bankName,
                    lastFour = normalized.accountLast4,
                    accountType = inferredAccountType
                )
            }.getOrNull()

            // ── Map transaction type ──
            val transactionType = when (normalized.type) {
                TransactionDirection.CREDIT -> TransactionType.INCOME
                TransactionDirection.DEBIT -> TransactionType.EXPENSE
                TransactionDirection.PAYMENT -> TransactionType.PAYMENT
                TransactionDirection.TRANSFER -> TransactionType.TRANSFER
            }

            // ── Currency conversion ──
            val homeCurrencyCode = currencyRepository.getHomeCurrency().first()
            val ratesByCode = runCatching {
                currencyRepository.getRates().first().associateBy { it.currencyCode }
            }.getOrElse { emptyMap() }

            // ── Compute needsReview ──
            val reviewReasons = NeedsReviewEvaluator.evaluate(
                merchantName = normalized.merchant,
                categoryName = category.name,
                paymentMethod = paymentMethod,
                accountLastFour = normalized.accountLast4
            )
            val needsReview = reviewReasons.isNotEmpty()

            val stubExpense = Expense(
                amount = normalized.amount,
                currencyCode = normalized.currencyCode,
                homeAmount = null,
                exchangeRate = null,
                description = "",
                category = category,
                paymentMethod = paymentMethod,
                transactionType = transactionType,
                date = Instant.fromEpochMilliseconds(now),
                merchantName = merchantName,
                sourceType = SourceType.NOTIFICATION_AUTO,
                sourceSender = null,
                accountId = resolvedAccountId,
                rawSmsBody = normalized.rawBody,
                billId = linkedBillId,
                needsReview = needsReview,
                reviewReasons = reviewReasons
            )
            val conversion = CurrencyConversion.resolve(stubExpense, homeCurrencyCode, ratesByCode)
            val savedExpense = stubExpense.copy(
                homeAmount = conversion.homeAmount,
                exchangeRate = conversion.exchangeRate
            )
            val savedId = expenseRepository.addExpense(savedExpense)

            _lastAutoSaved.value = AutoSavedEvent(
                expenseId = savedId,
                amount = normalized.amount,
                currencyCode = normalized.currencyCode,
                merchant = merchantName,
                needsReview = needsReview
            )
            TransactionAlertNotification.postForExpense(context, normalized, savedId)
        }
    }

    fun consume() {
        _lastAutoSaved.value = null
    }

    fun dismiss() {
        _lastAutoSaved.value = null
    }

    companion object {
        const val DEFAULT_PAYMENT_MERCHANT = "BillPayments"
    }

    private fun isSameCalendarDay(millis1: Long, millis2: Long): Boolean {
        val c1 = Calendar.getInstance(); c1.timeInMillis = millis1
        val c2 = Calendar.getInstance(); c2.timeInMillis = millis2
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
