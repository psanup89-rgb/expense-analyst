package com.expenseanalyst.feature.emi.ui

import com.expenseanalyst.domain.model.EmiGroup
import com.expenseanalyst.domain.util.BnplPlan

data class EmiListUiState(
    val activeGroups: List<EmiGroup> = emptyList(),
    val completedGroups: List<EmiGroup> = emptyList(),
    /** Tabby/Tamara purchases, shown as the same cards as EMIs (owner's choice, Oct 2026). */
    val activeBnpl: List<BnplPlan> = emptyList(),
    val completedBnpl: List<BnplPlan> = emptyList(),
    val isLoading: Boolean = true,
    val showCompleted: Boolean = false
) {
    val activeCount: Int get() = activeGroups.size + activeBnpl.size
    val completedCount: Int get() = completedGroups.size + completedBnpl.size

    /** EMIs and BNPL plans in one list, newest first. */
    val displayItems: List<EmiListItem>
        get() {
            val groups = if (showCompleted) completedGroups else activeGroups
            val plans = if (showCompleted) completedBnpl else activeBnpl
            return (groups.map { EmiListItem.Emi(it) } + plans.map { EmiListItem.Bnpl(it) })
                .sortedByDescending { it.startMillis }
        }
}

sealed interface EmiListItem {
    val key: String
    val startMillis: Long

    data class Emi(val group: EmiGroup) : EmiListItem {
        override val key = "emi-${group.id}"
        override val startMillis = group.startDate.toEpochMilliseconds()
    }

    data class Bnpl(val plan: BnplPlan) : EmiListItem {
        override val key = "bnpl-${plan.purchase.id}"
        override val startMillis = plan.purchase.date.toEpochMilliseconds()
    }
}
