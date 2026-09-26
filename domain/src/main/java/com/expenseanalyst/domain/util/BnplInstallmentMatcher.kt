package com.expenseanalyst.domain.util

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Links Tabby/Tamara card charges (the instalments) to the purchase they pay for.
 *
 * How BNPL actually shows up (observed on real messages, Sep 2026):
 * - Tabby sends "Your SAR 599.00 purchase at CENTREPOINT is confirmed" — the full price, recorded
 *   as a Split Payments PAYMENT that does NOT count toward Spent.
 * - The card is charged "At: Tabby" for each instalment (149.75 = 599 / 4). Those DO count.
 * - The instalment count varies (Centrepoint 4, IKEA and Magrabi 3), so it is inferred from the
 *   amounts rather than assumed.
 * - The first charge can land a few seconds BEFORE the confirmation, so matching is order-free.
 * - Tamara sends "Split in 3 payment confirmation: Store: … Order: 1,585.75 SAR" — it states the
 *   instalment count, passed as [Purchase.count]. Tamara can also charge several instalments that
 *   fall due together as one amount; such a charge divides by nothing and stays unlinked.
 *
 * A charge links to a purchase from the same provider when its amount is total ÷ N (to within a
 * cent) for some N in [MIN_INSTALMENTS]..[MAX_INSTALMENTS], it falls inside the purchase's
 * instalment window, and the purchase still has an open slot. Existing links are kept as they are.
 */
object BnplInstallmentMatcher {

    const val MIN_INSTALMENTS = 2
    const val MAX_INSTALMENTS = 12
    private const val AMOUNT_TOLERANCE = 0.02
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** First instalment is charged at purchase time; allow a day either side for clock/queue skew. */
    private const val FIRST_CHARGE_SLACK_MS = DAY_MS

    /** Instalments are monthly; give each a generous month plus a week of slack. */
    private const val PER_INSTALMENT_WINDOW_MS = 38L * DAY_MS

    data class Purchase(
        val id: Long,
        val provider: String,
        val shop: String,
        val total: Double,
        val dateMillis: Long,
        /** The instalment count when the confirmation states it (Tamara); null to infer it. */
        val count: Int? = null
    )

    data class Charge(
        val id: Long,
        val provider: String,
        val amount: Double,
        val dateMillis: Long,
        val linkedPurchaseId: Long? = null
    )

    /** One charge's place in its purchase: [number] of [count]. */
    data class Link(val chargeId: Long, val purchaseId: Long, val number: Int, val count: Int)

    /** The instalment count this charge implies for [total], or null if it doesn't divide evenly. */
    fun instalmentCount(total: Double, charge: Double): Int? {
        if (charge <= 0.0 || total <= 0.0) return null
        val n = (total / charge).roundToInt()
        if (n !in MIN_INSTALMENTS..MAX_INSTALMENTS) return null
        return n.takeIf { abs(total / n - charge) <= AMOUNT_TOLERANCE }
    }

    /**
     * Every link, existing ones included, numbered by date within each purchase. Charges that
     * match nothing are simply absent from the result.
     */
    fun match(purchases: List<Purchase>, charges: List<Charge>): List<Link> {
        val byId = purchases.associateBy { it.id }
        val assigned = mutableMapOf<Long, MutableList<Charge>>()   // purchaseId -> charges
        val counts = mutableMapOf<Long, Int>()

        // Keep links that already exist, as long as the purchase is still there.
        charges.filter { it.linkedPurchaseId != null && it.linkedPurchaseId in byId }.forEach { c ->
            val p = byId.getValue(c.linkedPurchaseId!!)
            assigned.getOrPut(p.id) { mutableListOf() } += c
            (p.count ?: instalmentCount(p.total, c.amount))?.let { counts.putIfAbsent(p.id, it) }
        }

        val open = charges.filter { it.linkedPurchaseId == null }.sortedBy { it.dateMillis }
        for (c in open) {
            val candidate = purchases
                .asSequence()
                .filter { it.provider.equals(c.provider, ignoreCase = true) }
                .mapNotNull { p -> instalmentCount(p.total, c.amount)?.let { n -> p to n } }
                .filter { (p, n) -> p.count == null || p.count == n }
                .filter { (p, n) ->
                    counts[p.id]?.let { it == n } ?: true
                } // a purchase's charges all imply the same count
                .filter { (p, n) ->
                    c.dateMillis >= p.dateMillis - FIRST_CHARGE_SLACK_MS &&
                        c.dateMillis <= p.dateMillis + FIRST_CHARGE_SLACK_MS + (n - 1) * PER_INSTALMENT_WINDOW_MS
                }
                .filter { (p, n) -> (assigned[p.id]?.size ?: 0) < n }
                .sortedBy { (p, _) -> abs(c.dateMillis - p.dateMillis) }
                .firstOrNull() ?: continue
            val (p, n) = candidate
            assigned.getOrPut(p.id) { mutableListOf() } += c
            counts.putIfAbsent(p.id, n)
        }

        return assigned.flatMap { (purchaseId, list) ->
            val count = counts[purchaseId] ?: return@flatMap emptyList()
            list.sortedBy { it.dateMillis }.mapIndexed { i, c -> Link(c.id, purchaseId, i + 1, count) }
        }
    }
}
