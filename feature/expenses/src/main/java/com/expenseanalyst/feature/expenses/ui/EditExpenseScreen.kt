package com.expenseanalyst.feature.expenses.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedExpenseId) {
        if (uiState.savedExpenseId != null) onSaved()
    }

    AddExpenseContent(
        uiState = uiState,
        titleOverride = "Edit Expense",
        onBack = onBack,
        onAmountChange = viewModel::onAmountChange,
        onTransactionTypeChange = viewModel::onTransactionTypeChange,
        onCategorySelect = viewModel::onCategorySelect,
        onShowCategorySheet = viewModel::showCategorySheet,
        onDismissCategorySheet = viewModel::dismissCategorySheet,
        onPaymentMethodChange = viewModel::onPaymentMethodChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onMerchantChange = viewModel::onMerchantChange,
        onTagSearchQueryChange = viewModel::onTagSearchQueryChange,
        onTagSelect = viewModel::onTagSelect,
        onTagRemove = viewModel::onTagRemove,
        onCreateTag = viewModel::onCreateTag,
        onCurrencyChange = viewModel::onCurrencyChange,
        onExchangeRateChange = viewModel::onExchangeRateChange,
        onShowCurrencyPicker = viewModel::showCurrencyPicker,
        onDismissCurrencyPicker = viewModel::dismissCurrencyPicker,
        onCurrencySearchQueryChange = viewModel::onCurrencySearchQueryChange,
        onShowAccountSheet = viewModel::showAccountSheet,
        onDismissAccountSheet = viewModel::dismissAccountSheet,
        onAccountSelect = viewModel::onAccountSelect,
        onShowAddNewAccount = viewModel::showAddNewAccountForm,
        onHideAddNewAccount = viewModel::hideAddNewAccountForm,
        onNewAccountBankNameChange = viewModel::onNewAccountBankNameChange,
        onNewAccountLastFourChange = viewModel::onNewAccountLastFourChange,
        onNewAccountTypeChange = viewModel::onNewAccountTypeChange,
        onSaveNewAccount = viewModel::saveNewAccount,
        onEditAccount = {},
        onDismissEditAccount = {},
        onEditBankNameChange = {},
        onEditLastFourChange = {},
        onEditAccountTypeChange = {},
        onSaveEditAccount = {},
        onShowDatePicker = viewModel::showDatePicker,
        onDismissDatePicker = viewModel::dismissDatePicker,
        onDateChange = viewModel::onDateChange,
        onShowTimePicker = viewModel::showTimePicker,
        onDismissTimePicker = viewModel::dismissTimePicker,
        onTimeChange = viewModel::onTimeChange,
        onSave = viewModel::saveExpense
    )
}
