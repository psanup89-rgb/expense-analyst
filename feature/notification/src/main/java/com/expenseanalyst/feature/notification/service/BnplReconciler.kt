package com.expenseanalyst.feature.notification.service

import com.expenseanalyst.domain.model.TransactionType
import com.expenseanalyst.domain.repository.CategoryRepository
import com.expenseanalyst.domain.repository.ExpenseRepository
import com.expenseanalyst.domain.repository.MerchantRuleRepository
import com.expenseanalyst.domain.util.BnplInstallmentMatcher
import com.expenseanalyst.domain.util.CategoryInference
import com.expenseanalyst.feature.notification.parser.TabbyTamaraParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps Tabby/Tamara purchases counted once. Chosen treatment ("A"): the card charges that
 * actually leave the account count toward Spent; the purchase confirmation is a Split Payments
 * record (shop + total) that doesn't count. Two passes, both idempotent, so this runs after every
 * capture, after a bulk import, and once at startup to repair history:
 *
 * 1. A confirmation saved as an ordinary EXPENSE — every one before the parser learned Tabby's real
 *    wording, found by re-parsing its stored SMS — becomes a Split Payments PAYMENT with the shop
 *    name fixed ("CENTREPOINT is confirmed" → "CENTREPOINT").
 * 2. Card charges to Tabby/Tamara are linked to their purchase by [BnplInstallmentMatcher] and
 *    renamed "Centrepoint 2/4 · Tabby", taking the shop's category. Unmatched charges (purchases
 *    older than the app's history, or Tamara charges that combine several due instalments) stay
 *    as they are.
 *
 * Works only on stored, parsed fields; the raw SMS is re-read by the parser and never surfaced.
 */
@Singleton
class BnplReconciler @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val merchantRuleRepository: MerchantRuleRepository
) {
    private val parser = TabbyTamaraParser()
    private val lock = Mutex()

    suspend fun reconcile() = lock.withLock {
        val categories = categoryRepository.getCategories().first()
        val splitPayments = categories.find { it.name == SPLIT_PAYMENTS } ?: return@withLock
        var rows = expenseRepository.getExpensesSnapshot()

        // ── Pass 1: mis-saved confirmations → Split Payments records ──
        var converted = false
        rows.filter { it.transactionType == TransactionType.EXPENSE && it.bnplPurchaseId == null }
            .forEach { e ->
                val body = e.rawSmsBody ?: return@forEach
                val sender = e.sourceSender.orEmpty()
                if (!parser.canParse(sender, body)) return@forEach
                val parsed = parser.parse(sender, body)?.takeIf { it.isBnplConfirmation } ?: return@forEach
                expenseRepository.convertToBnplPurchase(
                    e.id, splitPayments.id, parsed.merchant ?: e.merchantName.orEmpty()
                )
                converted = true
            }
        if (converted) rows = expenseRepository.getExpensesSnapshot()

        // ── Pass 2: link instalments to purchases ──
        val purchases = rows.mapNotNull { e ->
            if (e.transactionType != TransactionType.PAYMENT || e.category.id != splitPayments.id) return@mapNotNull null
            val provider = providerOf(e.sourceSender, e.rawSmsBody) ?: return@mapNotNull null
            BnplInstallmentMatcher.Purchase(
                id = e.id,
                provider = provider,
                shop = e.merchantName ?: provider,
                total = e.amount,
                dateMillis = e.date.toEpochMilliseconds(),
                count = e.rawSmsBody?.let { parser.instalmentCountOf(it) }
            )
        }
        if (purchases.isEmpty()) return@withLock
        val purchaseById = purchases.associateBy { it.id }

        val charges = rows.mapNotNull { e ->
            if (e.transactionType != TransactionType.EXPENSE) return@mapNotNull null
            val provider = e.bnplPurchaseId?.let { purchaseById[it]?.provider }
                ?: PROVIDERS.firstOrNull { it.equals(e.merchantName?.trim(), ignoreCase = true) }
                ?: return@mapNotNull null
            BnplInstallmentMatcher.Charge(
                id = e.id,
                provider = provider,
                amount = e.amount,
                dateMillis = e.date.toEpochMilliseconds(),
                linkedPurchaseId = e.bnplPurchaseId
            )
        }
        val rowById = rows.associateBy { it.id }
        val rules = merchantRuleRepository.getRules().first()

        BnplInstallmentMatcher.match(purchases, charges).forEach { link ->
            val purchase = purchaseById.getValue(link.purchaseId)
            val charge = rowById[link.chargeId] ?: return@forEach
            val shop = NotificationCopy.displayMerchant(purchase.shop) ?: purchase.shop
            val name = "$shop ${link.number}/${link.count} · ${purchase.provider}"   // count first: survives truncation
            // The shop's own category (Centrepoint → Shopping); fall back to what the charge has.
            val category = CategoryInference.infer(shop, purchase.provider, categories, merchantRules = rules)
                ?.takeIf { it.id != splitPayments.id }
                ?: charge.category
            if (charge.bnplPurchaseId != purchase.id || charge.merchantName != name || charge.category.id != category.id) {
                expenseRepository.linkBnplInstalment(charge.id, purchase.id, name, category.id)
            }
        }
    }

    // Tamara's confirmation never names Tamara in its text, so ask the parser first; it knows
    // the message shape. Fall back to the name appearing anywhere (Tabby's wording does).
    private fun providerOf(sender: String?, body: String?): String? {
        val b = body ?: return null
        val s = sender.orEmpty()
        if (parser.canParse(s, b)) parser.parse(s, b)?.takeIf { it.isBnplConfirmation }?.let { return it.bankName }
        return PROVIDERS.firstOrNull { (s + " " + b).contains(it, ignoreCase = true) }
    }

    private companion object {
        const val SPLIT_PAYMENTS = "Split Payments"
        val PROVIDERS = listOf("Tabby", "Tamara")
    }
}
