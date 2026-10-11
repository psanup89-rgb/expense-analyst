package com.expenseanalyst.feature.expenses.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.util.BnplPlan
import com.expenseanalyst.domain.util.BnplPlans
import kotlin.math.abs

/**
 * "Which plans does this charge pay?" for a Tabby/Tamara card charge no plan matched — usually
 * Tamara billing instalments of several plans as one charge. Each ticked plan gets one instalment
 * paid. A combination whose instalments add up to the charge is pre-ticked; the user decides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BnplLinkSheet(
    charge: Expense,
    plans: List<BnplPlan>,
    onLink: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val candidates = remember(charge.id, plans) { BnplPlans.candidatesFor(charge, plans) }
    var ticked by remember(charge.id, candidates) {
        mutableStateOf(BnplPlans.suggest(charge.amount, candidates))
    }
    val tickedTotal = candidates.filter { it.purchase.id in ticked }.sumOf { it.instalmentAmount ?: 0.0 }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            Text(
                text = "Which plans does this pay?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Text(
                text = "${CurrencyFormatter.format(charge.amount, charge.currencyCode)} to ${charge.merchantName} on " +
                    "${DateTimeUtil.formatDateHeader(charge.date)}. Each plan you tick gets one instalment paid. " +
                    "Your spending totals don't change.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp)
            )

            if (candidates.isEmpty()) {
                Text(
                    text = "No open ${charge.merchantName} plans bought before this charge. If it pays for a " +
                        "purchase from before the app's history, mark it done from Review.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                return@Column
            }

            candidates.forEach { plan ->
                val isTicked = plan.purchase.id in ticked
                val toggle = { ticked = if (isTicked) ticked - plan.purchase.id else ticked + plan.purchase.id }
                ListItem(
                    headlineContent = { Text(plan.shop, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        val count = plan.instalmentCount?.let { "${plan.paidCount}/$it paid" } ?: "${plan.paidCount} paid"
                        val size = plan.instalmentAmount?.let { CurrencyFormatter.format(it, plan.currencyCode) + " each · " }.orEmpty()
                        Text("$size$count · bought ${DateTimeUtil.formatDateHeader(plan.purchase.date)}")
                    },
                    leadingContent = { Checkbox(checked = isTicked, onCheckedChange = { toggle() }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = toggle)
                )
            }

            Spacer(Modifier.height(8.dp))
            if (ticked.isNotEmpty()) {
                val matches = abs(tickedTotal - charge.amount) <= BnplPlans.TOLERANCE
                Text(
                    text = "Ticked instalments: ${CurrencyFormatter.format(tickedTotal, charge.currencyCode)}" +
                        if (matches) " — matches the charge" else " — the charge is ${CurrencyFormatter.format(charge.amount, charge.currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            Button(
                onClick = { onLink(ticked.toList()) },
                enabled = ticked.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) { Text("Link to ${ticked.size} plan${if (ticked.size == 1) "" else "s"}") }
        }
    }
}
