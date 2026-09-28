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
import com.expenseanalyst.domain.repository.TransferRecipientRuleRepository
import com.expenseanalyst.domain.util.BillMatcher
import com.expenseanalyst.domain.util.CategoryInference
import com.expenseanalyst.domain.util.CurrencyConversion
import com.expenseanalyst.domain.util.MerchantRuleMatcher
import com.expenseanalyst.domain.util.NeedsReviewEvaluator
import com.expenseanalyst.domain.util.PaymentMethodInference
import com.expenseanalyst.domain.util.ReviewReason
import com.expenseanalyst.domain.util.ReimbursementMatcher
import com.expenseanalyst.domain.util.RefundMatcher
import com.expenseanalyst.domain.util.TransferRecipientMatcher
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
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.domain.util.SpendClassifier
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    private val transferRecipientRuleRepository: TransferRecipientRuleRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val bnplReconciler: BnplReconciler
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _lastAutoSaved = MutableStateFlow<AutoSavedEvent?>(null)
    val lastAutoSaved: StateFlow<AutoSavedEvent?> = _lastAutoSaved.asStateFlow()

    fun enqueue(transaction: ParsedTransaction) {
        // BNPL split confirmations (Tabby/Tamara) go through a dedicated path that bypasses
        // the normal auto-save flow entirely — including the amount+merchant+day dedup below,
        // which would otherwise treat this as a duplicate of the merchant's own full-amount
        // SMS instead of reclassifying it. See TabbyTamaraParser's KDoc for why this exists.
        if (transaction.isBnplConfirmation) {
            scope.launch { handleBnplConfirmation(transaction) }
            return
        }

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
                smsBody = normalized.rawBody, merchantRules = merchantRules,
                upiDebit = normalized.type == TransactionDirection.DEBIT && normalized.paymentMethodName == "UPI"
            ) ?: fallbackCategory
            val matchedRule = MerchantRuleMatcher.findMatch(merchantName, merchantRules)

            // ── Resolve payment method ──
            val parsedPaymentMethod = normalized.paymentMethodName?.let { name ->
                runCatching { PaymentMethod.valueOf(name) }.getOrNull()
            }

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
            // A message that names only the network ("By:1234 ;Visa") leaves the method unsaid;
            // the matched account's type answers it (PaymentMethodInference).
            val paymentMethod = parsedPaymentMethod
                ?: resolvedAccountId?.let { id ->
                    PaymentMethodInference.fromAccountType(
                        runCatching { accountRepository.getAccountById(id).first()?.accountType }.getOrNull()
                    )
                }
                ?: PaymentMethod.OTHER

            // ── Map transaction type ──
            val transferRules = transferRecipientRuleRepository.getRules().first()
            val parsedType = when (normalized.type) {
                TransactionDirection.CREDIT -> TransactionType.INCOME
                TransactionDirection.DEBIT -> TransactionType.EXPENSE
                TransactionDirection.PAYMENT -> TransactionType.PAYMENT
                TransactionDirection.TRANSFER -> TransactionType.TRANSFER
            }
            // A credit from the user's own name (a remembered OWN_ACCOUNT rule) is money moved
            // between their accounts, not income — it becomes an own-account transfer below.
            val transactionType =
                if (parsedType == TransactionType.INCOME && TransferRecipientMatcher.isFromOwnAccount(normalized.merchant, transferRules)) {
                    TransactionType.TRANSFER
                } else {
                    parsedType
                }

            // ── Transfer classification from the remembered recipient rule ──
            // Keyed on the recipient NAME, not the account: for an outgoing transfer the row's
            // accountId is the SOURCE (already the user's own account) and the destination
            // exists only as the merchant name. Forward-only by design — creating a rule never
            // touches rows already captured, so no historical total shifts retroactively.
            // EXTERNAL_IN is never produced here: it can only come from a rule the user set by
            // hand, because the parsers discard inbound/outbound direction for transfers.
            val transferClassification = if (transactionType == TransactionType.TRANSFER) {
                TransferRecipientMatcher.findRule(normalized.merchant, transferRules)?.classification
            } else {
                null
            }

            // ── Refund matching: for a Refund-category INCOME, find the original expense it
            // refunds (same amount+currency, within RefundMatcher's window) and inherit its
            // account/payment method instead of guessing from this SMS's own sparse wording ──
            val refundMatch = if (transactionType == TransactionType.INCOME && category.name == "Refund") {
                RefundMatcher.findMatch(
                    refundAmount = normalized.amount,
                    refundCurrencyCode = normalized.currencyCode,
                    refundDateMillis = now,
                    allExpenses = expenseRepository.getExpensesSnapshot()
                )
            } else null
            val effectivePaymentMethod = refundMatch?.paymentMethod ?: paymentMethod
            val effectiveAccountId = refundMatch?.accountId ?: resolvedAccountId

            // ── Currency conversion ──
            val homeCurrencyCode = currencyRepository.getHomeCurrency().first()
            val ratesByCode = runCatching {
                currencyRepository.getRates().first().associateBy { it.currencyCode }
            }.getOrElse { emptyMap() }

            // ── Compute needsReview ──
            val reviewReasons = NeedsReviewEvaluator.evaluate(
                merchantName = normalized.merchant,
                categoryName = category.name,
                paymentMethod = effectivePaymentMethod,
                accountLastFour = refundMatch?.accountLastFour ?: normalized.accountLast4,
                transactionType = transactionType,
                transferClassification = transferClassification,
                accountIdentified = (refundMatch?.accountLastFour ?: normalized.accountLast4) != null ||
                    isOnlyAccountOfBank(effectiveAccountId, normalized.bankName)
            ) + possibleReimbursementReason(transactionType, category.name, normalized, now)
            val needsReview = reviewReasons.isNotEmpty()

            val stubExpense = Expense(
                amount = normalized.amount,
                currencyCode = normalized.currencyCode,
                homeAmount = null,
                exchangeRate = null,
                description = "",
                category = category,
                paymentMethod = effectivePaymentMethod,
                transactionType = transactionType,
                date = Instant.fromEpochMilliseconds(now),
                merchantName = merchantName,
                sourceType = SourceType.NOTIFICATION_AUTO,
                sourceSender = null,
                accountId = effectiveAccountId,
                rawSmsBody = normalized.rawBody,
                billId = linkedBillId,
                needsReview = needsReview,
                reviewReasons = reviewReasons,
                tags = matchedRule?.tags ?: emptyList(),
                refundOriginalExpenseId = refundMatch?.id,
                transferClassification = transferClassification
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
            // A Tabby/Tamara card charge gets linked to its purchase (and renamed after the shop).
            runCatching { bnplReconciler.reconcile() }
            TransactionAlertNotification.postForExpense(
                context, normalized, savedId, category,
                needsReview = needsReview,
                monthToDate = monthToDateLine(homeCurrencyCode)
            )
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

    /**
     * BNPL split confirmation (Tabby/Tamara), per the owner's decision: these purchases never
     * count toward "spent this month," permanently — not deferred, unlike the existing EMI
     * feature. Two outcomes, both landing as `PAYMENT` under "Split Payments" (structurally
     * excluded from every total already, per [TransactionType.PAYMENT]'s existing behaviour):
     *
     * 1. The merchant's own full-amount SMS was already captured as a normal `EXPENSE` — that
     *    row is reclassified in place via a targeted update, never [ExpenseRepository.updateExpense]
     *    (which would null account_number and rewrite the tag join table).
     * 2. No matching expense exists (merchant SMS missed, or outside the same-day window) — a
     *    new expense is created directly from this SMS.
     *
     * Guarded against redelivery of the identical confirmation SMS: if a matching `PAYMENT` row
     * already exists (case 1 or 2 already ran), this returns without touching anything.
     */
    private suspend fun handleBnplConfirmation(parsed: ParsedTransaction) {
        val now = System.currentTimeMillis()
        val merchantLower = parsed.merchant?.trim()?.lowercase()
        val snapshot = expenseRepository.getExpensesSnapshot()

        fun sameAmountMerchantDay(expense: Expense) =
            !expense.isDeleted &&
                expense.amount == parsed.amount &&
                merchantLower != null &&
                expense.merchantName?.trim()?.lowercase() == merchantLower &&
                isSameCalendarDay(expense.date.toEpochMilliseconds(), now)

        val alreadyHandled = snapshot.any {
            it.transactionType == TransactionType.PAYMENT && sameAmountMerchantDay(it)
        }
        if (alreadyHandled) return

        val categories = categoryRepository.getCategories().first()
        val splitPaymentsCategory = categories.find { it.name == "Split Payments" }
            ?: categories.find { it.name == "Other" }
            ?: categories.last()

        val matched = snapshot.firstOrNull {
            it.transactionType == TransactionType.EXPENSE && sameAmountMerchantDay(it)
        }

        if (matched != null) {
            expenseRepository.reclassifyAsSplitPayment(matched.id, splitPaymentsCategory.id)
            return
        }

        val homeCurrencyCode = currencyRepository.getHomeCurrency().first()
        val ratesByCode = runCatching {
            currencyRepository.getRates().first().associateBy { it.currencyCode }
        }.getOrElse { emptyMap() }

        val paymentMethod = parsed.paymentMethodName?.let { name ->
            runCatching { PaymentMethod.valueOf(name) }.getOrNull()
        } ?: PaymentMethod.CREDIT_CARD

        val stubExpense = Expense(
            amount = parsed.amount,
            currencyCode = parsed.currencyCode,
            homeAmount = null,
            exchangeRate = null,
            description = "",
            category = splitPaymentsCategory,
            paymentMethod = paymentMethod,
            transactionType = TransactionType.PAYMENT,
            date = Instant.fromEpochMilliseconds(now),
            merchantName = parsed.merchant?.takeIf { it.isNotBlank() } ?: parsed.bankName,
            sourceType = SourceType.NOTIFICATION_AUTO,
            rawSmsBody = parsed.rawBody
        )
        val conversion = CurrencyConversion.resolve(stubExpense, homeCurrencyCode, ratesByCode)
        expenseRepository.addExpense(
            stubExpense.copy(homeAmount = conversion.homeAmount, exchangeRate = conversion.exchangeRate)
        )
        // The first instalment often lands on the card seconds BEFORE this confirmation; link it now.
        runCatching { bnplReconciler.reconcile() }
    }

    /**
     * An incoming payment matching pending reimbursable expenses is flagged "Reimbursement?" so
     * the user can link it from Review — suggested, never linked automatically (owner's choice).
     */
    private suspend fun possibleReimbursementReason(
        type: TransactionType,
        categoryName: String,
        parsed: ParsedTransaction,
        atMillis: Long
    ): List<ReviewReason> {
        if (type != TransactionType.INCOME || categoryName == SpendClassifier.REFUND_CATEGORY) return emptyList()
        val match = ReimbursementMatcher.suggest(
            parsed.amount, parsed.currencyCode, atMillis, expenseRepository.getExpensesSnapshot()
        )
        return if (match != null) listOf(ReviewReason.POSSIBLE_REIMBURSEMENT) else emptyList()
    }

    /** A digit-less message still names its account when the bank has just one (STC). */
    private suspend fun isOnlyAccountOfBank(accountId: Long?, bankName: String): Boolean {
        if (accountId == null || bankName == "Unknown Bank") return false
        val ofBank = runCatching { accountRepository.getAccounts().first() }.getOrElse { return false }
            .filter { it.bankName == bankName }
        return ofBank.size == 1 && ofBank.single().id == accountId
    }

        private fun isSameCalendarDay(millis1: Long, millis2: Long): Boolean {
        val c1 = Calendar.getInstance(); c1.timeInMillis = millis1
        val c2 = Calendar.getInstance(); c2.timeInMillis = millis2
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * "September so far: SAR34,443.00 spent" for the expanded notification. Uses SpendClassifier,
     * the same rule as the home screen's Spent card, so the two never disagree. Best-effort: any
     * failure just drops the line rather than the notification.
     */
    private suspend fun monthToDateLine(homeCurrencyCode: String): String? = runCatching {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val thisMonth = expenseRepository.getExpensesSnapshot().filter {
            val d = it.date.toLocalDateTime(tz).date
            d.year == today.year && d.month == today.month
        }
        val total = SpendClassifier.spendBreakdown(thisMonth).net
        val monthName = today.month.name.lowercase().replaceFirstChar { it.uppercase() }
        NotificationCopy.monthToDateLine(monthName, CurrencyFormatter.format(total, homeCurrencyCode))
    }.getOrNull()
}
