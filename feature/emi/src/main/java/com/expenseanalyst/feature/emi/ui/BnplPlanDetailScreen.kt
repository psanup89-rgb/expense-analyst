package com.expenseanalyst.feature.emi.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expenseanalyst.core.util.CurrencyFormatter
import com.expenseanalyst.core.util.DateTimeUtil
import com.expenseanalyst.domain.util.BnplPayment
import com.expenseanalyst.domain.util.BnplPlan

/**
 * One Tabby/Tamara plan: how much is paid and left, and the card charges that paid it. A charge
 * linked by hand (one combining several plans' instalments) can be unlinked here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BnplPlanDetailScreen(
    onBack: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: BnplPlanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val plan = uiState.plan

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        plan?.let { "${it.shop} · ${it.provider}" } ?: "Plan",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            plan == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This plan no longer exists", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { PlanSummary(plan) }
                item {
                    Text(
                        "Payments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (plan.payments.isEmpty()) {
                    item {
                        Text(
                            "No card charges linked yet. They link automatically when the amount is an even " +
                                "share of the total; a charge combining several plans is offered in Review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(plan.payments, key = { it.charge.id }) { payment ->
                    PaymentRow(
                        payment = payment,
                        currencyCode = plan.currencyCode,
                        onClick = { onExpenseClick(payment.charge.id) },
                        onUnlink = { viewModel.unlink(payment.charge.id) }
                    )
                    HorizontalDivider()
                }
                item {
                    OutlinedButton(
                        onClick = { onExpenseClick(plan.purchase.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("View purchase") }
                }
            }
        }
    }
}

@Composable
private fun PlanSummary(plan: BnplPlan) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            CurrencyFormatter.format(plan.total, plan.currencyCode),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            "Bought ${DateTimeUtil.formatDateHeader(plan.purchase.date)}" +
                (plan.instalmentAmount?.let { " · ${plan.instalmentCount} × ${CurrencyFormatter.format(it, plan.currencyCode)}" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                plan.instalmentCount?.let { "${plan.paidCount}/$it paid" } ?: "${plan.paidCount} paid",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                if (plan.isComplete) "Paid off" else "${CurrencyFormatter.format(plan.remaining, plan.currencyCode)} left",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { if (plan.total > 0) (plan.paidAmount / plan.total).toFloat().coerceIn(0f, 1f) else 0f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun PaymentRow(payment: BnplPayment, currencyCode: String, onClick: () -> Unit, onUnlink: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(DateTimeUtil.formatDateHeader(payment.charge.date), style = MaterialTheme.typography.bodyMedium)
            if (payment.isCombined) {
                Text(
                    "Part of a ${CurrencyFormatter.format(payment.charge.amount, payment.charge.currencyCode)} charge",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            CurrencyFormatter.format(payment.amount, currencyCode),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (payment.isCombined) {
            TextButton(onClick = onUnlink) { Text("Unlink") }
        }
    }
}
