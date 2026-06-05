package com.expenseanalyst.feature.loans.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanScreen(
    loanId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddLoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (loanId != null) "Edit Loan" else "Add Loan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.personName,
                onValueChange = viewModel::onPersonNameChange,
                label = { Text("Person name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.currencyCode,
                    onValueChange = viewModel::onCurrencyChange,
                    label = { Text("Currency") },
                    modifier = Modifier.weight(0.4f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(uiState.lentDateMillis))
            OutlinedTextField(
                value = dateStr,
                onValueChange = {},
                label = { Text("Lent date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = viewModel::showDatePicker) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                    }
                }
            )

            val reminderStr = uiState.reminderDatetimeMillis?.let {
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(it))
            } ?: "No reminder set"
            OutlinedTextField(
                value = reminderStr,
                onValueChange = {},
                label = { Text("Reminder") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    Row {
                        IconButton(onClick = viewModel::showReminderPicker) {
                            Icon(Icons.Default.Alarm, contentDescription = "Set reminder")
                        }
                        if (uiState.reminderDatetimeMillis != null) {
                            IconButton(onClick = { viewModel.onReminderSelected(null) }) {
                                Icon(Icons.Default.AlarmOff, contentDescription = "Clear reminder")
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                enabled = uiState.isValid && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                else Text("Save")
            }
        }

        if (uiState.showDatePicker) {
            val dateState = rememberDatePickerState(initialSelectedDateMillis = uiState.lentDateMillis)
            DatePickerDialog(
                onDismissRequest = viewModel::hideDatePicker,
                confirmButton = {
                    TextButton(onClick = {
                        dateState.selectedDateMillis?.let { viewModel.onDateSelected(it) }
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = viewModel::hideDatePicker) { Text("Cancel") } }
            ) { DatePicker(state = dateState) }
        }

        if (uiState.showReminderPicker) {
            val dateState = rememberDatePickerState(
                initialSelectedDateMillis = uiState.reminderDatetimeMillis ?: System.currentTimeMillis()
            )
            DatePickerDialog(
                onDismissRequest = viewModel::hideReminderPicker,
                confirmButton = {
                    TextButton(onClick = {
                        dateState.selectedDateMillis?.let { viewModel.onReminderSelected(it) }
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = viewModel::hideReminderPicker) { Text("Cancel") } }
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Pick reminder date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    DatePicker(state = dateState)
                }
            }
        }
    }
}
