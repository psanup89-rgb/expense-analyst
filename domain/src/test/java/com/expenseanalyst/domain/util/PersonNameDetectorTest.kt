package com.expenseanalyst.domain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PersonNameDetectorTest {

    @ParameterizedTest(name = "{0} (upi={1}) -> {2}")
    @CsvSource(
        // people, as real UPI payees appear
        "P K SASEEDHARAN, true, true",
        "SHEELA  V V, true, true",
        "Mr Ayush Wankhade, true, true",
        "Mrs USHA  M, true, true",
        "AKSHAY SUBASH, true, true",
        // businesses
        "3FIVE8 TECHNOLOGIES PRIVA, true, false",
        "HAPPYLOCATE RELOCATION SERVICES, true, false",
        "S BROTHERS LOGISTIC PACKE, true, false",
        "LULU EXPRESS DIPLOMATIC Q, true, false",
        "M231UCECNTOY2, true, false",
        "ROTTWEILER CHENNAI OPC PR, true, false",
        "COCO Maraimalainagar Naga, true, false",
        // one word is ambiguous (a person or a shop)
        "ANTHONYRAJ, true, false",
        "PAANDIKADAI, true, false",
        // never on a card: truncated descriptors look like names
        "RAYMOND L, false, false",
        "Mr Ayush Wankhade, false, false"
    )
    fun `detects individuals only on UPI debits`(name: String, upi: Boolean, expected: Boolean) {
        assertEquals(expected, PersonNameDetector.looksLikePerson(name, upi))
    }
}
