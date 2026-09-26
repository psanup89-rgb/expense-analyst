package com.expenseanalyst.domain.util

/**
 * Decides whether a UPI payee looks like an individual rather than a business, so the payment
 * can be filed under "People" instead of Misc.
 *
 * Deliberately conservative, and only ever asked about a UPI debit: a card purchase always
 * pays a business, and truncated card descriptors ("RAYMOND L", "SAUDI ELE") look exactly like
 * "first name + initial". Even on UPI a single word is not enough — "ANTHONYRAJ" and
 * "PAANDIKADAI" (a shop) are indistinguishable — so a name needs an honorific or 2–4 words.
 * Known brands never reach this check: [CategoryInference] runs keyword matching first.
 */
object PersonNameDetector {

    private val honorific = Regex("""(?i)^(mr|mrs|ms|miss|dr|smt|shri|sri)\.?\s+""")

    // Words that mark a business, a bank or a payment reference rather than a person.
    private val businessWords = setOf(
        "ltd", "limited", "pvt", "private", "priva", "llp", "llc", "inc", "co", "company", "corp",
        "corporation", "enterprise", "enterprises", "enter", "store", "stores", "shop", "mart",
        "traders", "trading", "services", "service", "technologies", "technology", "tech",
        "solutions", "hotel", "restaurant", "cafe", "foods", "food", "bank", "india", "agency",
        "center", "centre", "pharmacy", "medical", "motors", "cars", "sons", "brothers", "bros",
        "est", "industries", "logistics", "logistic", "club", "mall", "express", "payments",
        "payment", "online", "digital", "global", "international", "group", "studio", "salon",
        "clinic", "hospital", "school", "academy", "the", "and",
        // "OPC" = One Person Company; city/locality words mark a shop's descriptor
        "opc", "chennai", "bangalore", "bengaluru", "mumbai", "delhi", "kochi", "riyadh",
        "jeddah", "dubai", "naga", "nagar"
    )

    fun looksLikePerson(merchant: String?, upiDebit: Boolean): Boolean {
        if (!upiDebit) return false
        val name = merchant?.trim()?.replace(Regex("""\s+"""), " ") ?: return false
        if (name.isEmpty() || !name.all { it.isLetter() || it == ' ' || it == '.' }) return false
        if (honorific.containsMatchIn(name)) return true
        val words = name.lowercase().split(' ').map { it.trim('.') }.filter { it.isNotEmpty() }
        if (words.size !in 2..4) return false
        return words.none { it in businessWords || it.endsWith("nagar") }
    }
}
