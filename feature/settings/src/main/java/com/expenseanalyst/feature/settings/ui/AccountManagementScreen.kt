package com.expenseanalyst.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.domain.model.Account
import com.expenseanalyst.domain.model.AccountType

@Composable
fun AccountManagementScreen(
    onBack: () -> Unit,
    viewModel: AccountManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AccountManagementContent(
        uiState = uiState,
        onBack = onBack,
        onAddClick = viewModel::showAddDialog,
        onEditClick = viewModel::showEditDialog,
        onDeleteClick = viewModel::showDeleteDialog,
        onDismissDialog = viewModel::dismissDialog,
        onBankNameChange = viewModel::onBankNameChange,
        onLastFourChange = viewModel::onLastFourChange,
        onAccountTypeChange = viewModel::onAccountTypeChange,
        onSave = viewModel::saveAccount,
        onRemapTargetSelected = viewModel::onRemapTargetSelected,
        onConfirmDelete = viewModel::confirmDelete
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountManagementContent(
    uiState: AccountManagementUiState,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Account) -> Unit,
    onDeleteClick: (Account) -> Unit,
    onDismissDialog: () -> Unit,
    onBankNameChange: (String) -> Unit,
    onLastFourChange: (String) -> Unit,
    onAccountTypeChange: (AccountType) -> Unit,
    onSave: () -> Unit,
    onRemapTargetSelected: (Long?) -> Unit,
    onConfirmDelete: () -> Unit
) {
    if (uiState.showAddDialog || uiState.showEditDialog) {
        AccountDialog(
            isEdit = uiState.showEditDialog,
            bankName = uiState.dialogBankName,
            lastFour = uiState.dialogLastFour,
            accountType = uiState.dialogAccountType,
            expenseCount = if (uiState.showEditDialog) uiState.editingAccountExpenseCount else 0,
            isSaving = uiState.isSaving,
            onBankNameChange = onBankNameChange,
            onLastFourChange = onLastFourChange,
            onAccountTypeChange = onAccountTypeChange,
            onDismiss = onDismissDialog,
            onConfirm = onSave
        )
    }

    if (uiState.showDeleteDialog && uiState.deletingAccount != null) {
        DeleteRemapDialog(
            account = uiState.deletingAccount,
            expenseCount = uiState.deletingAccountExpenseCount,
            otherAccounts = uiState.accounts.filter { it.id != uiState.deletingAccount.id },
            remapTargetId = uiState.remapTargetAccountId,
            isSaving = uiState.isSaving,
            onRemapTargetSelected = onRemapTargetSelected,
            onDismiss = onDismissDialog,
            onConfirm = onConfirmDelete
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Manage Accounts", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets(0, 0, 0, 0),
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.accounts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No accounts yet. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        onEdit = { onEditClick(account) },
                        onDelete = { onDeleteClick(account) }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteRemapDialog(
    account: Account,
    expenseCount: Int,
    otherAccounts: List<Account>,
    remapTargetId: Long?,
    isSaving: Boolean,
    onRemapTargetSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Label for currently selected remap target
    val selectedLabel = when {
        remapTargetId == null -> "Unassign (no account)"
        else -> otherAccounts.find { it.id == remapTargetId }?.displayName ?: "Select account"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${account.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (expenseCount > 0) {
                    Text(
                        text = "$expenseCount expense${if (expenseCount != 1) "s are" else " is"} linked to this account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Remap them to:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unassign (no account)") },
                                onClick = {
                                    onRemapTargetSelected(null)
                                    dropdownExpanded = false
                                }
                            )
                            otherAccounts.forEach { target ->
                                DropdownMenuItem(
                                    text = { Text(target.displayName) },
                                    onClick = {
                                        onRemapTargetSelected(target.id)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No expenses are linked to this account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AccountRow(
    account: Account,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = account.accountType.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AccountDialog(
    isEdit: Boolean,
    bankName: String,
    lastFour: String,
    accountType: AccountType,
    expenseCount: Int,
    isSaving: Boolean,
    onBankNameChange: (String) -> Unit,
    onLastFourChange: (String) -> Unit,
    onAccountTypeChange: (AccountType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Account" else "Add Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEdit && expenseCount > 0) {
                    Text(
                        text = "$expenseCount expense${if (expenseCount != 1) "s" else ""} linked to this account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = bankName,
                    onValueChange = onBankNameChange,
                    label = { Text("Bank / Wallet name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                OutlinedTextField(
                    value = lastFour,
                    onValueChange = onLastFourChange,
                    label = { Text("Last 4 digits (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    "Account type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AccountType.entries) { type ->
                        FilterChip(
                            selected = accountType == type,
                            onClick = { onAccountTypeChange(type) },
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
            TextButton(
                onClick = onConfirm,
                enabled = bankName.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEdit) "Save" else "Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
