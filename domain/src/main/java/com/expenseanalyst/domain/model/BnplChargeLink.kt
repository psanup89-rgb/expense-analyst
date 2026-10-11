package com.expenseanalyst.domain.model

/**
 * A card charge the user linked by hand to a Tabby/Tamara plan it pays (DB v35). One charge may
 * link to several plans — Tamara bills instalments that fall due together as one charge — so this
 * is a separate link rather than [Expense.bnplPurchaseId], which holds the automatic one-plan match.
 */
data class BnplChargeLink(val chargeId: Long, val purchaseId: Long)
