package com.expenseanalyst.feature.notification.parser

/**
 * FASTag (LivQuik / QWFSTG) toll/parking SMS parser.
 * Sender IDs: *-QWFSTG, *-QWFSTG-S, VMQWFSTG, etc.
 *
 * Sample (toll):
 *   "Livquik Fastag debited Rs25.0 for TN19AM4212 in VANAGARAM at 28/09/24 20:39,Bal Rs338.0."
 * Sample (parking):
 *   "Customer,Parking fee of Rs.120.0 in 9042173418 is debited from LivQuik Fastag at NEXUS VIJAYA MA, 20/04/25"
 */
class FasTagParser : TransactionParser {

    override val bankName = "FASTag"

    private val senderPattern = Regex("""(?i)qwfstg""")
    private val amountPattern = Regex("""(?i)(?:rs\.?)\s*([\d,]+\.?\d*)""")

    // Toll: "debited RsX for VEHICLEID in LOCATION at DATE"
    private val tollLocationPattern = Regex("""(?i)\bin\s+([A-Z][A-Z _\-]+?)\s+at\s+\d""")
    // Parking: "at LOCATION, DATE"
    private val parkingLocationPattern = Regex("""(?i)(?:Fastag|LivQuik)\s+at\s+([A-Za-z0-9][A-Za-z0-9 _\-&.]*?),\s*\d""")

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        if (!Regex("""(?i)\bdebited\b""").containsMatchIn(body)) return null

        val amount = amountPattern.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: return null

        // Extract location as merchant
        val merchant = (tollLocationPattern.find(body)?.groupValues?.get(1)
            ?: parkingLocationPattern.find(body)?.groupValues?.get(1))
            ?.trim()?.takeIf { it.isNotBlank() && it.length < 60 }

        return ParsedTransaction(
            amount = amount,
            currencyCode = "INR",
            type = TransactionDirection.DEBIT,
            merchant = merchant,
            accountLast4 = null,
            referenceNumber = null,
            bankName = bankName,
            paymentMethodName = "WALLET"
        )
    }
}
