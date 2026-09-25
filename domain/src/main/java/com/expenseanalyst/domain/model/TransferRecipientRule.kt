package com.expenseanalyst.domain.model

/**
 * Remembers how the user classified transfers to one recipient, so future transfers to the same
 * name classify themselves. Keyed on the recipient NAME rather than an account, because for an
 * outgoing transfer the row's `accountId` is the *source* (already the user's own account) — the
 * destination exists only as [Expense.merchantName]. An `Account.isMine` flag cannot solve this.
 *
 * Kept out of [MerchantRule] deliberately: that table's `categoryId` is non-null (so remembering
 * a recipient would force picking a category and then silently apply it to every future row from
 * that name), its `merchantPattern` is uniquely indexed and upserted with REPLACE (so a later
 * category rule for the same name would destroy this), and its matcher is substring containment,
 * which is wrong for person names — a "RAJ" pattern would swallow "RAJASEKAR" and "RAJESH".
 *
 * @param recipientKey normalised match key, see [com.expenseanalyst.domain.util.TransferRecipientMatcher.normalize]
 * @param recipientDisplayName the name as it was originally captured, for showing back to the user
 */
data class TransferRecipientRule(
    val id: Long = 0,
    val recipientKey: String,
    val recipientDisplayName: String,
    val classification: TransferClassification,
    val createdAt: Long
)
