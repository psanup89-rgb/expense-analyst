package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.AccountRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.usecase.AddExpenseUseCase
import com.expenseanalyst.domain.usecase.GetAccountsUseCase
import com.expenseanalyst.domain.usecase.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

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
        val accountNavArg = savedStateHandle.get<String>("account")?.takeIf { it.isNotBlank() }
        if (accountNavArg != null) {
            viewModelScope.launch {
                val allAccounts = getAccountsUseCase().first()
                val last4 = Regex("""\*(\d{4})""").find(accountNavArg)?.groupValues?.get(1)
                val bankName = accountNavArg.substringBefore("*").trim().ifEmpty { null }
                val matched = allAccounts.find { account ->
                    (last4 != null && account.lastFour == last4) ||
                        (bankName != null && account.bankName.contains(bankName, ignoreCase = true))
                }
                if (matched != null) {
                    _form.update { it.copy(selectedAccountId = matched.id) }
                } else if (bankName != null) {
                    val newId = accountRepository.findOrCreate(
                        bankName = bankName,
                        lastFour = last4,
                        accountType = AccountType.OTHER
                    )
                    _form.update { it.copy(selectedAccountId = newId) }
                }
            }
        }
    }

    val uiState = combine(
        combine(_form, getAccountsUseCase()) { form, accounts -> form.copy(accounts = accounts) },
        getCategoriesUseCase(),
        homeCurrencyFlow,
        selectedCurrencyRateFlow,
        homeCurrencyRateFlow
    ) { form, categories, homeCurrency, selectedRate, homeRate ->
        val suggestedExchangeRate = when {
            form.currencyCode == homeCurrency -> 1.0
            selectedRate != null && homeRate != null && selectedRate.rateToBase > 0.0 ->
                homeRate.rateToBase / selectedRate.rateToBase
            else -> null
        }
        form.copy(
            categories = categories,
            homeCurrencyCode = homeCurrency,
            suggestedExchangeRate = suggestedExchangeRate
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

    fun onTransactionTypeChange(type: TransactionType) = _form.update { it.copy(transactionType = type) }
    fun onCategorySelect(category: Category) = _form.update { it.copy(selectedCategory = category, isCategorySheetVisible = false) }
    fun showCategorySheet() = _form.update { it.copy(isCategorySheetVisible = true) }
    fun dismissCategorySheet() = _form.update { it.copy(isCategorySheetVisible = false) }
    fun onPaymentMethodChange(method: PaymentMethod) = _form.update { it.copy(paymentMethod = method) }
    fun onDescriptionChange(value: String) = _form.update { it.copy(description = value) }
    fun onMerchantChange(value: String) = _form.update { it.copy(merchantName = value) }
    fun onNoteChange(value: String) = _form.update { it.copy(note = value) }
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

    fun saveExpense() {
        val state = uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            try {
                val fromNotification = savedStateHandle.get<String>("amount")?.isNotBlank() == true
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
                    merchantName = state.merchantName.trim().ifEmpty { null },
                    sourceType = if (fromNotification) SourceType.NOTIFICATION_AUTO else SourceType.MANUAL,
                    note = state.note.trim().ifEmpty { null },
                    accountId = state.selectedAccountId
                )
                val id = addExpenseUseCase(expense)
                _form.update { it.copy(isSaving = false, savedExpenseId = id) }
            } catch (e: Exception) {
                _form.update { it.copy(isSaving = false, error = "Failed to save expense") }
            }
        }
    }
}
