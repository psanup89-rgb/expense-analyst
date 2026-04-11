package com.expenseanalyst.feature.notification.parser

/**
 * Dispatches bill statement SMS to the first matching [BillStatementParser].
 * Tried only after [ParserRegistry] returns null (i.e., it's not a transaction SMS).
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
