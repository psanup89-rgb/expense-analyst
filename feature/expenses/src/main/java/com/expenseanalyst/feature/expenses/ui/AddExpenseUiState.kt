package com.expenseanalyst.feature.expenses.ui

import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.usecase.InferenceSource
import kotlinx.datetime.Instant

data class AddExpenseUiState(
    val amountInput: String = "",
    val currencyCode: String = "INR",
    val homeCurrencyCode: String = "INR",
    val exchangeRateInput: String = "",
    val suggestedExchangeRate: Double? = null,
    val isCurrencyPickerVisible: Boolean = false,
    val currencySearchQuery: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: Category? = null,
    val isCategorySheetVisible: Boolean = false,
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val date: Instant = Instant.DISTANT_PAST,
    val description: String = "",
    val merchantName: String = "",
    val selectedTags: List<Tag> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val tagSearchQuery: String = "",
    val accounts: List<Account> = emptyList(),
    val selectedAccountId: Long? = null,
    val isAccountSheetVisible: Boolean = false,
    val isAddingNewAccount: Boolean = false,
    val newAccountBankName: String = "",
    val newAccountLastFour: String = "",
    val newAccountType: AccountType = AccountType.SAVINGS,
    val isSavingAccount: Boolean = false,
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val savedExpenseId: Long? = null,
    val error: String? = null,
    val rawSmsBody: String? = null,
    val expenseSourceType: SourceType? = null,
    val isAddingNewCategory: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryIconName: String = "more_horiz",
    val isSavingCategory: Boolean = false,
    val isCategoryInferring: Boolean = false,
    val categoryInferenceSource: InferenceSource? = null,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val editingAccount: Account? = null,
    val editBankName: String = "",
    val editLastFour: String = "",
    val editAccountType: AccountType = AccountType.SAVINGS,
    // Bill linking — visible for PAYMENT type transactions
    val linkedBillId: Long? = null,
    val linkedBill: Bill? = null,
    val availableBills: List<Bill> = emptyList(),
    val isBillPickerVisible: Boolean = false
) {
    val selectedAccount: Account? get() = accounts.find { it.id == selectedAccountId }

    val parsedAmount: Double get() = amountInput.toDoubleOrNull() ?: 0.0
    val parsedExchangeRate: Double?
        get() = exchangeRateInput.toDoubleOrNull()?.takeIf { it > 0 }

    val effectiveExchangeRate: Double?
        get() = when {
            currencyCode == homeCurrencyCode -> 1.0
            parsedExchangeRate != null -> parsedExchangeRate
            suggestedExchangeRate != null -> suggestedExchangeRate
            else -> null
        }

    val computedHomeAmount: Double?
        get() = when {
            parsedAmount <= 0 -> null
            currencyCode == homeCurrencyCode -> parsedAmount
            effectiveExchangeRate != null -> parsedAmount * effectiveExchangeRate!!
            else -> null
        }

    val needsManualExchangeRate: Boolean
        get() = currencyCode != homeCurrencyCode && suggestedExchangeRate == null

    val isValid: Boolean
        get() = parsedAmount > 0 &&
            selectedCategory != null &&
            selectedAccountId != null &&
            merchantName.isNotBlank() &&
            (currencyCode == homeCurrencyCode || effectiveExchangeRate != null)
}
