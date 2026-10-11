package com.expenseanalyst.feature.emi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.expenseanalyst.domain.model.EmiGroup
import com.expenseanalyst.domain.util.BnplPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiListScreen(
    onNavigateToDetail: (Long) -> Unit,
    /** A Tabby/Tamara plan, by its purchase record's id. */
    onBnplPlanClick: (Long) -> Unit = {},
    viewModel: EmiListViewModel = hiltViewModel(),
    /** Inside the Dues tab: the host draws the title and tabs, so no top bar. */
    embedded: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (!embedded) TopAppBar(
                title = { Text("EMI Tracker", fontWeight = FontWeight.Bold) },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val displayItems = uiState.displayItems
        val isEmpty = displayItems.isEmpty()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !uiState.showCompleted,
                        onClick = { if (uiState.showCompleted) viewModel.toggleShowCompleted() },
                        label = { Text("Active (${uiState.activeCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = uiState.showCompleted,
                        onClick = { if (!uiState.showCompleted) viewModel.toggleShowCompleted() },
                        label = { Text("Completed (${uiState.completedCount})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            if (isEmpty) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (uiState.showCompleted) "No completed EMIs" else "No active EMIs",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (uiState.showCompleted) "Completed EMIs will appear here."
                                else "Convert an expense into EMI from the expense detail screen. " +
                                    "Tabby and Tamara purchases appear here automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(displayItems, key = { it.key }) { item ->
                    when (item) {
                        is EmiListItem.Emi -> EmiGroupCard(group = item.group, onClick = { onNavigateToDetail(item.group.id) })
                        is EmiListItem.Bnpl -> BnplPlanCard(plan = item.plan, onClick = { onBnplPlanClick(item.plan.purchase.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmiGroupCard(group: EmiGroup, onClick: () -> Unit) {
    PlanCard(
        title = group.description,
        subtitle = group.category.name,
        label = null,
        total = group.totalAmount,
        instalment = group.installmentAmount,
        currencyCode = group.currencyCode,
        paidCount = group.paidCount,
        instalmentCount = group.numberOfInstallments,
        remainingText = "${group.numberOfInstallments - group.paidCount} remaining",
        isCompleted = group.paidCount >= group.numberOfInstallments,
        note = group.interestRate?.takeIf { it > 0 }?.let { "@ $it% p.a." },
        startedText = "Started ${DateTimeUtil.formatDateHeader(group.startDate)}",
        onClick = onClick
    )
}

/**
 * A Tabby/Tamara purchase as an EMI-style card. Progress comes from the card charges linked to it
 * (automatically, or by hand from Review); the remaining figure is money, since a plan whose count
 * isn't known yet can still show what's left.
 */
@Composable
private fun BnplPlanCard(plan: BnplPlan, onClick: () -> Unit) {
    PlanCard(
        title = plan.shop,
        subtitle = plan.payments.firstOrNull()?.charge?.category?.name ?: "Split Payments",
        label = plan.provider,
        total = plan.total,
        instalment = plan.instalmentAmount,
        currencyCode = plan.currencyCode,
        paidCount = plan.paidCount,
        instalmentCount = plan.instalmentCount,
        remainingText = "${CurrencyFormatter.format(plan.remaining, plan.currencyCode)} left",
        isCompleted = plan.isComplete,
        note = null,
        startedText = "Bought ${DateTimeUtil.formatDateHeader(plan.purchase.date)}",
        onClick = onClick
    )
}

@Composable
private fun PlanCard(
    title: String,
    subtitle: String,
    /** Provider tag for a BNPL plan ("Tabby"); null for an EMI. */
    label: String?,
    total: Double,
    instalment: Double?,
    currencyCode: String,
    paidCount: Int,
    instalmentCount: Int?,
    remainingText: String,
    isCompleted: Boolean,
    note: String?,
    startedText: String,
    onClick: () -> Unit
) {
    val progress = when {
        isCompleted -> 1f
        instalmentCount != null && instalmentCount > 0 -> paidCount.toFloat() / instalmentCount.toFloat()
        else -> 0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (label != null) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(total, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Installment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = instalment?.let { CurrencyFormatter.format(it, currencyCode) } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = instalmentCount?.let { "$paidCount/$it paid" } ?: "$paidCount paid",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCompleted) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(visible = !isCompleted) {
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (isCompleted) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            note?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = startedText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
