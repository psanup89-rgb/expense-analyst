package com.expenseanalyst.feature.notification.parser

/**
 * Dispatches bill statement SMS to the first matching [BillStatementParser].
 * Tried FIRST by [TransactionNotificationService], before [ParserRegistry], so bill reminders
 * are never misclassified as spend transactions.
 * Order: specific banks first, generic fallback last.
 */
object BillStatementParserRegistry {

    private val parsers: List<BillStatementParser> = listOf(
        IdfcFirstBankStatementParser(),
        AxisBankStatementParser(),
        EmiratesNbdStatementParser(),
        AlRajhiStatementParser(),
        HdfcStatementParser(),
        TamaraStatementParser(),
        SaudiEnergyStatementParser(),
        EjarStatementParser(),
        AirtelStatementParser(),
        GenericStatementParser()
    )

    fun parse(sender: String, body: String): ParsedBillStatement? {
        for (parser in parsers) {
            if (parser.canParse(sender, body)) {
                val result = parser.parse(sender, body)
                if (result != null) return result
            }
        }
        return null
    }
}
