package com.expenseanalyst.feature.settings.ui

import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.model.Expense

data class AccountManagementUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingAccount: Account? = null,
    val dialogBankName: String = "",
    val dialogLastFour: String = "",
    val dialogAccountType: AccountType = AccountType.SAVINGS,
    val isSaving: Boolean = false,
    // Expense count shown in edit dialog
    val editingAccountExpenseCount: Int = 0,
    // Delete-with-remap dialog
    val showDeleteDialog: Boolean = false,
    val deletingAccount: Account? = null,
    val deletingAccountExpenses: List<Expense> = emptyList(),
    val remapTargetAccountId: Long? = null   // null = unassign (set account_id to null)
)
