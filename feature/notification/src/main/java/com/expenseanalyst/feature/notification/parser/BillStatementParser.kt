package com.expenseanalyst.feature.notification.parser

/** Mirror of [TransactionParser] for bill/statement SMS messages. */
interface BillStatementParser {
    val bankName: String
    fun canParse(sender: String, body: String): Boolean
    fun parse(sender: String, body: String): ParsedBillStatement?
}
