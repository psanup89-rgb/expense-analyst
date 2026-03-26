# Bank Parser Skill

You are a specialist in adding and modifying bank SMS/notification parsers for the **Expense Analyst** Android app.

## Context

The app uses a `NotificationListenerService` to intercept bank SMS and push notifications, parses them into `ParsedTransaction` objects, and surfaces a confirmation UI before saving.

**Key files** (once project is scaffolded):
```
feature/notification/src/main/
  service/TransactionNotificationService.kt  ← NotificationListenerService
  parser/TransactionParser.kt               ← Interface all parsers implement
  parser/ParserRegistry.kt                  ← Dispatches by sender ID
  parser/hdfc/HdfcParser.kt                 ← Example bank parser
  parser/generic/GenericFallbackParser.kt   ← Catch-all parser
  model/ParsedTransaction.kt                ← Parsed result model

feature/notification/src/test/
  resources/sms_samples/                    ← CSV fixture files per bank
  parser/HdfcParserTest.kt                  ← Parameterized tests
```

## TransactionParser Interface

```kotlin
interface TransactionParser {
    val supportedSenderIds: List<String>  // e.g. ["HDFCBK", "HDFC-B"]
    fun canParse(sender: String, body: String): Boolean
    fun parse(sender: String, body: String): ParsedTransaction?
}
```

## ParsedTransaction Model

```kotlin
data class ParsedTransaction(
    val amount: Double,
    val currencyCode: String,          // ISO 4217
    val transactionType: TransactionType, // DEBIT or CREDIT
    val merchantName: String?,
    val accountLastFour: String?,
    val referenceNumber: String?,
    val dateUtcMillis: Long?,           // null = use current time
    val description: String,
    val rawMessage: String
)
```

## SOP: Adding a New Bank Parser

1. **Collect SMS samples** — get 20+ real SMS examples from the bank covering debits, credits, UPI, card transactions, international amounts, edge cases
2. **Create fixture CSV** at `src/test/resources/sms_samples/<bank_name>.csv` with columns:
   ```
   sender,body,expectedAmount,expectedCurrency,expectedType,expectedMerchant,expectedAccount
   ```
3. **Write failing tests first** using `@CsvFileSource` (TDD approach)
4. **Implement the parser** in `parser/<bank_name>/<BankName>Parser.kt`
5. **Register** in `ParserRegistry.kt`
6. **Verify** 95%+ parse rate on all fixture rows

## Regex Patterns by Region

### Indian Banks (INR)
```
Amount:     (?i)(?:rs\.?|inr)\s*([\d,]+\.?\d*)
Debit:      (?i)(debited|debit|spent|paid|payment of|purchase)
Credit:     (?i)(credited|credit|received|refund)
Account:    (?i)(?:a\/c|acct?|account)[^0-9]*(?:x+|[*]+)?(\d{4})
Reference:  (?i)(?:ref\.?\s*(?:no\.?)?|txn\s*(?:id|no))[:\s]*([a-z0-9]+)
```

### Saudi Banks (SAR)
```
Amount:     (?i)(?:sar|ر\.س\.?)\s*([\d,]+\.?\d*)
Debit:      (?i)(debited|purchased|withdrawn|payment)
Credit:     (?i)(credited|received|deposited)
```

### UPI Apps
```
Amount:     (?i)(?:rs\.?|inr|₹)\s*([\d,]+\.?\d*)
UPI Ref:    (?i)(?:upi\s*ref(?:erence)?)[:\s]*(\d+)
Sender:     (?i)from\s+([a-z0-9\s]+?)(?:\s+to|\s+via|$)
```

### Generic Fallback
```
Amount:     (?i)(?:rs\.?|inr|sar|usd|aed|eur|gbp|₹|ر\.س)\s*([\d,]+\.?\d*)
```

## Supported Banks Reference

| Bank | Sender IDs | Currency | Notes |
|------|-----------|----------|-------|
| HDFC | HDFCBK, HDFC-B | INR | Debit + credit card, UPI |
| SBI | SBIINB, SBI-UPI | INR | ATM, NEFT, UPI |
| ICICI | ICICIB, ICICI | INR | |
| Axis | AXISBK, AXIS-B | INR | |
| Kotak | KOTAKB, KOTAK | INR | |
| Yes Bank | YESBNK, YESBK | INR | |
| Al Rajhi | ALRAJHI, RJHISB | SAR | Arabic + English variants |
| STC Bank | STCPAY, STCBNK | SAR | |
| Alinma | ALINMA | SAR | |
| D360 | D360BK | SAR | |
| Google Pay | com.google.android.apps.nbu.paisa.user | INR/multi | App notification |
| PhonePe | com.phonepe.app | INR | App notification |
| Paytm | net.one97.communications | INR | App notification |

## Test Template

```kotlin
@ExtendWith(MockitoExtension::class)
class HdfcParserTest {

    private val parser = HdfcParser()

    @ParameterizedTest
    @CsvFileSource(resources = ["/sms_samples/hdfc.csv"], numLinesToSkip = 1)
    fun `parses HDFC SMS correctly`(
        sender: String,
        body: String,
        expectedAmount: Double,
        expectedCurrency: String,
        expectedType: String,
        expectedMerchant: String?,
        expectedAccount: String?
    ) {
        val result = parser.parse(sender, body)

        assertNotNull(result)
        assertEquals(expectedAmount, result!!.amount, 0.01)
        assertEquals(expectedCurrency, result.currencyCode)
        assertEquals(TransactionType.valueOf(expectedType), result.transactionType)
        expectedMerchant?.let { assertEquals(it, result.merchantName) }
        expectedAccount?.let { assertEquals(it, result.accountLastFour) }
    }
}
```

## Quality Checklist
- [ ] 20+ SMS samples per bank in CSV fixture
- [ ] Tests cover: debit, credit, UPI, card, international, edge cases
- [ ] 95%+ parse rate on fixture data
- [ ] Parser registered in `ParserRegistry`
- [ ] `canParse()` correctly rejects non-matching messages
- [ ] Handles null gracefully (returns `null` on parse failure, never throws)
- [ ] Amount parsing handles comma-formatted numbers (e.g., "1,234.56")
