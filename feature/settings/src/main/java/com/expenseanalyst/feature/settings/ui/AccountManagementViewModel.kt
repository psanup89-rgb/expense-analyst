package com.expenseanalyst.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.model.AccountType
import com.expenseanalyst.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _form = MutableStateFlow(AccountManagementUiState())

    val uiState = combine(accountRepository.getAccounts(), _form) { accounts, state ->
        state.copy(accounts = accounts.sortedBy { it.bankName }, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountManagementUiState()
    )

    fun showAddDialog() {
        _form.update {
            it.copy(
                showAddDialog = true,
                showEditDialog = false,
                editingAccount = null,
                dialogBankName = "",
                dialogLastFour = "",
                dialogAccountType = AccountType.SAVINGS
            )
        }
    }

    fun showEditDialog(account: Account) {
        _form.update {
            it.copy(
                showEditDialog = true,
                showAddDialog = false,
                editingAccount = account,
                dialogBankName = account.bankName,
                dialogLastFour = account.lastFour ?: "",
                dialogAccountType = account.accountType
            )
        }
    }

    fun dismissDialog() {
        _form.update {
            it.copy(
                showAddDialog = false,
                showEditDialog = false,
                editingAccount = null,
                dialogBankName = "",
                dialogLastFour = ""
            )
        }
    }

    fun onBankNameChange(value: String) = _form.update { it.copy(dialogBankName = value) }

    fun onLastFourChange(value: String) =
        _form.update { it.copy(dialogLastFour = value.filter { c -> c.isDigit() }.take(4)) }

    fun onAccountTypeChange(type: AccountType) = _form.update { it.copy(dialogAccountType = type) }

    fun saveAccount() {
        val s = _form.value
        val bankName = s.dialogBankName.trim()
        if (bankName.isBlank()) return
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            val lastFour = s.dialogLastFour.ifEmpty { null }
            val displayName = buildDisplayName(bankName, lastFour, s.dialogAccountType)
            runCatching {
                if (s.showEditDialog && s.editingAccount != null) {
                    accountRepository.updateAccount(
                        s.editingAccount.copy(
                            bankName = bankName,
                            lastFour = lastFour,
                            accountType = s.dialogAccountType,
                            displayName = displayName
                        )
                    )
                } else {
                    accountRepository.addAccount(
                        Account(
                            bankName = bankName,
                            lastFour = lastFour,
                            accountType = s.dialogAccountType,
                            displayName = displayName
                        )
                    )
                }
            }
            _form.update {
                it.copy(
                    isSaving = false,
                    showAddDialog = false,
                    showEditDialog = false,
                    editingAccount = null,
                    dialogBankName = "",
                    dialogLastFour = ""
                )
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch { accountRepository.deleteAccount(account.id) }
    }

    private fun buildDisplayName(bankName: String, lastFour: String?, type: AccountType): String =
        if (lastFour != null) "$bankName *$lastFour · ${type.label}"
        else "$bankName · ${type.label}"
}
