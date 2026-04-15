package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.core.util.DateTimeUtil
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.BillStatus
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.AccountRepository
import com.expenseanalyst.domain.repository.BillRepository
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.repository.PendingNotificationRepository
import com.expenseanalyst.domain.repository.TagRepository
import com.expenseanalyst.domain.usecase.AddExpenseUseCase
import com.expenseanalyst.domain.usecase.GetAccountsUseCase
import com.expenseanalyst.domain.usecase.GetCategoriesUseCase
import com.expenseanalyst.domain.usecase.InferCategoryUseCase
import com.expenseanalyst.domain.usecase.InferenceSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val pendingNotificationRepository: PendingNotificationRepository,
    private val billRepository: BillRepository,
    private val inferCategoryUseCase: InferCategoryUseCase,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val tagRepository: TagRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private var inferenceJob: Job? = null

    private val _form = MutableStateFlow(
        run {
            val amount = savedStateHandle.get<String>("amount")?.takeIf { it.isNotBlank() }
            val currency = savedStateHandle.get<String>("currency")?.takeIf { it.isNotBlank() }
            val merchant = savedStateHandle.get<String>("merchant")?.takeIf { it.isNotBlank() }
            val type = savedStateHandle.get<String>("type")
            AddExpenseUiState(
                date = DateTimeUtil.now(),
                amountInput = amount ?: "",
                currencyCode = currency ?: "INR",
                merchantName = merchant ?: "",
                description = "",
                transactionType = when (type) {
                    "CREDIT", "INCOME" -> TransactionType.INCOME
                    "TRANSFER" -> TransactionType.TRANSFER
                    "PAYMENT" -> TransactionType.PAYMENT
                    else -> TransactionType.EXPENSE
                }
            )
        }
    )

    private val homeCurrencyFlow: Flow<String> = currencyRepository.getHomeCurrency().distinctUntilChanged()

    private val selectedCurrencyRateFlow = _form
        .map { it.currencyCode }
        .distinctUntilChanged()
        .flatMapLatest { code -> currencyRepository.getRate(code) }

    private val homeCurrencyRateFlow = homeCurrencyFlow
        .flatMapLatest { code -> currencyRepository.getRate(code) }

    init {
        viewModelScope.launch {
            if (currencyRepository.isStale()) currencyRepository.refreshRates()
        }
        // Apply payment method from nav arg (notification tap path)
        val paymentMethodArg = savedStateHandle.get<String>("paymentMethod")?.takeIf { it.isNotBlank() }
        if (paymentMethodArg != null) {
            val pm = PaymentMethod.entries.find { it.name == paymentMethodArg }
            if (pm != null) _form.update { it.copy(paymentMethod = pm) }
        }

        val accountNavArg = savedStateHandle.get<String>("account")?.takeIf { it.isNotBlank() }
        if (accountNavArg != null) {
            viewModelScope.launch {
                val allAccounts = getAccountsUseCase().first()
                val last4 = Regex("""\*(\d{4})""").find(accountNavArg)?.groupValues?.get(1)
                val bankName = accountNavArg.substringBefore("*").trim().ifEmpty { null }
                // When both bankName and last4 are known, require both to match.
                // Matching on last4 alone is too broad — different banks can share card digits.
                val matched = allAccounts.find { account ->
                    val bankMatches = bankName != null &&
                        account.bankName.contains(bankName, ignoreCase = true)
                    val last4Matches = last4 != null && account.lastFour == last4
                    when {
                        bankName != null && last4 != null -> bankMatches && last4Matches
                        bankName != null -> bankMatches
                        else -> last4Matches
                    }
                }
                val isWalletPayment = paymentMethodArg in listOf("APPLE_PAY", "SAMSUNG_PAY", "GOOGLE_PAY")
                val inferredType = if (isWalletPayment) AccountType.CREDIT_CARD else AccountType.OTHER

                if (matched != null) {
                    // If the account type is less specific (e.g. Savings/Other) but we now know
                    // it's a credit card (wallet payment), upgrade the type in-place.
                    if (isWalletPayment && matched.accountType != AccountType.CREDIT_CARD) {
                        val displayName = if (matched.lastFour != null)
                            "${matched.bankName} *${matched.lastFour} · ${AccountType.CREDIT_CARD.label}"
                        else "${matched.bankName} · ${AccountType.CREDIT_CARD.label}"
                        accountRepository.updateAccount(
                            matched.copy(accountType = AccountType.CREDIT_CARD, displayName = displayName)
                        )
                    }
                    _form.update { it.copy(selectedAccountId = matched.id) }
                } else if (bankName != null) {
                    val newId = accountRepository.findOrCreate(
                        bankName = bankName,
                        lastFour = last4,
                        accountType = inferredType
                    )
                    _form.update { it.copy(selectedAccountId = newId) }
                }
            }
        }

        // For PAYMENT type: load open bills for picker and attempt auto-link by merchant name
        if (_form.value.transactionType == TransactionType.PAYMENT) {
            viewModelScope.launch { loadBillsForLinking() }
        }

        // Load rawSmsBody and paymentMethod from pending notification (inbox path)
        val pendingIdArg = savedStateHandle.get<Long>("pendingId")?.takeIf { it > 0 }
        if (pendingIdArg != null) {
            viewModelScope.launch {
                val pending = pendingNotificationRepository.getById(pendingIdArg) ?: return@launch
                val updates = mutableListOf<AddExpenseUiState.() -> AddExpenseUiState>()
                if (pending.rawBody != null) updates.add { copy(rawSmsBody = pending.rawBody) }
                updates.add { copy(date = Instant.fromEpochMilliseconds(pending.detectedAtMillis)) }
                if (pending.paymentMethod != null && paymentMethodArg == null) {
                    val pm = PaymentMethod.entries.find { it.name == pending.paymentMethod }
                    if (pm != null) updates.add { copy(paymentMethod = pm) }
                }
                if (updates.isNotEmpty()) {
                    _form.update { state -> updates.fold(state) { acc, fn -> fn(acc) } }
                }
            }
        }

        // Category inference — runs when merchant is pre-filled from a notification nav arg
        val merchantArg = savedStateHandle.get<String>("merchant")?.takeIf { it.isNotBlank() }
        val bankArg = savedStateHandle.get<String>("account")
            ?.substringBefore("*")?.trim()?.ifEmpty { null }
        if (merchantArg != null) {
            _form.update { it.copy(isCategoryInferring = true) }
            inferenceJob = viewModelScope.launch {
                val result = inferCategoryUseCase(
                    merchant = merchantArg,
                    bankName = bankArg,
                    smsBody = _form.value.rawSmsBody
                )
                if (result != null) {
                    _form.update {
                        it.copy(
                            selectedCategory = result.category,
                            isCategoryInferring = false,
                            categoryInferenceSource = result.source
                        )
                    }
                    // Tier 3 web result → persist as MerchantRule for future instant lookups
                    if (result.source == InferenceSource.AI_SEARCH) {
                        merchantRuleRepository.saveRule(
                            merchantPattern = merchantArg.trim().lowercase(),
                            categoryId = result.category.id,
                            categoryName = result.category.name
                        )
                    }
                } else {
                    _form.update { it.copy(isCategoryInferring = false) }
                }
            }
        }
    }

    val uiState = combine(
        combine(_form, getAccountsUseCase()) { form, accounts -> form.copy(accounts = accounts) },
        getCategoriesUseCase(),
        homeCurrencyFlow,
        combine(selectedCurrencyRateFlow, homeCurrencyRateFlow) { sel, home -> sel to home },
        tagRepository.getAllTags()
    ) { form, categories, homeCurrency, (selectedRate, homeRate), allTags ->
        val suggestedExchangeRate = when {
            form.currencyCode == homeCurrency -> 1.0
            selectedRate != null && homeRate != null && selectedRate.rateToBase > 0.0 ->
                homeRate.rateToBase / selectedRate.rateToBase
            else -> null
        }
        form.copy(
            categories = categories,
            homeCurrencyCode = homeCurrency,
            suggestedExchangeRate = suggestedExchangeRate,
            availableTags = allTags
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddExpenseUiState(date = DateTimeUtil.now())
    )

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { s ->
                val dotIdx = s.indexOf('.')
                if (dotIdx == -1) s else s.substring(0, dotIdx + 1) + s.substring(dotIdx + 1).filter { it.isDigit() }
            }
        _form.update { it.copy(amountInput = filtered) }
    }

    fun onTransactionTypeChange(type: TransactionType) {
        _form.update { it.copy(transactionType = type) }
        if (type == TransactionType.PAYMENT) {
            viewModelScope.launch { loadBillsForLinking() }
        } else {
            _form.update { it.copy(linkedBillId = null, linkedBill = null, availableBills = emptyList()) }
        }
    }
    fun onCategorySelect(category: Category) {
        inferenceJob?.cancel()
        inferenceJob = null
        _form.update { it.copy(selectedCategory = category, isCategorySheetVisible = false, isCategoryInferring = false) }
        // Auto-save merchant rule so future transactions for this merchant are auto-categorized
        val merchant = _form.value.merchantName.trim()
        if (merchant.isNotBlank()) {
            viewModelScope.launch {
                merchantRuleRepository.saveRule(
                    merchantPattern = merchant.lowercase(),
                    categoryId = category.id,
                    categoryName = category.name
                )
            }
        }
    }
    fun showCategorySheet() = _form.update { it.copy(isCategorySheetVisible = true) }
    fun dismissCategorySheet() = _form.update { it.copy(isCategorySheetVisible = false) }
    fun onPaymentMethodChange(method: PaymentMethod) = _form.update { it.copy(paymentMethod = method) }
    fun onDescriptionChange(value: String) = _form.update { it.copy(description = value) }
    fun onMerchantChange(value: String) = _form.update { it.copy(merchantName = value) }
    fun onTagSearchQueryChange(value: String) = _form.update { it.copy(tagSearchQuery = value) }
    fun onTagSelect(tag: Tag) = _form.update {
        if (it.selectedTags.any { t -> t.id == tag.id }) it
        else it.copy(selectedTags = it.selectedTags + tag, tagSearchQuery = "")
    }
    fun onTagRemove(tag: Tag) = _form.update {
        it.copy(selectedTags = it.selectedTags.filter { t -> t.id != tag.id })
    }
    fun onCreateTag(name: String) {
        viewModelScope.launch {
            val tag = tagRepository.createTag(name.trim())
            _form.update {
                it.copy(selectedTags = it.selectedTags + tag, tagSearchQuery = "")
            }
        }
    }
    fun showDatePicker() = _form.update { it.copy(showDatePicker = true) }
    fun dismissDatePicker() = _form.update { it.copy(showDatePicker = false) }
    fun showTimePicker() = _form.update { it.copy(showTimePicker = true) }
    fun dismissTimePicker() = _form.update { it.copy(showTimePicker = false) }

    fun onDateChange(selectedDateMillis: Long) {
        _form.update { state ->
            val tz = TimeZone.currentSystemDefault()
            val newDate = Instant.fromEpochMilliseconds(selectedDateMillis).toLocalDateTime(tz).date
            val currentTime = state.date.toLocalDateTime(tz).time
            state.copy(date = LocalDateTime(newDate, currentTime).toInstant(tz), showDatePicker = false)
        }
    }

    fun onTimeChange(hour: Int, minute: Int) {
        _form.update { state ->
            val tz = TimeZone.currentSystemDefault()
            val currentDate = state.date.toLocalDateTime(tz).date
            state.copy(date = LocalDateTime(currentDate, LocalTime(hour, minute)).toInstant(tz), showTimePicker = false)
        }
    }

    fun onCurrencyChange(code: String) = _form.update {
        it.copy(currencyCode = code, exchangeRateInput = "", isCurrencyPickerVisible = false, currencySearchQuery = "")
    }
    fun onExchangeRateChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }
            .let { s ->
                val dotIdx = s.indexOf('.')
                if (dotIdx == -1) s else s.substring(0, dotIdx + 1) + s.substring(dotIdx + 1).filter { it.isDigit() }
            }
        _form.update { it.copy(exchangeRateInput = filtered) }
    }
    fun showCurrencyPicker() = _form.update { it.copy(isCurrencyPickerVisible = true) }
    fun dismissCurrencyPicker() = _form.update { it.copy(isCurrencyPickerVisible = false) }
    fun onCurrencySearchQueryChange(value: String) = _form.update { it.copy(currencySearchQuery = value) }
    fun clearError() = _form.update { it.copy(error = null) }

    fun showEditAccount(account: com.expenseanalyst.domain.model.Account) = _form.update {
        it.copy(editingAccount = account, editBankName = account.bankName, editLastFour = account.lastFour ?: "", editAccountType = account.accountType)
    }
    fun dismissEditAccount() = _form.update { it.copy(editingAccount = null, editBankName = "", editLastFour = "") }
    fun onEditBankNameChange(value: String) = _form.update { it.copy(editBankName = value) }
    fun onEditLastFourChange(value: String) = _form.update { it.copy(editLastFour = value.filter { c -> c.isDigit() }.take(4)) }
    fun onEditAccountTypeChange(type: com.expenseanalyst.domain.model.AccountType) = _form.update { it.copy(editAccountType = type) }
    fun saveEditAccount() {
        val s = _form.value
        val acct = s.editingAccount ?: return
        val bankName = s.editBankName.trim()
        if (bankName.isBlank()) return
        viewModelScope.launch {
            val lastFour = s.editLastFour.ifEmpty { null }
            val displayName = if (lastFour != null) "$bankName *$lastFour · ${s.editAccountType.label}"
                             else "$bankName · ${s.editAccountType.label}"
            accountRepository.updateAccount(acct.copy(bankName = bankName, lastFour = lastFour, accountType = s.editAccountType, displayName = displayName))
            _form.update { it.copy(editingAccount = null, editBankName = "", editLastFour = "") }
        }
    }

    fun showAccountSheet() = _form.update { it.copy(isAccountSheetVisible = true) }
    fun dismissAccountSheet() = _form.update { it.copy(isAccountSheetVisible = false, isAddingNewAccount = false) }
    fun onAccountSelect(accountId: Long) = _form.update { it.copy(selectedAccountId = accountId, isAccountSheetVisible = false, isAddingNewAccount = false) }
    fun showAddNewAccountForm() = _form.update { it.copy(isAddingNewAccount = true) }
    fun hideAddNewAccountForm() = _form.update { it.copy(isAddingNewAccount = false, newAccountBankName = "", newAccountLastFour = "", newAccountType = AccountType.SAVINGS) }
    fun onNewAccountBankNameChange(value: String) = _form.update { it.copy(newAccountBankName = value) }
    fun onNewAccountLastFourChange(value: String) = _form.update { it.copy(newAccountLastFour = value.filter { c -> c.isDigit() }.take(4)) }
    fun onNewAccountTypeChange(type: AccountType) = _form.update { it.copy(newAccountType = type) }

    fun saveNewAccount() {
        val state = _form.value
        if (state.newAccountBankName.isBlank()) return
        viewModelScope.launch {
            _form.update { it.copy(isSavingAccount = true) }
            try {
                val newId = accountRepository.findOrCreate(
                    bankName = state.newAccountBankName.trim(),
                    lastFour = state.newAccountLastFour.ifEmpty { null },
                    accountType = state.newAccountType
                )
                _form.update {
                    it.copy(
                        selectedAccountId = newId,
                        isAccountSheetVisible = false,
                        isAddingNewAccount = false,
                        newAccountBankName = "",
                        newAccountLastFour = "",
                        newAccountType = AccountType.SAVINGS,
                        isSavingAccount = false
                    )
                }
            } catch (e: Exception) {
                _form.update { it.copy(isSavingAccount = false) }
            }
        }
    }

    fun showAddNewCategoryForm() = _form.update { it.copy(isAddingNewCategory = true) }
    fun hideAddNewCategoryForm() = _form.update {
        it.copy(isAddingNewCategory = false, newCategoryName = "", newCategoryIconName = "more_horiz")
    }
    fun onNewCategoryNameChange(value: String) = _form.update { it.copy(newCategoryName = value) }
    fun onNewCategoryIconChange(icon: String) = _form.update { it.copy(newCategoryIconName = icon) }
    fun saveNewCategory() {
        val state = _form.value
        if (state.newCategoryName.isBlank()) return
        viewModelScope.launch {
            _form.update { it.copy(isSavingCategory = true) }
            try {
                val maxSortOrder = state.categories.maxOfOrNull { it.sortOrder } ?: -1
                val newId = categoryRepository.addCategory(
                    Category(
                        name = state.newCategoryName.trim(),
                        iconName = state.newCategoryIconName,
                        colorHex = "#9E9E9E",
                        isDefault = false,
                        sortOrder = maxSortOrder + 1
                    )
                )
                val newCategory = Category(
                    id = newId,
                    name = state.newCategoryName.trim(),
                    iconName = state.newCategoryIconName,
                    colorHex = "#9E9E9E",
                    isDefault = false,
                    sortOrder = maxSortOrder + 1
                )
                _form.update {
                    it.copy(
                        selectedCategory = newCategory,
                        isCategorySheetVisible = false,
                        isAddingNewCategory = false,
                        newCategoryName = "",
                        newCategoryIconName = "more_horiz",
                        isSavingCategory = false
                    )
                }
            } catch (e: Exception) {
                _form.update { it.copy(isSavingCategory = false) }
            }
        }
    }

    fun onLinkBill(bill: com.expenseanalyst.domain.model.Bill) =
        _form.update { it.copy(linkedBillId = bill.id, linkedBill = bill, isBillPickerVisible = false) }

    fun onUnlinkBill() = _form.update { it.copy(linkedBillId = null, linkedBill = null) }
    fun showBillPicker() = _form.update { it.copy(isBillPickerVisible = true) }
    fun dismissBillPicker() = _form.update { it.copy(isBillPickerVisible = false) }

    private suspend fun loadBillsForLinking() {
        val openBills = billRepository.getBills().first()
            .filter { !it.isDeleted && it.status != BillStatus.SETTLED }
        val merchant = _form.value.merchantName.ifBlank { null }
        val autoMatch = if (merchant != null) {
            openBills.firstOrNull { bill ->
                bill.billerName.contains(merchant, ignoreCase = true) ||
                    merchant.contains(bill.billerName, ignoreCase = true)
            }
        } else null
        _form.update { state ->
            state.copy(
                availableBills = openBills,
                linkedBillId = autoMatch?.id ?: state.linkedBillId,
                linkedBill = autoMatch ?: state.linkedBill
            )
        }
    }

    fun saveExpense() {
        val state = uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            try {
                val fromNotification = savedStateHandle.get<String>("amount")?.isNotBlank() == true
                val merchantName = state.merchantName.trim().ifEmpty { null }
                val sourceType = if (fromNotification) SourceType.NOTIFICATION_AUTO else SourceType.MANUAL

                // Bill linking — use whatever the user explicitly linked (auto or manual)
                var billId = state.linkedBillId
                if (state.transactionType == TransactionType.PAYMENT && billId != null) {
                    val linkedBill = billRepository.getBillById(billId).first()
                    if (linkedBill != null) {
                        // Bills are always stored in home currency; compare using homeAmount
                        val paid = state.computedHomeAmount ?: state.parsedAmount
                        val billTotalDue = linkedBill.totalDue
                        val newStatus = if (billTotalDue == null || paid >= billTotalDue)
                            BillStatus.SETTLED else BillStatus.PARTIAL
                        billRepository.updateBill(linkedBill.copy(status = newStatus))
                    }
                }

                val expense = Expense(
                    amount = state.parsedAmount,
                    currencyCode = state.currencyCode,
                    homeAmount = state.computedHomeAmount,
                    exchangeRate = state.effectiveExchangeRate,
                    description = state.description.trim(),
                    category = state.selectedCategory!!,
                    paymentMethod = state.paymentMethod,
                    transactionType = state.transactionType,
                    date = state.date,
                    merchantName = merchantName,
                    sourceType = sourceType,
                    tags = state.selectedTags,
                    accountId = state.selectedAccountId,
                    billId = billId,
                    rawSmsBody = state.rawSmsBody
                )
                val id = addExpenseUseCase(expense)

                // Clear all matching pending TRANSACTION notifications regardless of how we got here.
                // This covers: tap-notification flow, banner flow, and manual add with same amount/currency.
                val allPending = pendingNotificationRepository.getAll().first()
                allPending
                    .filter { pending ->
                        pending.pendingType == "TRANSACTION" &&
                            pending.amount == expense.amount &&
                            pending.currencyCode == expense.currencyCode
                    }
                    .forEach { pending -> pendingNotificationRepository.delete(pending.id) }

                // Also clear by explicit pendingId in case the user edited the amount before saving.
                val pendingId = savedStateHandle.get<Long>("pendingId")?.takeIf { it > 0 }
                if (pendingId != null) pendingNotificationRepository.delete(pendingId)
                _form.update { it.copy(isSaving = false, savedExpenseId = id) }
            } catch (e: Exception) {
                _form.update { it.copy(isSaving = false, error = "Failed to save expense") }
            }
        }
    }
}
