package com.expenseanalyst.feature.notification.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.AccountRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.util.CategoryInference
import com.expenseanalyst.domain.util.CurrencyConversion
import com.expenseanalyst.feature.notification.parser.ParsedTransaction
import com.expenseanalyst.feature.notification.parser.ParserRegistry
import com.expenseanalyst.feature.notification.parser.TransactionDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class SmsImportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val accountRepository: AccountRepository,
    private val merchantRuleRepository: MerchantRuleRepository
) : ViewModel() {

    /** Non-null when launched from onboarding with a pre-selected import range. */
    val autoStart: String? = savedStateHandle["autoStart"]

    private val _uiState = MutableStateFlow<SmsImportUiState>(SmsImportUiState.ChooseMode)
    val uiState: StateFlow<SmsImportUiState> = _uiState.asStateFlow()

    /** Saved so we know what to run once permission is granted. */
    private var pendingAction: ImportAction = ImportAction.BROWSE

    private val financialKeywords = Regex(
        """(?i)\b(?:debited|credited|deducted|paid|received|txn|transaction|rs\.?|inr|sar|aed|usd|₹|balance|card|account|purchase|payment|withdrawn|sent|transfer)\b"""
    )

    // ── Permission callbacks ──────────────────────────────────────────────────

    fun setPendingAction(action: ImportAction) {
        pendingAction = action
    }

    fun onPermissionGranted() = execute(pendingAction)

    fun onPermissionDenied() {
        _uiState.value = SmsImportUiState.PermissionDenied
    }

    /** Called by the screen to execute an action when permission is already granted. */
    fun execute(action: ImportAction) {
        when (action) {
            ImportAction.BULK_LAST_MONTH -> startBulkImport(lastMonthOnly = true)
            ImportAction.BULK_ALL -> startBulkImport(lastMonthOnly = false)
            ImportAction.BROWSE -> loadSmsInbox()
        }
    }

    // ── Bulk import ───────────────────────────────────────────────────────────

    fun startBulkImport(lastMonthOnly: Boolean) {
        viewModelScope.launch {
            _uiState.value = SmsImportUiState.BulkImporting(processed = 0, total = 0)

            // Load categories; find "Misc" as fallback
            val categories = categoryRepository.getCategories().first()
            val miscCategory = categories.find { it.name == "Misc" }
                ?: categories.find { it.name == "Other" }
                ?: categories.last()

            // Load user-defined merchant rules for intelligent categorization
            val merchantRules = merchantRuleRepository.getRules().first()

            // Load home currency and rates for conversion
            val homeCurrencyCode = currencyRepository.getHomeCurrency().first()
            val ratesByCode = runCatching {
                if (currencyRepository.isStale()) currencyRepository.refreshRates()
                currencyRepository.getRates().first().associateBy { it.currencyCode }
            }.getOrElse { emptyMap() }

            // Existing expenses used for in-memory duplicate check (amount + day)
            val existingKeys = expenseRepository.getExpensesSnapshot()
                .filter { it.sourceType == SourceType.SMS_AUTO }
                .mapTo(mutableSetOf()) { dedupeKey(it.amount, it.date.toEpochMilliseconds()) }

            // Read SMS
            val sinceMillis = if (lastMonthOnly) {
                System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            } else null

            val smsList = withContext(Dispatchers.IO) { querySmsInbox(sinceMillis = sinceMillis) }
            _uiState.value = SmsImportUiState.BulkImporting(processed = 0, total = smsList.size)

            val toSave = mutableListOf<Expense>()
            var skipped = 0
            var failed = 0
            // Cache accountId lookups to avoid redundant DB calls within the same import
            val accountCache = mutableMapOf<Pair<String, String?>, Long>()

            smsList.forEachIndexed { index, sms ->
                val parsed = ParserRegistry.parse(sms.sender, sms.body)
                if (parsed == null) {
                    failed++
                    _uiState.value = SmsImportUiState.BulkImporting(
                        processed = index + 1, total = smsList.size
                    )
                    return@forEachIndexed
                }

                // Dedup: same amount on same calendar day
                val key = dedupeKey(parsed.amount, sms.timestampMs)
                if (key in existingKeys) {
                    skipped++
                    _uiState.value = SmsImportUiState.BulkImporting(
                        processed = index + 1, total = smsList.size
                    )
                    return@forEachIndexed
                }
                existingKeys.add(key)

                val transactionType = when (parsed.type) {
                    TransactionDirection.CREDIT -> TransactionType.INCOME
                    TransactionDirection.DEBIT -> TransactionType.EXPENSE
                    TransactionDirection.PAYMENT -> TransactionType.PAYMENT
                }

                // Resolve account: find or create by bank name + last4
                val bankDisplay = if (parsed.bankName != "Unknown Bank") parsed.bankName
                    else bankDisplayNameFromSender(sms.sender) ?: parsed.bankName

                // Merchant name is the primary identifier; fall back to bank display name
                val merchantName = parsed.merchant?.takeIf { it.isNotBlank() } ?: bankDisplay

                // Category inference — uses merchantName which is now always populated
                val category = CategoryInference.infer(
                    merchantName, parsed.bankName, categories,
                    smsBody = sms.body, merchantRules = merchantRules
                ) ?: miscCategory
                val inferredAccountType = when {
                    sms.body.contains("credit card", ignoreCase = true) ||
                        sms.body.contains(" CC ", ignoreCase = true) ||
                        sms.body.contains("CC No", ignoreCase = true) ||
                        sms.body.contains("credit limit", ignoreCase = true) -> AccountType.CREDIT_CARD
                    sms.body.contains("fx card", ignoreCase = true) ||
                        sms.body.contains("forex card", ignoreCase = true) ||
                        sms.body.contains("prepaid card", ignoreCase = true) -> AccountType.FOREX_CARD
                    sms.body.contains("debit card", ignoreCase = true) ||
                        sms.body.contains(" DC ", ignoreCase = true) -> AccountType.DEBIT_CARD
                    sms.body.contains("wallet", ignoreCase = true) ||
                        sms.body.contains("stc pay", ignoreCase = true) ||
                        sms.body.contains("paytm", ignoreCase = true) ||
                        sms.body.contains("phonepe", ignoreCase = true) ||
                        sms.body.contains("gpay", ignoreCase = true) -> AccountType.WALLET
                    sms.body.contains("current account", ignoreCase = true) -> AccountType.CURRENT
                    else -> AccountType.SAVINGS
                }
                val accountCacheKey = bankDisplay to parsed.accountLast4
                val resolvedAccountId = accountCache[accountCacheKey] ?: run {
                    val id = accountRepository.findOrCreate(
                        bankName = bankDisplay,
                        lastFour = parsed.accountLast4,
                        accountType = inferredAccountType
                    )
                    accountCache[accountCacheKey] = id
                    id
                }

                // Build a stub expense to run CurrencyConversion.resolve()
                val stubExpense = Expense(
                    amount = parsed.amount,
                    currencyCode = parsed.currencyCode,
                    homeAmount = null,
                    exchangeRate = null,
                    description = "",
                    category = category,
                    paymentMethod = PaymentMethod.OTHER,
                    transactionType = transactionType,
                    date = Instant.fromEpochMilliseconds(sms.timestampMs),
                    merchantName = merchantName,
                    sourceType = SourceType.SMS_AUTO,
                    sourceSender = sms.sender,
                    accountId = resolvedAccountId,
                    rawSmsBody = sms.body
                )
                val conversion = CurrencyConversion.resolve(stubExpense, homeCurrencyCode, ratesByCode)

                toSave.add(
                    stubExpense.copy(
                        homeAmount = conversion.homeAmount,
                        exchangeRate = conversion.exchangeRate
                    )
                )
                _uiState.value = SmsImportUiState.BulkImporting(
                    processed = index + 1, total = smsList.size
                )
            }

            withContext(Dispatchers.IO) { expenseRepository.addExpenses(toSave) }

            _uiState.value = SmsImportUiState.BulkImportDone(
                totalScanned = smsList.size,
                saved = toSave.size,
                skipped = skipped,
                failed = failed
            )
        }
    }

    // ── Single-message browse mode ────────────────────────────────────────────

    fun onMessageSelected(message: SmsMessage) {
        _uiState.value = SmsImportUiState.Parsing
        viewModelScope.launch(Dispatchers.Default) {
            val parsed = ParserRegistry.parse(message.sender, message.body)
            _uiState.value = if (parsed != null) {
                SmsImportUiState.ParseSuccess(parsed, message.body)
            } else {
                SmsImportUiState.ParseFailed(message.body)
            }
        }
    }

    fun onAddAsExpense(parsed: ParsedTransaction) {
        _uiState.value = SmsImportUiState.NavigateToAddExpense(parsed)
    }

    fun onNavigationHandled() {
        _uiState.value = SmsImportUiState.ChooseMode
    }

    fun onBack() {
        _uiState.value = when (_uiState.value) {
            is SmsImportUiState.ParseSuccess,
            is SmsImportUiState.ParseFailed -> SmsImportUiState.LoadingSms.also { loadSmsInbox() }
            is SmsImportUiState.SmsList -> SmsImportUiState.ChooseMode
            is SmsImportUiState.BulkImportDone -> SmsImportUiState.ChooseMode
            else -> SmsImportUiState.ChooseMode
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadSmsInbox() {
        _uiState.value = SmsImportUiState.LoadingSms
        val sinceMillis = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        viewModelScope.launch(Dispatchers.IO) {
            val messages = querySmsInbox(sinceMillis = sinceMillis, maxResults = 50)
            _uiState.value = SmsImportUiState.SmsList(messages)
        }
    }

    private fun querySmsInbox(sinceMillis: Long?, maxResults: Int? = null): List<SmsMessage> {
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val selection = if (sinceMillis != null) "date > ?" else null
        val selectionArgs = if (sinceMillis != null) arrayOf(sinceMillis.toString()) else null
        val sortOrder = if (maxResults != null) "date DESC LIMIT $maxResults" else "date DESC"
        return buildList {
            runCatching {
                context.contentResolver.query(
                    uri, projection, selection, selectionArgs, sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow("_id")
                    val addrCol = cursor.getColumnIndexOrThrow("address")
                    val bodyCol = cursor.getColumnIndexOrThrow("body")
                    val dateCol = cursor.getColumnIndexOrThrow("date")
                    while (cursor.moveToNext()) {
                        val body = cursor.getString(bodyCol) ?: continue
                        if (!isFinancialSms(body)) continue
                        add(
                            SmsMessage(
                                id = cursor.getLong(idCol),
                                sender = cursor.getString(addrCol) ?: "",
                                body = body,
                                timestampMs = cursor.getLong(dateCol)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun isFinancialSms(body: String) = financialKeywords.containsMatchIn(body)

    /** Maps a raw SMS sender ID to a human-readable bank name, or null if unknown. */
    private fun bankDisplayNameFromSender(sender: String): String? {
        val s = sender.uppercase()
        return when {
            "HDFC" in s -> "HDFC Bank"
            "ICICI" in s -> "ICICI Bank"
            "SBI" in s -> "SBI"
            "AXIS" in s -> "Axis Bank"
            "KOTAK" in s -> "Kotak Bank"
            "YESBNK" in s || "YESBANK" in s -> "Yes Bank"
            "INDUS" in s -> "IndusInd Bank"
            "PNBSMS" in s || "PUNJAB" in s -> "PNB"
            "ALRJHI" in s || "ALRAJHI" in s -> "Al Rajhi Bank"
            "ALINMA" in s -> "Alinma Bank"
            "STCBNK" in s || "STCPAY" in s -> "STC Bank"
            "D360" in s -> "Bank D·360"
            else -> null
        }
    }

    /** Key for duplicate detection: amount (rounded to 2 dp) + calendar day (UTC). */
    private fun dedupeKey(amount: Double, epochMillis: Long): String {
        val roundedAmount = "%.2f".format(amount)
        val dayBucket = epochMillis / 86_400_000
        return "$roundedAmount:$dayBucket"
    }
}
