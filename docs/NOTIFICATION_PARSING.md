# Notification Parsing - Standard Operating Procedure

## Current Repo Status
The notification parsing architecture described here is still a **planned Phase 1D workstream**. The feature module exists, but the parser/service implementation described below is not yet complete in the current repository state.

## Overview
The notification parsing system intercepts bank SMS and push notifications, extracts transaction data using regex parsers, and presents pre-filled expense entries for user confirmation.

## Architecture

```
Notification/SMS arrives
  ↓
TransactionNotificationService (NotificationListenerService)
  ↓ extracts text + sender
ParserRegistry.parse(sender, messageText)
  ↓ dispatches to matching parser
BankSpecificParser.parse(messageText)
  ↓ returns
ParsedTransaction (amount, type, merchant, date, account, currency)
  ↓ shown as
Confirmation Banner → User edits/confirms → Expense saved to Room
```

## Key Files
- `feature/notification/service/TransactionNotificationService.kt` — Android service
- `feature/notification/parser/TransactionParser.kt` — Parser interface
- `feature/notification/parser/ParserRegistry.kt` — Dispatcher
- `feature/notification/parser/<Bank>Parser.kt` — Bank-specific parsers
- `feature/notification/model/ParsedTransaction.kt` — Parsed result model

## ParsedTransaction Model

```kotlin
data class ParsedTransaction(
    val amount: Double,
    val transactionType: TransactionType,  // DEBIT or CREDIT
    val merchantName: String?,
    val date: Instant?,                     // null = use current time
    val accountLast4: String?,
    val currencyCode: String,               // e.g., "INR", "SAR"
    val referenceNumber: String?,
    val balance: Double?,                   // available balance if present
    val paymentMethod: PaymentMethod?,      // inferred from context
    val rawMessage: String                  // original message text
)
```

## TransactionParser Interface

```kotlin
interface TransactionParser {
    /** Sender IDs this parser handles (e.g., "HDFCBK", "AD-HDFCBK") */
    val supportedSenders: Set<String>

    /** Default currency for this parser */
    val defaultCurrency: String

    /** Parse the message text. Returns null if parsing fails. */
    fun parse(messageText: String): ParsedTransaction?
}
```

## Supported Parsers

### Indian Banks (Currency: INR)

| Bank | Sender IDs | Key Patterns |
|------|-----------|--------------|
| HDFC | HDFCBK, AD-HDFCBK | `Rs.X debited/credited from A/C XXXX1234` |
| SBI | SBIINB, SBIPSG, AD-SBISMS | `debited by/with Rs.X` |
| ICICI | ICICIB, AD-ICICIB | `Acct XXXX1234 debited with Rs.X` |
| Axis | AXISBK, AD-AXISBK | `Rs.X debited from A/C XXXX1234` |
| Kotak | KOTAKB, AD-KOTAKB | `A/c XXXX1234 debited for Rs.X` |
| Yes Bank | YESBK, AD-YESBK | `INR X debited from A/C XXXX1234` |

### Saudi Banks (Currency: SAR)

| Bank | Sender IDs | Key Patterns |
|------|-----------|--------------|
| Al Rajhi | ALRAJHI, AlRajhiBank | `SAR X Purchase/Withdrawal at Merchant` |
| STC Bank | STCBANK, STCPay | `SAR X debited/Transaction of SAR X` |
| Alinma | ALINMA, AlinmaBank | `SAR X debited from account` |
| D360 | D360Bank, D360 | `SAR X Transaction at Merchant` |

### Digital Wallets

| Wallet | Package / Sender | Key Patterns |
|--------|-----------------|--------------|
| Apple Pay | com.apple.passbook | `Payment of X to Merchant` |
| Google Wallet | com.google.android.apps.walletnfcrel | `Paid X to Merchant` |
| Samsung Pay | com.samsung.android.spay | `Payment X at Merchant` |

### UPI Apps (Currency: INR)

| App | Package / Sender | Key Patterns |
|-----|-----------------|--------------|
| Google Pay | com.google.android.apps.nbu.paisa.user | `Paid Rs.X to Merchant` |
| PhonePe | com.phonepe.app | `Paid Rs.X to Merchant` |
| Paytm | net.one97.paytm | `Rs.X paid to Merchant via Paytm` |

### Generic Fallback Parser
Attempts to extract transactions from any unrecognized sender using broad patterns:
```regex
Amount:  (?i)(?:rs|inr|sar|usd|aed|eur|gbp|ر\.س)\.?\s*([\d,]+\.?\d*)
Type:    (?i)(debited|credited|paid|received|spent|purchased|withdrawn|transferred)
```

## SOP: Adding a New Bank Parser

### Step 1: Collect SMS Samples
- Collect 20+ real SMS samples from the bank (anonymize account numbers)
- Include: debit, credit, UPI, card transactions, failed transactions
- Save in `feature/notification/src/test/resources/sms_samples/<bank_name>.csv`
- CSV format: `sender,message_body,expected_amount,expected_type,expected_merchant,expected_currency,expected_account_last4`

### Step 2: Create the Parser
1. Create `feature/notification/parser/<BankName>Parser.kt`
2. Implement `TransactionParser` interface
3. Set `supportedSenders` to match bank's sender IDs
4. Set `defaultCurrency` (e.g., "INR", "SAR")
5. Write regex patterns to extract: amount, type, merchant, date, account, reference

### Step 3: Register in ParserRegistry
Add the new parser to `ParserRegistry.parsers` list:
```kotlin
class ParserRegistry @Inject constructor() {
    private val parsers: List<TransactionParser> = listOf(
        HdfcSmsParser(),
        SbiSmsParser(),
        // ... add new parser here
        GenericBankSmsParser()  // always last (fallback)
    )
}
```

### Step 4: Write Tests
1. Create `feature/notification/src/test/kotlin/.../parser/<BankName>ParserTest.kt`
2. Use `@ParameterizedTest` with `@CsvFileSource` pointing to the samples CSV
3. Test each sample produces correct `ParsedTransaction`
4. Test edge cases: partial messages, missing fields, different formats
5. Target: 95%+ parse success rate on collected samples

### Step 5: Manual Testing
1. Run app on emulator
2. Use emulator's SMS tool to send test messages with the bank's sender ID
3. Verify confirmation banner appears with correct amount, merchant, etc.
4. Confirm expense is saved correctly

## Common Regex Patterns

### Amount Extraction
```regex
# Indian (INR)
(?:Rs\.?\s*|₹\s*|INR\s*)([\d,]+(?:\.\d{2})?)

# Saudi (SAR)
(?:SAR\s*|ر\.س\s*)([\d,]+(?:\.\d{2})?)

# Generic multi-currency
(?:Rs|INR|SAR|USD|AED|EUR|GBP|₹|ر\.س)\.?\s*([\d,]+(?:\.\d{2})?)
```

### Transaction Type
```regex
(?i)(debit(?:ed)?|credit(?:ed)?|paid|received|spent|purchas(?:e|ed)|withdr(?:awn|awal)|transfer(?:red)?)
```

### Account Number (Last 4)
```regex
(?:A\/[Cc]|[Aa]cct?|[Aa]ccount)\.?\s*[xX*]+(\d{4})
```

### Date
```regex
# DD-MMM-YY or DD-MMM-YYYY
(\d{1,2}[-/][A-Za-z]{3}[-/]\d{2,4})

# DD/MM/YYYY
(\d{2}/\d{2}/\d{4})
```

### UPI Reference
```regex
(?i)(?:ref|rrn|utr|txn)\s*(?:no\.?\s*)?:?\s*([A-Z0-9]{8,})
```

## Error Handling
- If a parser returns `null`, try the next parser in `ParserRegistry`
- If all parsers fail (including generic), log the message for analysis but don't show a banner
- Never crash on unparseable messages
- Log anonymized parse failures for improving patterns

## Security Considerations
- Never log full account numbers or card numbers
- Only store last 4 digits of account
- SMS content is processed locally, never sent to any server
- Notification access requires explicit user permission
