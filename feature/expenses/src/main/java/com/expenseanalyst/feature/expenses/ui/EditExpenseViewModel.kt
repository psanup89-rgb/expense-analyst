package com.expenseanalyst.feature.expenses.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.AccountRepository
import com.expenseanalyst.domain.repository.CurrencyRepository
import com.expenseanalyst.domain.repository.TagRepository
import com.expenseanalyst.domain.usecase.GetAccountsUseCase
import com.expenseanalyst.domain.usecase.GetCategoriesUseCase
import com.expenseanalyst.domain.usecase.GetExpenseByIdUseCase
import com.expenseanalyst.domain.usecase.UpdateExpenseUseCase
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
class EditExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val expenseId: Long = checkNotNull(savedStateHandle["expenseId"])

    private val _form = MutableStateFlow(AddExpenseUiState())
    private val _originalExpense = MutableStateFlow<Expense?>(null)

    private val homeCurrencyFlow: Flow<String> = currencyRepository.getHomeCurrency().distinctUntilChanged()

    private val selectedCurrencyRateFlow = _form
        .map { it.currencyCode }
        .distinctUntilChanged()
        .flatMapLatest { code -> currencyRepository.getRate(code) }

    private val homeCurrencyRateFlow = homeCurrencyFlow
        .flatMapLatest { code -> currencyRepository.getRate(code) }

    init {
        viewModelScope.launch {
            val expense = getExpenseByIdUseCase(expenseId).first() ?: return@launch
            _originalExpense.value = expense
            // For older auto-imported expenses, merchantName was left null and the merchant/bank
            // name was stored in description. Migrate those on edit: move description → merchantName.
            val migratedMerchant = expense.merchantName?.takeIf { it.isNotBlank() }
                ?: expense.description.takeIf { it.isNotBlank() }
                ?: ""
            val migratedDescription = if (expense.merchantName.isNullOrBlank()) "" else expense.description
            _form.value = AddExpenseUiState(
                amountInput = expense.amount.toBigDecimal().stripTrailingZeros().toPlainString(),
                currencyCode = expense.currencyCode,
                transactionType = expense.transactionType,
                selectedCategory = expense.category,
                paymentMethod = expense.paymentMethod,
                date = expense.date,
                description = migratedDescription,
                merchantName = migratedMerchant,
                selectedTags = expense.tags,
                selectedAccountId = expense.accountId
            )
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
        initialValue = AddExpenseUiState()
    )

    fun onAmountChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }.let { s ->
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
    fun onCurrencyChange(code: String) = _form.update {
        it.copy(currencyCode = code, exchangeRateInput = "", isCurrencyPickerVisible = false, currencySearchQuery = "")
    }
    fun onExchangeRateChange(value: String) {
        val filtered = value.filter { it.isDigit() || it == '.' }.let { s ->
            val dotIdx = s.indexOf('.')
            if (dotIdx == -1) s else s.substring(0, dotIdx + 1) + s.substring(dotIdx + 1).filter { it.isDigit() }
        }
        _form.update { it.copy(exchangeRateInput = filtered) }
    }
    fun showCurrencyPicker() = _form.update { it.copy(isCurrencyPickerVisible = true) }
    fun dismissCurrencyPicker() = _form.update { it.copy(isCurrencyPickerVisible = false) }
    fun onCurrencySearchQueryChange(value: String) = _form.update { it.copy(currencySearchQuery = value) }

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
        val original = _originalExpense.value ?: return
        val state = uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            try {
                val updated = original.copy(
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
                    tags = state.selectedTags,
                    accountId = state.selectedAccountId
                )
                updateExpenseUseCase(updated)
                _form.update { it.copy(isSaving = false, savedExpenseId = original.id) }
            } catch (e: Exception) {
                _form.update { it.copy(isSaving = false, error = "Failed to update expense") }
            }
        }
    }
}
