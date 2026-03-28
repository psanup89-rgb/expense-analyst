# Notification Parsing - Standard Operating Procedure

## Current Status
The notification parsing system is **fully implemented and active**. 17 bank/wallet parsers are in production, handling real-time notification capture and bulk SMS import.

## Overview
The notification parsing system intercepts bank SMS and push notifications, extracts transaction data using regex parsers, and presents pre-filled expense entries for user confirmation.

## Architecture

```
Notification/SMS arrives
  ↓
TransactionNotificationService (NotificationListenerService)
  ↓ extracts text + sender
ParserRegistry.parse(sender, messageText)
  ↓ dispatches to matching parser (first match wins)
BankSpecificParser.parse(sender, body)
  ↓ returns
ParsedTransaction (amount, currency, type, merchant, accountLast4, paymentMethodName, bankName, rawBody)
  ↓
PendingNotificationManager.enqueue(parsed)
  ↓ saves to Room, posts system tray notification
NotificationBanner (in-app) + System tray notification
  ↓ user taps
AddExpenseScreen (pre-filled with parsed data, Source SMS card shown)
```

## Key Files
- `feature/notification/service/TransactionNotificationService.kt` — Android NotificationListenerService
- `feature/notification/parser/TransactionParser.kt` — Parser interface
- `feature/notification/parser/ParserRegistry.kt` — Dispatcher (ordered list, first match wins)
- `feature/notification/parser/<Bank>Parser.kt` — Bank-specific parsers (17 total)
- `feature/notification/parser/PaymentMethodDetector.kt` — Shared utility for inferring payment method from SMS text
- `feature/notification/parser/ParsedTransaction.kt` — Parsed result model
- `feature/notification/service/PendingNotificationManager.kt` — Saves to Room + posts tray notification
- `feature/notification/service/TransactionAlertNotification.kt` — System tray notification builder
- `feature/notification/ui/SmsImportViewModel.kt` — Bulk SMS import with dedup
- `feature/notification/ui/SmsImportScreen.kt` — Import UI (choose mode, progress, results)

## ParsedTransaction Model

```kotlin
data class ParsedTransaction(
    val amount: Double,
    val currencyCode: String,              // e.g., "INR", "SAR", "AED"
    val type: TransactionDirection,        // DEBIT, CREDIT, or PAYMENT
    val merchant: String?,
    val accountLast4: String?,
    val referenceNumber: String?,
    val bankName: String,
    val rawBody: String? = null,           // original SMS text
    val paymentMethodName: String? = null  // PaymentMethod enum name, e.g. "CREDIT_CARD", "UPI"
)
```

## TransactionParser Interface

```kotlin
interface TransactionParser {
    /** Human-readable bank name (e.g. "HDFC Bank", "Al Rajhi Bank") */
    val bankName: String

    /** Returns true if this parser can handle the given sender + body */
    fun canParse(sender: String, body: String): Boolean

    /** Parse the SMS. Returns null if parsing fails. */
    fun parse(sender: String, body: String): ParsedTransaction?
}
```

## PaymentMethodDetector

Shared utility (`PaymentMethodDetector.kt`) that infers payment method from SMS body text. Used by all parsers to set `paymentMethodName`.

| SMS Text Clue | Detected PaymentMethod |
|---|---|
| "Apple Pay" | `APPLE_PAY` |
| "Samsung Pay" | `SAMSUNG_PAY` |
| "Google Pay" | `GOOGLE_PAY` |
| "UPI", "UPI/P2M", "UPI Ref", "UPI:" | `UPI` |
| "NEFT", "IMPS", "RTGS", "Net Banking", "BBPS", "ACH D-" | `NET_BANKING` |
| "Credit Card", "CC no" | `CREDIT_CARD` |
| "Debit Card", "Fx Card", "Forex Card" | `DEBIT_CARD` |

Priority: Wallet overlays > UPI > Net Banking > Credit Card > Debit Card.

Some parsers add context-aware fallbacks (e.g. IDFC CC spend → `CREDIT_CARD`, FASTag → `WALLET`, OneCard → `CREDIT_CARD`).

## Supported Parsers (17 total)

### ParserRegistry Order (first match wins)

| # | Parser | Bank | Detection | Currency | Key Patterns |
|---|--------|------|-----------|----------|--------------|
| 1 | HdfcParser | HDFC Bank | Sender `hdfc` | INR | `Rs.X debited/credited`, UPI, NEFT, card payment |
| 2 | SbiParser | SBI | Sender `sbi` | INR | `Rs X debited from A/c`, `Info:`, card payment |
| 3 | IciciParser | ICICI Bank | Sender `icici` | INR | `Acct XX1234 debited`, `Info:`, card payment |
| 4 | AxisParser | Axis Bank | Sender `axis` | Multi | `Rs.X debited/credited`, Forex (`SAR/USD/AED/EUR/GBP`), UPI/P2M compact |
| 5 | KotakParser | Kotak Bank | Sender `kotak` | INR | `INR X debited from A/c`, `via UPI to` |
| 6 | YesBankParser | Yes Bank | Sender `yes` + body `YES BANK` | INR | `Ac X2919 debited`, UPI `/To:`, NEFT `/From:` |
| 7 | IdfcFirstBankParser | IDFC First Bank | Sender `idfcfb` | INR | CC spend (fun prefixes), savings debit/credit, card payment, interest |
| 8 | OneCardParser | OneCard (Federal Bank) | Sender `onecrd` | Multi | `paid X at MERCHANT`, payment received, refund |
| 9 | AlRajhiParser | Al Rajhi Bank | Sender OR body content | SAR | `PoS/Online Purchase`, `;Visa-Apple Pay`, `At:` merchant |
| 10 | StcBankParser | STC Bank | Sender `stc` | SAR | `SAR X paid to`, `received` |
| 11 | AlinmaParser | Alinma Bank | Sender `alinma` | SAR | `card ending X used for SAR X at` |
| 12 | D360Parser | D360 Bank | Sender `d360` | SAR | `SAR X paid to`, Transaction ID |
| 13 | EmiratesNbdParser | Emirates NBD | Sender OR body fingerprint | Multi | `POS/Online Purchase`, `Card: Visa card XX4388`, `Amount: SAR X`, `Merchant:` |
| 14 | FasTagParser | FASTag (LivQuik) | Sender `qwfstg` | INR | `debited RsX for VEHICLE in LOCATION at DATE` |
| 15 | WalletParser | Digital Wallet | Sender Apple/Google/Samsung Pay | Multi | `Payment of $X at`, `Paid X to` |
| 16 | UpiParser | UPI (GPay/PhonePe/Paytm) | Sender or `UPI` in body | INR | `paid ₹X to`, `received from` |
| 17 | GenericParser | Unknown (fallback) | Always matches | Multi | Best-effort: `At:` merchant, `Card:` account, amount + direction |

## SMS Import (Bulk)

The SMS import feature (`SmsImportViewModel`) reads SMS from the device inbox and bulk-creates expenses.

### Import Modes
- **Last 30 days**: Quick import of recent transactions
- **All time**: Full history import
- **Browse**: Manual single-message selection

### Dedup Logic
Two-tier duplicate detection prevents re-importing existing expenses:

1. **Primary (exact match)**: Hash of raw SMS body text — if the exact same SMS was already imported, skip it
2. **Fallback (for old records without rawSmsBody)**: `amount + calendar day + merchant name` — catches approximate duplicates

The import does NOT clear existing data. It only adds new expenses that don't match existing ones.

### Import Pipeline
```
querySmsInbox() → filter by financialKeywords
  → for each SMS:
    → ParserRegistry.parse(sender, body)
    → dedup check (body hash → fallback key)
    → PaymentMethod from parsed.paymentMethodName
    → AccountType inference from SMS body keywords
    → accountRepository.findOrCreate(bank, last4, type)
    → CategoryInference.infer(merchant, bank, categories, smsBody, merchantRules)
    → CurrencyConversion.resolve() for home amount
    → batch save via expenseRepository.addExpenses()
```

## SOP: Adding a New Bank Parser

### Step 1: Collect SMS Samples
- Collect 10+ real SMS samples from the bank (anonymize account numbers)
- Include: debit, credit, UPI, card transactions, payment confirmations
- Identify the sender ID (e.g., "HDFCBK", "AD-AXISBK-S", "EmiratesNBD")

### Step 2: Create the Parser
1. Create `feature/notification/parser/<BankName>Parser.kt`
2. Implement `TransactionParser` interface:
   - `bankName`: human-readable name
   - `canParse(sender, body)`: sender regex match (and/or body fingerprint for multi-sender banks)
   - `parse(sender, body)`: extract amount, currency, type, merchant, accountLast4
3. Use `PaymentMethodDetector.detect(body)` for payment method (with context-aware fallback if needed)
4. Follow existing parser patterns — see `AlRajhiParser` or `HdfcParser` as examples

### Step 3: Register in ParserRegistry
Add the new parser to `ParserRegistry.parsers` list **before** GenericParser (which is always last):
```kotlin
// In ParserRegistry.kt
companion object {
    private val parsers: List<TransactionParser> = listOf(
        HdfcParser(),
        SbiParser(),
        // ... bank-specific parsers ...
        EmiratesNbdParser(),
        FasTagParser(),
        YourNewParser(),    // ← add here
        WalletParser(),
        UpiParser(),
        GenericParser()     // always last (fallback)
    )
}
```

### Step 4: Write Tests
1. Create `feature/notification/src/test/kotlin/.../parser/<BankName>ParserTest.kt`
2. Use `@ParameterizedTest` with `@CsvSource` for multiple SMS samples
3. Test: `canParse()` true/false, amount, currency, type, merchant, accountLast4, paymentMethodName
4. Test edge cases: missing fields, different formats, non-transaction SMS (OTP, promotions)

### Step 5: Update bankDisplayNameFromSender
In `SmsImportViewModel.kt`, add the new bank's sender ID pattern to `bankDisplayNameFromSender()` so bulk import shows the correct bank name.

### Step 6: Build & Test
```bash
./gradlew clean assembleDebug                              # Full build
./gradlew :feature:notification:testDebugUnitTest          # Parser tests
```

## Common Regex Patterns

### Amount Extraction
```regex
# Indian (INR)
(?:Rs\.?\s*|₹\s*|INR\s*)([\d,]+(?:\.\d{2})?)

# Saudi (SAR)
(?:SAR\s*|ر\.س\s*)([\d,]+(?:\.\d{2})?)

# Multi-currency (two-group pattern — IMPORTANT: use .takeIf { it.isNotBlank() } for both groups)
(?:rs\.?|inr|sar|usd|aed|eur|gbp)\s*([\d,]+\.?\d*)|([\d,]+\.?\d*)\s*(?:rs\.?|inr|sar|usd|aed|eur|gbp)
```

### Account Number (Last 4)
```regex
# Common patterns: "a/c XX1234", "Card:7573", "card ending 1041", "Acct XX9876", "CC no. XX4502"
(?i)(?:acct|card|a/c|cc)\s*(?:no\.?|ending(?:\s+with)?)?\s*[xX*\s]+(\d{3,4})
```

### Transaction Type
```regex
(?i)(debit(?:ed)?|credit(?:ed)?|paid|received|spent|purchas(?:e|ed)|withdr(?:awn|awal)|transfer(?:red)?)
```

### UPI Reference
```regex
(?i)(?:ref|rrn|utr|txn)\s*(?:no\.?\s*)?:?\s*([A-Z0-9]{8,})
```

## Parser Bug to Avoid
Two-group amount regex: `groupValues[1]` is `""` (empty string, not null) when only group 2 matches. Always use:
```kotlin
val amount = (match.groupValues[1].takeIf { it.isNotBlank() }
    ?: match.groupValues[2].takeIf { it.isNotBlank() })
    ?.replace(",", "")?.toDoubleOrNull()
```

## Error Handling
- If a parser returns `null`, ParserRegistry tries the next parser
- If all parsers fail (including GenericParser), the SMS is silently skipped
- Never crash on unparseable messages
- During bulk import: failed count is tracked and shown in results

## Security Considerations
- Never log full account numbers or card numbers
- Only store last 4 digits of account
- SMS content is processed locally, never sent to any server
- Notification access requires explicit user permission
