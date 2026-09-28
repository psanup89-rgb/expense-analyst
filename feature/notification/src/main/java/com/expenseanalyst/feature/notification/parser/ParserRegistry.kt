package com.expenseanalyst.feature.notification.parser

/**
 * Dispatches incoming notification/SMS to the first matching [TransactionParser].
 * Order matters: specific bank parsers first, generic fallback last.
 */
object ParserRegistry {

    private val parsers: List<TransactionParser> = listOf(
        // Indian banks
        HdfcParser(),
        SbiParser(),
        IciciParser(),
        AxisParser(),
        KotakParser(),
        YesBankParser(),
        IdfcFirstBankParser(),
        OneCardParser(),
        // Saudi banks
        AlRajhiParser(),
        StcBankParser(),
        AlinmaParser(),
        D360Parser(),
        SabParser(),
        // UAE banks
        EmiratesNbdParser(),
        // Toll / FASTag
        FasTagParser(),
        // Digital wallets
        WalletParser(),
        // UPI apps
        UpiParser(),
        // Bill payment apps (body-fingerprint detection)
        MubasherParser(),
        // Merchant apps (food delivery, e-commerce, etc.)
        KeetaParser(),
        // BNPL purchase-split confirmations (Tabby, Tamara) — see TabbyTamaraParser's KDoc,
        // unverified against a real message. Must precede GenericParser, whose isPayment
        // regex ("payment successful/confirmed") could otherwise catch these SMS first and
        // silently under-count them instead of routing to the BNPL-specific handling.
        TabbyTamaraParser(),
        // Generic fallback
        GenericParser()
    )

    // OTP/verification-code messages often restate the transaction amount for context
    // ("Enter OTP 1234 to authorize SAR 649.00 at noon") but are not themselves a
    // transaction — a bank sends one alongside its real purchase-confirmation SMS. Without
    // this guard, several parsers' generic amount+keyword matching happily parses these as
    // a second, duplicate expense for the same real-world purchase.
    private val otpPattern = Regex("""(?i)\bOTP\b|\bone[\s-]?time[\s-]?password\b|\bverification\s+code\b""")

    /**
     * Finds the first parser that can handle this sender/body, runs it, and returns the result.
     * Returns null only if even the generic parser found nothing useful.
     */
    fun parse(sender: String, body: String): ParsedTransaction? {
        if (otpPattern.containsMatchIn(body)) return null
        if (nonTransactionPattern.containsMatchIn(body)) return null
        for (parser in parsers) {
            if (parser.canParse(sender, body)) {
                val result = parser.parse(sender, body)
                if (result != null) return normalize(result, body)
            }
        }
        return null
    }

    // Messages that mention an amount but move no money: a card-due reminder (the debit comes
    // later in its own SMS — BillStatementParserRegistry still gets these, it runs on a null
    // here), a failed auto-debit, a low-balance notice, a UPI collect *request*, a bill-download
    // link, a biller's bill/suspension notice. Each was being saved as a transaction.
    private val nonTransactionPattern = Regex(
        """(?i)\bis\s+due\s+for\s+payment\b|auto\s*debit\s+instruction.*\bhas\s+failed|""" +
            """\bbalance\s+is\s+almost\s+consumed|\bhas\s+requested\s+money\b|""" +
            """\bto\s+download\s+your\s+bill\b|""" +
            // A biller's notice, not a payment: "bill has been issued", "service will be
            // suspended / has been temporarily suspended". The Mobily suspension notice was
            // being saved as INCOME. Not "total due amount" alone — Al Rajhi's foreign-purchase
            // SMS carries that line too.
            """\bbill\s+has\s+been\s+issued\b|\bwill\s+be\s+(?:temporarily\s+)?(?:suspended|deactivated)\b|""" +
            """\bhas\s+been\s+temporarily\s+suspended\b|""" +
            // Axis: "Your Credit Card … has a credit balance of INR 1445. The amount will be
            // credited to your Savings Account if not used" — a notice; the actual move arrives
            // later as "Excess amount … has been credited to A/c".
            """\bhas\s+a\s+credit\s+balance\b""",
        RegexOption.DOT_MATCHES_ALL
    )

    // Paying a credit-card bill moves money between the user's own accounts: the spending was
    // already counted when the card was used. Sent via CRED, or straight to the card issuer.
    private val cardBillPayeePattern = Regex(
        """(?i)^(?:cred(?:\s+club)?|dreamplug.*|american\s+express|amex|.*credit\s+card\s+bil.*)$"""
    )

    // The card side of the same bill payment ("Payment of INR 500 has been received towards
    // your Axis Bank Credit Card", "Online Payment ... credited to your card ending ..."), and
    // excess card balance returned to a bank account. Not income. A refund to a card is — so
    // refund wording opts out.
    private val cardPaymentCreditPattern = Regex(
        """(?i)payment.{0,60}(?:received|credited).{0,40}\bcard\b|credited\s+to\s+your\s+card\b|""" +
            """excess\s+amount.{0,60}credit\s+card|credit\s+card\s*:\s*credited"""
    )
    private val refundWording = Regex("""(?i)refund|reversal|cash\s*back""")

    // Merchant fragments that identify an account or card, or a helpline number — never a
    // payee (CLAUDE.md output rules: card identifiers must never be displayed).
    private val identifierInMerchant = Regex("""(?i)\bcard\s+ending\b|\bxx\s*\d{2,}|\b(?:a/?c|account)\b.*\d|^\+?[\d\s-]{8,}$""")
    private val usingYourSuffix = Regex("""(?i)\s+using\s+your\b.*$""")
    // "ACH D- Groww-0000ZACR1X…" / "ACH D- TP ACH INDIANESIGN-2320147606" → the payee alone
    private val achPattern = Regex("""(?i)^ACH\s*D-\s*(?:TP\s+ACH\s+)?(.+?)-[A-Z0-9]{6,}$""")

    private val symbolRun = Regex("""[\p{Sm}\p{So}]+""")
    // Amazon's own refund notice names no merchant: "Refund Issued: Amount SAR X (item) will be
    // credited to your Visa / Amazon Account … amznsa.com/…"
    private val amazonNotice = Regex("""(?i)\bamazon\b|amznsa\.com""")
    // "Info: ACH D- Groww-…" — a bank auto-debit (SIP, loan EMI)
    private val achDebit = Regex("""(?i)\bACH\s*D-""")

    /** Cross-bank corrections applied to every parser's result; see the patterns above. */
    internal fun normalize(parsed: ParsedTransaction, body: String): ParsedTransaction {
        val merchant = sanitizeMerchant(parsed.merchant)
            ?: if (amazonNotice.containsMatchIn(body)) "Amazon" else null
        val paymentMethod = parsed.paymentMethodName
            ?: if (achDebit.containsMatchIn(body)) "NET_BANKING" else null
        val type = when {
            parsed.type == TransactionDirection.DEBIT && merchant != null &&
                cardBillPayeePattern.matches(merchant.trim()) -> TransactionDirection.PAYMENT
            parsed.type == TransactionDirection.CREDIT && cardPaymentCreditPattern.containsMatchIn(body) &&
                !refundWording.containsMatchIn(body) -> TransactionDirection.PAYMENT
            else -> parsed.type
        }
        return parsed.copy(merchant = merchant, type = type, paymentMethodName = paymentMethod)
    }

    internal fun sanitizeMerchant(merchant: String?): String? {
        // Banks pad descriptors with symbols ("HUNGERSTATION LLC××", "HungerStation××Riya")
        var m = merchant?.replace(symbolRun, " ")?.replace(Regex("""\s+"""), " ")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        achPattern.find(m)?.let { m = it.groupValues[1].trim() }
        m = m.replace(usingYourSuffix, "").trim()
        if (m.isEmpty() || identifierInMerchant.containsMatchIn(m)) return null
        return m
    }
}
