package com.expenseanalyst.feature.notification.ui

import com.expenseanalyst.feature.notification.parser.ParsedTransaction

data class SmsMessage(
    val id: Long,
    val sender: String,
    val body: String,
    val timestampMs: Long
)

sealed interface SmsImportUiState {
    /** Initial state — user chooses between bulk import or manual browse. */
    data object ChooseMode : SmsImportUiState

    data object PermissionDenied : SmsImportUiState
    data object LoadingSms : SmsImportUiState
    data class SmsList(val messages: List<SmsMessage>) : SmsImportUiState
    data object Parsing : SmsImportUiState
    data class ParseSuccess(val parsed: ParsedTransaction, val rawBody: String) : SmsImportUiState
    data class ParseFailed(val rawBody: String) : SmsImportUiState
    data class NavigateToAddExpense(val parsed: ParsedTransaction) : SmsImportUiState

    /** Bulk import is running. [total] may be 0 while still reading SMS. */
    data class BulkImporting(val processed: Int, val total: Int) : SmsImportUiState

    /** Bulk import finished. */
    data class BulkImportDone(
        val totalScanned: Int,
        val saved: Int,
        val skipped: Int,
        val failed: Int
    ) : SmsImportUiState
}

enum class ImportAction { BULK_LAST_MONTH, BULK_ALL, BROWSE }
