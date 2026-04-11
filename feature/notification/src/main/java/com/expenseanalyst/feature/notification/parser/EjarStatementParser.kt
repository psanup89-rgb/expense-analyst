package com.expenseanalyst.feature.notification.parser

/**
 * Parses Ejar (إيجار) rental platform bill SMS into bill entries.
 * SMS body is in Arabic.
 *
 * Sample (translated: "Dear Tenant: A bill has been issued for contract number 10953825733
 * with an amount of 6000.00 riyals. Pay via https://checkout.ejar.sa/..."):
 *
 *   "عزيزنا المستأجر:
 *    نفيدك بإصدار فاتورة لعقد رقم 10953825733 بقيمة 6000.00 ريال،
 *    يمكنك سداد المبلغ المستحق مباشرة عبر منصة إيجار من خلال الدفع السريع
 *    https://checkout.ejar.sa/?sh=..."
 */
class EjarStatementParser : BillStatementParser {

    override val bankName = "Ejar"

    private val bodyFingerprint = Regex("""منصة\s*إيجار|checkout\.ejar\.sa|ejar\.sa""")

    // Arabic: بقيمة X ريال  ("with amount of X riyals")
    private val amountPattern = Regex("""بقيمة\s*([\d,]+\.?\d*)\s*ريال""")

    // Arabic: عقد رقم XXXXXXXXXX  ("contract number XXXX")
    private val contractPattern = Regex("""عقد\s*رقم\s*(\d+)""")

    override fun canParse(sender: String, body: String): Boolean =
        bodyFingerprint.containsMatchIn(body)

    override fun parse(sender: String, body: String): ParsedBillStatement? {
        val amount = amountPattern.find(body)
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        val contractNumber = contractPattern.find(body)?.groupValues?.get(1)?.trim()

        return ParsedBillStatement(
            billerName = "Ejar",
            totalDue = amount,
            minimumDue = null,
            currencyCode = "SAR",
            dueDateMillis = null,
            statementPeriodStart = null,
            statementPeriodEnd = null,
            accountLast4 = contractNumber?.takeLast(4),
            bankName = bankName,
            reference = contractNumber,
            rawBody = body
        )
    }
}
