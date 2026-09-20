package com.expenseanalyst.feature.notification.parser

/**
 * Canonical bank/biller name for an SMS sender ID.
 *
 * Single source of truth, deliberately. This lookup previously existed as three drifting
 * copies (GenericParser, GenericStatementParser's sender-derived biller, and
 * SmsImportViewModel) — which is how the same Al Rajhi card ended up filed under both
 * "Al Rajhi Bank" and "AlRajhiBank", and Emirates NBD under "EmiratesNBD".
 *
 * Real sender IDs are 6-character DLT codes (FEDONE, ONECRD, DBSBNK), so the checks below
 * match those truncations rather than the readable full names.
 */
object BankNameFromSender {

    /** Canonical name, or null when the sender maps to no known bank. */
    fun resolve(sender: String): String? {
        val s = sender.uppercase()
        return when {
            "HDFC" in s -> "HDFC Bank"
            "ICICI" in s -> "ICICI Bank"
            "AXISBK" in s || "AXISBANK" in s || "AXIS" in s -> "Axis Bank"
            "SBIINB" in s || "SBIPSG" in s || "SBIUPI" in s || "SBI" in s -> "SBI"
            "KOTAK" in s -> "Kotak Bank"
            "YESBNK" in s || "YESBANK" in s -> "Yes Bank"
            "INDUS" in s -> "IndusInd Bank"
            "PNBSMS" in s || "PUNJAB" in s -> "PNB"
            "ALRJHI" in s || "ALRAJHI" in s || "RAJHI" in s -> "Al Rajhi Bank"
            "ALINMA" in s -> "Alinma Bank"
            "STCBNK" in s || "STCPAY" in s -> "STC Bank"
            "D360" in s -> "D360 Bank"
            "DBSBNK" in s || "DBS" in s -> "DBS Bank"
            "EMIRNBD" in s || "ENBD" in s || "EMIRATES" in s -> "Emirates NBD"
            "IDFCFB" in s || "IDFCFIRST" in s || "IDFC" in s -> "IDFC First Bank"
            "ONECARD" in s || "ONECRD" in s || "FEDERAL" in s || "FEDONE" in s -> "OneCard"
            "CANARA" in s -> "Canara Bank"
            "BANKOFBARODA" in s || "BOB" in s -> "Bank of Baroda"
            "UNION" in s -> "Union Bank"
            "CITI" in s -> "Citi Bank"
            "AMEX" in s -> "American Express"
            "PAYTM" in s -> "Paytm"
            "AIRTEL" in s -> "Airtel Payments Bank"
            "CLRTRP" in s || "CLEARTRIP" in s -> "Cleartrip"
            else -> null
        }
    }
}
