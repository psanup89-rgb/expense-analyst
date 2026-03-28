package com.expenseanalyst.feature.notification.parser

/**
 * IDFC FIRST Bank (India) SMS parser.
 * Sender IDs: *-IDFCFB, *-IDFCFB-S, *-IDFCFB-T, TMIDFCFB, JKIDFCFB, etc.
 *
 * Sample (credit card spend):
 *   "Transaction Successful! INR 392.43 spent on your IDFC FIRST Bank Credit Card ending XX6426
 *    at ZOMATO on 24 MAR 2026 at 07:38 PM Avbl Limit: INR 166566.96 ..."
 * Sample (fun prefix):
 *   "Delicious Purchase! INR 399.93 spent on your IDFC FIRST Bank Credit Card ending XX6426
 *    at ZOMATO LIMITED on 24 MAR 2026 at 07:36 PM ..."
 * Sample (savings debit):
 *   "Your A/C XXXXX632065 is debited by INR 230.00 on 27/01/25 09:15. New Bal :INR 25,988.14."
 * Sample (savings credit):
 *   "Your A/C XXXXX632065 is credited with INR 3,000.00 on 23/01/25 13:24."
 * Sample (card payment):
 *   "Thank you for payment of INR 32,638.46 towards your Mayura Credit Card XX6887 on 05 Mar 2026."
 * Sample (interest credit):
 *   "Monthly interest of INR.62.00 earned on your Savings A/c XX2065 has been credited..."
 */
class IdfcFirstBankParser : TransactionParser {

    override val bankName = "IDFC First Bank"

    private val senderPattern = Regex("""(?i)(?:idfcfb|idfc\s*first)""")

    // Credit card spend: "INR 392.43 spent on your IDFC FIRST Bank Credit Card ending XX6426 at MERCHANT on DATE"
    private val ccSpendPattern = Regex(
        """(?i)INR\s*([\d,]+\.?\d*)\s+spent\s+on\s+your\s+.*?Credit\s+Card\s+ending\s+XX(\d{4})\s+at\s+(.+?)\s+on\s+\d"""
    )

    // Savings account debit: "A/C XXXXX632065 is debited by INR 230.00"
    private val savingsDebitPattern = Regex(
        """(?i)A/C\s+\w*?(\d{4})\s+is\s+debited\s+by\s+INR\s*([\d,]+\.?\d*)"""
    )

    // Savings account credit: "A/C XXXXX632065 is credited with INR 3,000.00"
    private val savingsCreditPattern = Regex(
        """(?i)A/C\s+\w*?(\d{4})\s+is\s+credited\s+with\s+INR\s*([\d,]+\.?\d*)"""
    )

    // Card payment: "payment of INR 32,638.46 towards your Mayura Credit Card XX6887"
    private val paymentPattern = Regex(
        """(?i)payment\s+of\s+INR\s*([\d,]+\.?\d*)\s+towards\s+your\s+.*?(?:Credit\s+)?Card\s+XX(\d{4})"""
    )

    // Interest credit: "Monthly interest of INR.62.00 earned on your Savings A/c XX2065"
    // Note: IDFC uses "INR.62.00" (dot after INR, no space) or "Rs.62.00"
    private val interestPattern = Regex(
        """(?i)interest\s+of\s+(?:INR|Rs)\.?\s*([\d,]+\.?\d*)\s+earned\s+on\s+your\s+Savings\s+A/c\s+XX(\d{4})"""
    )

    override fun canParse(sender: String, body: String): Boolean =
        senderPattern.containsMatchIn(sender)

    override fun parse(sender: String, body: String): ParsedTransaction? {
        val paymentMethod = PaymentMethodDetector.detect(body)

        // 1. Credit card spend (most common)
        ccSpendPattern.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val last4 = match.groupValues[2]
            val merchant = match.groupValues[3].trim().takeIf { it.isNotBlank() && it.length < 60 }
            return ParsedTransaction(
                amount = amount,
                currencyCode = "INR",
                type = TransactionDirection.DEBIT,
                merchant = merchant,
                accountLast4 = last4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = paymentMethod ?: "CREDIT_CARD"
            )
        }

        // 2. Savings account debit
        savingsDebitPattern.find(body)?.let { match ->
            val last4 = match.groupValues[1]
            val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                currencyCode = "INR",
                type = TransactionDirection.DEBIT,
                merchant = null,
                accountLast4 = last4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = paymentMethod ?: "DEBIT_CARD"
            )
        }

        // 3. Savings account credit
        savingsCreditPattern.find(body)?.let { match ->
            val last4 = match.groupValues[1]
            val amount = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            return ParsedTransaction(
                amount = amount,
                currencyCode = "INR",
                type = TransactionDirection.CREDIT,
                merchant = null,
                accountLast4 = last4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = paymentMethod ?: "DEBIT_CARD"
            )
        }

        // 4. Card payment confirmation
        paymentPattern.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val last4 = match.groupValues[2]
            return ParsedTransaction(
                amount = amount,
                currencyCode = "INR",
                type = TransactionDirection.PAYMENT,
                merchant = null,
                accountLast4 = last4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = paymentMethod ?: "CREDIT_CARD"
            )
        }

        // 5. Interest credit
        interestPattern.find(body)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val last4 = match.groupValues[2]
            return ParsedTransaction(
                amount = amount,
                currencyCode = "INR",
                type = TransactionDirection.CREDIT,
                merchant = "Interest",
                accountLast4 = last4,
                referenceNumber = null,
                bankName = bankName,
                paymentMethodName = paymentMethod ?: "DEBIT_CARD"
            )
        }

        return null
    }
}
