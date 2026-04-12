package com.expenseanalyst.feature.expenses.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.expenseanalyst.core.util.CurrencyCatalog
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.core.util.availableCategoryIcons
import com.expenseanalyst.core.util.categoryIconVector
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.Bill
import com.expenseanalyst.domain.model.Category
import com.expenseanalyst.domain.model.PaymentMethod
import com.expenseanalyst.domain.model.SourceType
import com.expenseanalyst.domain.model.Tag
import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.usecase.InferenceSource
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.ReceiptLong
import java.text.SimpleDateFormat
import java.util.Date


@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedExpenseId) {
        if (uiState.savedExpenseId != null) onSaved()
    }

    AddExpenseContent(
        uiState = uiState,
        titleOverride = "Add Expense",
        onBack = onBack,
        onAmountChange = viewModel::onAmountChange,
        onTransactionTypeChange = viewModel::onTransactionTypeChange,
        onCategorySelect = viewModel::onCategorySelect,
        onShowCategorySheet = viewModel::showCategorySheet,
        onDismissCategorySheet = viewModel::dismissCategorySheet,
        onShowAddNewCategory = viewModel::showAddNewCategoryForm,
        onHideAddNewCategory = viewModel::hideAddNewCategoryForm,
        onNewCategoryNameChange = viewModel::onNewCategoryNameChange,
        onNewCategoryIconChange = viewModel::onNewCategoryIconChange,
        onSaveNewCategory = viewModel::saveNewCategory,
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
        onEditAccount = viewModel::showEditAccount,
        onDismissEditAccount = viewModel::dismissEditAccount,
        onEditBankNameChange = viewModel::onEditBankNameChange,
        onEditLastFourChange = viewModel::onEditLastFourChange,
        onEditAccountTypeChange = viewModel::onEditAccountTypeChange,
        onSaveEditAccount = viewModel::saveEditAccount,
        onShowDatePicker = viewModel::showDatePicker,
        onDismissDatePicker = viewModel::dismissDatePicker,
        onDateChange = viewModel::onDateChange,
        onShowTimePicker = viewModel::showTimePicker,
        onDismissTimePicker = viewModel::dismissTimePicker,
        onTimeChange = viewModel::onTimeChange,
        onLinkBill = viewModel::onLinkBill,
        onUnlinkBill = viewModel::onUnlinkBill,
        onShowBillPicker = viewModel::showBillPicker,
        onDismissBillPicker = viewModel::dismissBillPicker,
        onSave = viewModel::saveExpense
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddExpenseContent(
    uiState: AddExpenseUiState,
    titleOverride: String = "Add Expense",
    onBack: () -> Unit,
    onAmountChange: (String) -> Unit,
    onTransactionTypeChange: (TransactionType) -> Unit,
    onCategorySelect: (Category) -> Unit,
    onShowCategorySheet: () -> Unit,
    onDismissCategorySheet: () -> Unit,
    onShowAddNewCategory: () -> Unit,
    onHideAddNewCategory: () -> Unit,
    onNewCategoryNameChange: (String) -> Unit,
    onNewCategoryIconChange: (String) -> Unit,
    onSaveNewCategory: () -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onTagSearchQueryChange: (String) -> Unit,
    onTagSelect: (Tag) -> Unit,
    onTagRemove: (Tag) -> Unit,
    onCreateTag: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onExchangeRateChange: (String) -> Unit,
    onShowCurrencyPicker: () -> Unit,
    onDismissCurrencyPicker: () -> Unit,
    onCurrencySearchQueryChange: (String) -> Unit,
    onShowAccountSheet: () -> Unit,
    onDismissAccountSheet: () -> Unit,
    onAccountSelect: (Long) -> Unit,
    onShowAddNewAccount: () -> Unit,
    onHideAddNewAccount: () -> Unit,
    onNewAccountBankNameChange: (String) -> Unit,
    onNewAccountLastFourChange: (String) -> Unit,
    onNewAccountTypeChange: (AccountType) -> Unit,
    onSaveNewAccount: () -> Unit,
    onEditAccount: (com.expenseanalyst.domain.model.Account) -> Unit,
    onDismissEditAccount: () -> Unit,
    onEditBankNameChange: (String) -> Unit,
    onEditLastFourChange: (String) -> Unit,
    onEditAccountTypeChange: (AccountType) -> Unit,
    onSaveEditAccount: () -> Unit,
    onShowDatePicker: () -> Unit,
    onDismissDatePicker: () -> Unit,
    onDateChange: (Long) -> Unit,
    onShowTimePicker: () -> Unit,
    onDismissTimePicker: () -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    onLinkBill: (Bill) -> Unit,
    onUnlinkBill: () -> Unit,
    onShowBillPicker: () -> Unit,
    onDismissBillPicker: () -> Unit,
    onSave: () -> Unit
) {
    val isExpense = uiState.transactionType == TransactionType.EXPENSE
    val accentColor = if (isExpense) Color(0xFFFF5555) else MaterialTheme.colorScheme.primary
    val categoryRows = uiState.categories.chunked(4)
    val filteredCurrencies = CurrencyCatalog.all.filter { currency ->
        val query = uiState.currencySearchQuery.trim()
        query.isBlank() ||
            currency.code.contains(query, ignoreCase = true) ||
            currency.displayName.contains(query, ignoreCase = true)
    }

    // Currency picker sheet
    if (uiState.isCurrencyPickerVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissCurrencyPicker,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text("Select Currency", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.currencySearchQuery,
                    onValueChange = onCurrencySearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search currency code or name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filteredCurrencies.take(200)) { currency ->
                        val selected = currency.code == uiState.currencyCode
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCurrencyChange(currency.code) }
                                .padding(vertical = 12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(currency.code, style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Text(currency.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(currency.symbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }

    // Category picker sheet
    if (uiState.isCategorySheetVisible) {
        var categoryQuery by remember { mutableStateOf("") }
        val filteredCategoryRows = uiState.categories
            .filter { it.name.contains(categoryQuery.trim(), ignoreCase = true) }
            .chunked(4)
        ModalBottomSheet(
            onDismissRequest = onDismissCategorySheet,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text("Select Category", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                if (!uiState.isAddingNewCategory) {
                    // Normal mode: "Add new category" button + search + grid
                    TextButton(
                        onClick = onShowAddNewCategory,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add new category")
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = categoryQuery,
                        onValueChange = { categoryQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search category") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredCategoryRows.forEach { rowCategories ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowCategories.forEach { category ->
                                    CategoryItem(
                                        category = category,
                                        selected = uiState.selectedCategory?.id == category.id,
                                        onClick = { onCategorySelect(category) },
                                        modifier = Modifier.width(72.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Inline "new category" form
                    Spacer(Modifier.height(12.dp))
                    Text("New Category", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.newCategoryName,
                        onValueChange = onNewCategoryNameChange,
                        label = { Text("Category name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Icon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    var iconSearch by remember { mutableStateOf("") }
                    val filteredIcons = remember(iconSearch) {
                        availableCategoryIcons.filter {
                            it.replace("_", " ").contains(iconSearch.trim(), ignoreCase = true)
                        }
                    }
                    OutlinedTextField(
                        value = iconSearch,
                        onValueChange = { iconSearch = it },
                        placeholder = { Text("Search icons…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItems(filteredIcons) { icon ->
                            val selected = icon == uiState.newCategoryIconName
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(
                                        if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clickable { onNewCategoryIconChange(icon) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIconVector(icon),
                                    contentDescription = icon,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onHideAddNewCategory) { Text("Cancel") }
                        Button(
                            onClick = onSaveNewCategory,
                            enabled = uiState.newCategoryName.isNotBlank() && !uiState.isSavingCategory
                        ) {
                            if (uiState.isSavingCategory) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Account picker sheet
    if (uiState.isAccountSheetVisible) {
        var accountQuery by remember { mutableStateOf("") }
        val filteredAccounts = uiState.accounts.filter {
            it.displayName.contains(accountQuery.trim(), ignoreCase = true)
        }
        ModalBottomSheet(
            onDismissRequest = onDismissAccountSheet,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text("Select Account", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                if (!uiState.isAddingNewAccount) {
                    TextButton(
                        onClick = onShowAddNewAccount,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add new account")
                    }
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text("New Account", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.newAccountBankName,
                        onValueChange = onNewAccountBankNameChange,
                        label = { Text("Bank / Wallet name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.newAccountLastFour,
                        onValueChange = onNewAccountLastFourChange,
                        label = { Text("Last 4 digits (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AccountType.entries) { type ->
                            FilterChip(
                                selected = uiState.newAccountType == type,
                                onClick = { onNewAccountTypeChange(type) },
                                label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(onClick = onHideAddNewAccount) { Text("Cancel") }
                        Button(
                            onClick = onSaveNewAccount,
                            enabled = uiState.newAccountBankName.isNotBlank() && !uiState.isSavingAccount
                        ) {
                            if (uiState.isSavingAccount) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
                if (!uiState.isAddingNewAccount) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accountQuery,
                        onValueChange = { accountQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search account") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                if (uiState.accounts.isEmpty() && !uiState.isAddingNewAccount) {
                    Text("No accounts yet. Add one above.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filteredAccounts, key = { it.id }) { account ->
                        ListItem(
                            headlineContent = { Text(account.displayName, style = MaterialTheme.typography.bodyMedium) },
                            supportingContent = { Text(account.accountType.label, style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onEditAccount(account) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit account", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (account.id == uiState.selectedAccountId) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onAccountSelect(account.id) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Edit account dialog
    if (uiState.editingAccount != null) {
        AlertDialog(
            onDismissRequest = onDismissEditAccount,
            title = { Text("Edit Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.editBankName,
                        onValueChange = onEditBankNameChange,
                        label = { Text("Bank / Wallet name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    OutlinedTextField(
                        value = uiState.editLastFour,
                        onValueChange = onEditLastFourChange,
                        label = { Text("Last 4 digits (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text("Account type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AccountType.entries) { type ->
                            FilterChip(
                                selected = uiState.editAccountType == type,
                                onClick = { onEditAccountTypeChange(type) },
                                label = { Text(type.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onSaveEditAccount, enabled = uiState.editBankName.isNotBlank()) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEditAccount) { Text("Cancel") }
            }
        )
    }

    // Bill picker sheet (for PAYMENT type)
    if (uiState.isBillPickerVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissBillPicker,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text("Link to a Bill", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Select the open bill this payment settles",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                if (uiState.availableBills.isEmpty()) {
                    Text(
                        "No open bills found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(uiState.availableBills) { bill ->
                            val isSelected = bill.id == uiState.linkedBillId
                            val dueDateStr = bill.dueDateMillis?.let {
                                "Due " + SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(Date(it))
                            }
                            ListItem(
                                headlineContent = {
                                    Text(
                                        bill.billerName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                supportingContent = {
                                    val parts = listOfNotNull(
                                        bill.totalDue?.let { "%.2f %s".format(it, bill.currencyCode) },
                                        dueDateStr
                                    )
                                    if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.clickable { onLinkBill(bill) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }

    // Date picker dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = onDismissDatePicker,
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateChange(it) }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDatePicker) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // Time picker dialog
    if (uiState.showTimePicker) {
        val ldt = uiState.date.toLocalDateTime(TimeZone.currentSystemDefault())
        val timePickerState = rememberTimePickerState(
            initialHour = ldt.hour,
            initialMinute = ldt.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = onDismissTimePicker,
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = { onTimeChange(timePickerState.hour, timePickerState.minute) }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onDismissTimePicker) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(titleOverride, style = MaterialTheme.typography.titleLarge) },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Button(
                onClick = onSave,
                enabled = uiState.isValid && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Save Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        val context = LocalContext.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {
            // Amount + Type card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.transactionType == TransactionType.EXPENSE,
                                onClick = { onTransactionTypeChange(TransactionType.EXPENSE) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                                colors = SegmentedButtonDefaults.colors(activeContainerColor = Color(0xFFFF5555).copy(alpha = 0.15f), activeContentColor = Color(0xFFFF5555))
                            ) { Text("Expense", style = MaterialTheme.typography.labelLarge) }
                            SegmentedButton(
                                selected = uiState.transactionType == TransactionType.INCOME,
                                onClick = { onTransactionTypeChange(TransactionType.INCOME) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                                colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.primaryContainer, activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) { Text("Income", style = MaterialTheme.typography.labelLarge) }
                            SegmentedButton(
                                selected = uiState.transactionType == TransactionType.TRANSFER,
                                onClick = { onTransactionTypeChange(TransactionType.TRANSFER) },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                                colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.secondaryContainer, activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) { Text("Transfer", style = MaterialTheme.typography.labelLarge) }
                            SegmentedButton(
                                selected = uiState.transactionType == TransactionType.PAYMENT,
                                onClick = { onTransactionTypeChange(TransactionType.PAYMENT) },
                                shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                                colors = SegmentedButtonDefaults.colors(activeContainerColor = Color(0xFF7C5CBF).copy(alpha = 0.15f), activeContentColor = Color(0xFF7C5CBF))
                            ) { Text("Payment", style = MaterialTheme.typography.labelLarge) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(uiState.currencyCode, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = if (uiState.amountInput.isEmpty()) "0" else uiState.amountInput,
                                fontSize = 48.sp, fontWeight = FontWeight.Bold,
                                color = if (uiState.amountInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else accentColor
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = uiState.amountInput,
                            onValueChange = onAmountChange,
                            label = { Text("Enter amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor, focusedLabelColor = accentColor, cursorColor = accentColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent
                            ),
                            trailingIcon = {
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onShowCurrencyPicker).padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(uiState.currencyCode, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Home currency: ${uiState.homeCurrencyCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (uiState.currencyCode != uiState.homeCurrencyCode) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.exchangeRateInput,
                                onValueChange = onExchangeRateChange,
                                label = { Text(if (uiState.suggestedExchangeRate == null) "Exchange rate *" else "Override rate (optional)") },
                                supportingText = {
                                    val msg = if (uiState.suggestedExchangeRate != null)
                                        "Auto rate: 1 ${uiState.currencyCode} = ${String.format("%.4f", uiState.suggestedExchangeRate)} ${uiState.homeCurrencyCode}"
                                    else "No cached rate yet. Enter how much 1 ${uiState.currencyCode} equals in ${uiState.homeCurrencyCode}."
                                    Text(msg)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor, focusedLabelColor = accentColor, cursorColor = accentColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent
                                )
                            )
                        }
                        uiState.computedHomeAmount?.let { homeAmount ->
                            Spacer(Modifier.height(12.dp))
                            Text("Home amount: ${CurrencyFormatter.format(homeAmount, uiState.homeCurrencyCode)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Date & Time
            item {
                FormSection(title = "Date & Time") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onShowDatePicker,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(DateTimeUtil.formatDateHeader(uiState.date), style = MaterialTheme.typography.bodyMedium)
                        }
                        OutlinedButton(
                            onClick = onShowTimePicker,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(DateTimeUtil.formatTime(uiState.date), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Account (mandatory)
            item {
                FormSection(title = "Account *") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (uiState.selectedAccountId == null)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .border(
                                width = 1.dp,
                                color = if (uiState.selectedAccountId == null)
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = onShowAccountSheet)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.selectedAccount?.displayName ?: "Select or add account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.selectedAccountId == null)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Category (compact chip -> sheet)
            item {
                FormSection(title = "Category *") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable(onClick = onShowCategorySheet)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val selectedCategory = uiState.selectedCategory
                        if (selectedCategory != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = categoryIconVector(selectedCategory.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(selectedCategory.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    if (uiState.categoryInferenceSource == InferenceSource.AI_SEARCH ||
                                        uiState.categoryInferenceSource == InferenceSource.KEYWORD) {
                                        Text(
                                            text = "Suggested · tap to change",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (uiState.isCategoryInferring) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                Text("Detecting category…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Text("Select category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Payment Method (horizontal scroll row)
            item {
                FormSection(title = "Payment Method") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val ordered = listOf(uiState.paymentMethod) +
                            PaymentMethod.entries.filter { it != uiState.paymentMethod }
                        items(ordered) { method ->
                            FilterChip(
                                selected = uiState.paymentMethod == method,
                                onClick = { onPaymentMethodChange(method) },
                                label = { Text(method.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = uiState.paymentMethod == method,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Details
            item {
                FormSection(title = "Details") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NeonTextField(value = uiState.merchantName, onValueChange = onMerchantChange, label = "Merchant *", accentColor = accentColor)
                        NeonTextField(value = uiState.description, onValueChange = onDescriptionChange, label = "Description (optional)", accentColor = accentColor)
                        TagSelector(
                            selectedTags = uiState.selectedTags,
                            availableTags = uiState.availableTags,
                            searchQuery = uiState.tagSearchQuery,
                            onSearchQueryChange = onTagSearchQueryChange,
                            onTagSelect = onTagSelect,
                            onTagRemove = onTagRemove,
                            onCreateTag = onCreateTag,
                            accentColor = accentColor
                        )
                    }
                }
            }

            // Linked bill — shown for PAYMENT type only
            if (uiState.transactionType == TransactionType.PAYMENT) {
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    FormSection(title = "Linked Bill") {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            if (uiState.linkedBill != null) {
                                // Linked bill chip row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.linkedBill.billerName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        val parts = listOfNotNull(
                                            uiState.linkedBill.totalDue?.let {
                                                "%.2f %s due".format(it, uiState.linkedBill.currencyCode)
                                            },
                                            uiState.linkedBill.dueDateMillis?.let {
                                                SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(Date(it))
                                            }
                                        )
                                        if (parts.isNotEmpty()) {
                                            Text(
                                                text = parts.joinToString(" · "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    // Change button
                                    TextButton(onClick = onShowBillPicker) {
                                        Text("Change", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    // Unlink
                                    IconButton(onClick = onUnlinkBill, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                            Icons.Default.LinkOff,
                                            contentDescription = "Unlink bill",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = onShowBillPicker,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (uiState.availableBills.isEmpty()) "No open bills to link"
                                        else "Link to a Bill"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Raw SMS preview (shown for auto-imported expenses; hidden for manually added ones)
            val isAutoImported = uiState.expenseSourceType != null &&
                uiState.expenseSourceType != SourceType.MANUAL
            if (isAutoImported || !uiState.rawSmsBody.isNullOrBlank()) {
                val rawBody = uiState.rawSmsBody
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    RawSmsPreviewCard(
                        rawBody = rawBody,
                        onOpenInMessages = if (!rawBody.isNullOrBlank()) {
                            {
                                val address = findSmsAddress(context, rawBody)
                                val intent = if (address != null) {
                                    Intent(Intent.ACTION_VIEW, Uri.parse("sms:$address"))
                                } else {
                                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        } else null
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun RawSmsPreviewCard(rawBody: String?, onOpenInMessages: (() -> Unit)? = null) {
    val hasBody = !rawBody.isNullOrBlank()
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (hasBody) Modifier.clickable { expanded = !expanded } else Modifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Source SMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                if (hasBody) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hasBody && expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = rawBody!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                if (onOpenInMessages != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onOpenInMessages,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "Open in Messages \u2197",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else if (!hasBody) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Auto-imported from SMS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
        )
        content()
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = categoryIconVector(category.iconName),
            contentDescription = category.name,
            modifier = Modifier.size(24.dp),
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun NeonTextField(value: String, onValueChange: (String) -> Unit, label: String, accentColor: Color) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            focusedLabelColor = accentColor,
            cursorColor = accentColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TagSelector(
    selectedTags: List<Tag>,
    availableTags: List<Tag>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onTagSelect: (Tag) -> Unit,
    onTagRemove: (Tag) -> Unit,
    onCreateTag: (String) -> Unit,
    accentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Selected tags as removable chips
        if (selectedTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onTagRemove(tag) },
                        label = { Text(tag.name, style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove ${tag.name}",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.15f),
                            selectedLabelColor = accentColor,
                            selectedTrailingIconColor = accentColor
                        )
                    )
                }
            }
        }

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Tags") },
            placeholder = { Text("Search or create tags...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Suggestions: filtered available tags (excluding already selected)
        val selectedIds = selectedTags.map { it.id }.toSet()
        val query = searchQuery.trim()
        val suggestions = if (query.isBlank()) {
            availableTags.filter { it.id !in selectedIds }.take(6)
        } else {
            availableTags.filter {
                it.id !in selectedIds && it.name.contains(query, ignoreCase = true)
            }
        }
        val exactMatch = availableTags.any { it.name.equals(query, ignoreCase = true) }

        if (suggestions.isNotEmpty() || (query.isNotBlank() && !exactMatch)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                suggestions.forEach { tag ->
                    FilterChip(
                        selected = false,
                        onClick = { onTagSelect(tag) },
                        label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
                // "Create" chip when no exact match
                if (query.isNotBlank() && !exactMatch) {
                    FilterChip(
                        selected = false,
                        onClick = { onCreateTag(query) },
                        label = {
                            Text(
                                "+ Create \"$query\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        },
                        leadingIcon = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = accentColor.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}

private fun findSmsAddress(context: Context, body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("address"),
            "body = ?",
            arrayOf(body),
            "date DESC LIMIT 1"
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) { null }
}
