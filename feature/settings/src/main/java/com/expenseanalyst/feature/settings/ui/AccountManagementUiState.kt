package com.expenseanalyst.feature.settings.ui

import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.model.AccountType

data class AccountManagementUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingAccount: Account? = null,
    val dialogBankName: String = "",
    val dialogLastFour: String = "",
    val dialogAccountType: AccountType = AccountType.SAVINGS,
    val isSaving: Boolean = false
)
