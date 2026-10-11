package com.expenseanalyst.domain.util

import com.expenseanalyst.domain.model.BnplChargeLink
import com.expenseanalyst.domain.model.Expense
import com.expenseanalyst.domain.model.TransactionType
import kotlin.math.abs

/**
 * One Tabby/Tamara purchase as an instalment plan, for the EMIs tab (shown as EMI-style cards —
 * owner's choice, Oct 2026).
 *
 * The purchase is the Split Payments PAYMENT record BnplReconciler stamps with a provider; the
 * payments are the card charges that pay it — matched automatically ([Expense.bnplPurchaseId]) or
 * linked by hand ([BnplChargeLink], a charge combining instalments of several plans). Nothing here
 * changes a total: the charges count toward Spent as they always did; this only shows progress.
 */
data class BnplPlan(
    val purchase: Expense,
    val provider: String,
    /** Null when neither stated nor inferable yet (no evenly dividing charge linked). */
    val instalmentCount: Int?,
    val payments: List<BnplPayment>
) {
    val total: Double get() = purchase.amount
    val currencyCode: String get() = purchase.currencyCode
    val shop: String get() = purchase.merchantName?.takeIf { it.isNotBlank() } ?: provider
    val instalmentAmount: Double? get() = instalmentCount?.let { total / it }
    val paidCount: Int get() = payments.size
    val paidAmount: Double get() = payments.sumOf { it.amount }
    val remaining: Double get() = (total - paidAmount).coerceAtLeast(0.0)
    val isComplete: Boolean
        get() = (instalmentCount != null && paidCount >= instalmentCount) || remaining <= BnplPlans.TOLERANCE
}

/**
 * One charge's part in a plan. A charge linked to several plans pays one instalment of each, so
 * [amount] is that plan's instalment, not the whole charge.
 */
data class BnplPayment(val charge: Expense, val amount: Double, val isCombined: Boolean)

object BnplPlans {

    const val TOLERANCE = 0.05
    val PROVIDERS = listOf("Tabby", "Tamara")

    fun isPurchase(e: Expense): Boolean =
        !e.isDeleted && e.transactionType == TransactionType.PAYMENT && e.bnplProvider != null

    /** A card charge to Tabby/Tamara itself — the shape BnplReconciler matches (and renames once matched). */
    fun providerOfCharge(e: Expense): String? =
        if (e.transactionType != TransactionType.EXPENSE) null
        else PROVIDERS.firstOrNull { it.equals(e.merchantName?.trim(), ignoreCase = true) }

    fun build(expenses: List<Expense>, links: List<BnplChargeLink>): List<BnplPlan> {
        val live = expenses.filter { !it.isDeleted }
        val byId = live.associateBy { it.id }
        val purchases = live.filter(::isPurchase)
        val purchaseIds = purchases.mapTo(mutableSetOf()) { it.id }
        val manual = links.filter { it.purchaseId in purchaseIds && it.chargeId in byId }
        val plansPerCharge = manual.groupingBy { it.chargeId }.eachCount()
        val manualByPurchase = manual.groupBy { it.purchaseId }
        val directByPurchase = live.filter { it.bnplPurchaseId in purchaseIds }.groupBy { it.bnplPurchaseId!! }

        return purchases.map { p ->
            val direct = directByPurchase[p.id].orEmpty()
            val count = p.bnplInstalmentCount
                ?: direct.firstNotNullOfOrNull { BnplInstallmentMatcher.instalmentCount(p.amount, it.amount) }
            val payments = direct.map { BnplPayment(it, it.amount, isCombined = false) } +
                manualByPurchase[p.id].orEmpty().mapNotNull { link ->
                    val charge = byId[link.chargeId] ?: return@mapNotNull null
                    val share = count?.let { p.amount / it }
                        ?: (charge.amount / (plansPerCharge[link.chargeId] ?: 1))
                    BnplPayment(charge, share, isCombined = true)
                }
            BnplPlan(p, p.bnplProvider!!, count, payments.sortedBy { it.charge.date })
        }.sortedByDescending { it.purchase.date }
    }

    /**
     * Plans a charge could pay: same provider and currency, bought by the time of the charge (a
     * day's slack — the first charge can land before the confirmation), and not yet complete.
     */
    fun candidatesFor(charge: Expense, plans: List<BnplPlan>): List<BnplPlan> {
        val provider = providerOfCharge(charge) ?: return emptyList()
        val chargeMillis = charge.date.toEpochMilliseconds()
        return plans.filter {
            it.provider.equals(provider, ignoreCase = true) &&
                it.currencyCode == charge.currencyCode &&
                !it.isComplete &&
                it.purchase.date.toEpochMilliseconds() <= chargeMillis + DAY_MS
        }
    }

    /**
     * The plans whose next instalments add up to [chargeAmount], to pre-tick in the link sheet, or
     * empty when no combination fits. Fewest plans wins; ties go to the older plans, whose
     * instalments fall due first. Plans with an unknown instalment size can't take part.
     */
    fun suggest(chargeAmount: Double, candidates: List<BnplPlan>): Set<Long> {
        val sized = candidates.filter { it.instalmentAmount != null }.take(MAX_SEARCH)
        var best: List<BnplPlan>? = null
        for (mask in 1 until (1 shl sized.size)) {
            val picked = sized.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            if (abs(picked.sumOf { it.instalmentAmount!! } - chargeAmount) > TOLERANCE) continue
            val current = best
            if (current == null || picked.size < current.size ||
                (picked.size == current.size && oldest(picked) < oldest(current))
            ) {
                best = picked
            }
        }
        return best.orEmpty().mapTo(mutableSetOf()) { it.purchase.id }
    }

    private fun oldest(plans: List<BnplPlan>): Long = plans.sumOf { it.purchase.date.toEpochMilliseconds() }

    private const val DAY_MS = 24L * 60 * 60 * 1000
    /** 2^12 subsets at most; a user never has more open plans with one provider than this. */
    private const val MAX_SEARCH = 12
}
