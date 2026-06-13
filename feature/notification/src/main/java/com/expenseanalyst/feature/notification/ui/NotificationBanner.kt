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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RateReview
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.feature.notification.service.AutoSavedEvent
import java.text.NumberFormat
import java.util.Locale

/**
 * Floating banner shown at the top of the expense list when a transaction is
 * auto-saved. The user can tap "Edit" to review it or dismiss the banner.
 */
@Composable
fun NotificationBanner(
    onEdit: (expenseId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationBannerViewModel = hiltViewModel()
) {
    val event by viewModel.lastAutoSaved.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = event != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        event?.let { saved ->
            BannerContent(
                event = saved,
                onEdit = {
                    viewModel.consume()
                    onEdit(saved.expenseId)
                },
                onDismiss = viewModel::dismiss
            )
        }
    }
}

@Composable
private fun BannerContent(
    event: AutoSavedEvent,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                if (event.needsReview) Icons.Default.RateReview else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (event.needsReview) Color(0xFFF57C00) else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = buildSavedLabel(event),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = if (event.needsReview) "Needs review — tap to edit" else "Saved · tap to review",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (event.needsReview)
                        Color(0xFFF57C00)
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        TextButton(onClick = onEdit) {
            Text(
                "Edit",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
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

private fun buildSavedLabel(event: AutoSavedEvent): String {
    val amount = try {
        val fmt = NumberFormat.getInstance(Locale.US)
        fmt.maximumFractionDigits = 2
        fmt.minimumFractionDigits = 2
        "${event.currencyCode} ${fmt.format(event.amount)}"
    } catch (e: Exception) {
        "${event.currencyCode} ${event.amount}"
    }
    return if (event.merchant != null) "Saved $amount at ${event.merchant}"
    else "Saved $amount"
}
