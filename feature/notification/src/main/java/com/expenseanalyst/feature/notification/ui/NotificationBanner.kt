package com.expenseanalyst.feature.notification.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.feature.notification.parser.ParsedTransaction
import com.expenseanalyst.feature.notification.parser.TransactionDirection
import java.text.NumberFormat
import java.util.Locale

/**
 * Floating banner shown at the top of the expense list when a transaction
 * is auto-detected. The user can tap "Save" (opens pre-filled Add Expense)
 * or dismiss.
 */
@Composable
fun NotificationBanner(
    onSave: (ParsedTransaction, pendingId: Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationBannerViewModel = hiltViewModel()
) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val lastPendingId by viewModel.lastPendingId.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = pending != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        pending?.let { tx ->
            BannerContent(
                transaction = tx,
                onSave = {
                    viewModel.consume()
                    onSave(tx, lastPendingId)
                },
                onDismiss = viewModel::dismiss
            )
        }
    }
}

@Composable
private fun BannerContent(
    transaction: ParsedTransaction,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onSave)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = buildLabel(transaction),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Tap to save • ${transaction.bankName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        TextButton(onClick = onSave) {
            Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun buildLabel(tx: ParsedTransaction): String {
    val direction = when (tx.type) {
        TransactionDirection.DEBIT -> "spent"
        TransactionDirection.TRANSFER -> "transferred"
        else -> "received"
    }
    val amount = try {
        val fmt = NumberFormat.getInstance(Locale.US)
        fmt.maximumFractionDigits = 2
        fmt.minimumFractionDigits = 2
        "${tx.currencyCode} ${fmt.format(tx.amount)}"
    } catch (e: Exception) {
        "${tx.currencyCode} ${tx.amount}"
    }
    return if (tx.merchant != null) "You $direction $amount at ${tx.merchant}"
    else "You $direction $amount"
}
