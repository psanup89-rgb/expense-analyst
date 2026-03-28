# Expense Analyst — Handoff Document

**Last updated**: 2026-03-28
**DB version**: 9
**Build status**: `./gradlew clean assembleDebug` passes
**Device tested**: Samsung S26 Ultra (SM-S948B), connected via ADB

---

## What Was Built in the Latest Session (2026-03-28, continued)

### New Parsers (4 added)
- **`EmiratesNbdParser`**: Emirates NBD (UAE). Handles POS Purchase and Online Purchase formats. Extracts merchant from `Merchant:`, card from `Card: Visa card XX4388`, currency from `Amount: SAR X`. Detects wallet overlays from `(Apple Pay)` on first line. Infers card type (Credit/Debit/Mada) from `Card:` line.
- **`IdfcFirstBankParser`**: IDFC First Bank (India). 5 patterns: CC spend (with fun prefixes like "Delicious Purchase!"), savings debit/credit, card payment confirmation, interest credit. Handles `INR.62.00` (dot after INR) format.
- **`FasTagParser`**: FASTag/LivQuik toll and parking (India). Extracts toll location from `in LOCATION at DATE` and parking location from `at LOCATION, DATE`. PaymentMethod hardcoded to `WALLET`.
- **`OneCardParser`**: OneCard/Federal Bank credit card (India). Handles spend (`paid X at MERCHANT`), payment received, refund. Multi-currency (INR, AED, USD, EUR, GBP).

### Parser Fixes (6 modified)
- **`AxisParser`**: Added UPI compact merchant extraction (`UPI/P2M/txnid/MERCHANT Not you?`). Broadened account pattern for `A/c no. XX0426`, `CC no. XX4502`.
- **`HdfcParser`**: Broadened account pattern for `HDFC Bank XX7823`, `A/C No 7823`, `card ending 1041`, `ENDING WITH 1041`. Added PAYMENT type for card payment confirmations. Added merchant extraction from `Info:`, `towards...UMRN`, `For IMPS/NEFT -NAME-`.
- **`IciciParser`**: Added `card` to account pattern for `Credit Card XX9008`. Added PAYMENT type for `Payment received on Credit Card`.
- **`SbiParser`**: Broadened account pattern to handle `Card ending XX83` (2-4 digit account numbers). Added PAYMENT type detection.
- **`YesBankParser`**: Account pattern now matches `Ac X2919` (single X prefix). Added UPI/NEFT merchant extraction from `/To:` and `/From:` patterns.
- **`AlRajhiParser`**: Added `*` to atPattern character class for merchants like `GOOGLE*PA`, `OPENAI *C`.

### PaymentMethodDetector (new shared utility)
- Created `PaymentMethodDetector.kt` — centralized payment method inference from SMS body text.
- Detects: APPLE_PAY, SAMSUNG_PAY, GOOGLE_PAY, UPI, NET_BANKING, CREDIT_CARD, DEBIT_CARD.
- **All 15 parsers now set `paymentMethodName`** using this detector (previously only Al Rajhi and Emirates NBD did).
- Context-aware fallbacks: IDFC CC spend → `CREDIT_CARD`, FASTag → `WALLET`, STC Bank → `WALLET`, OneCard → `CREDIT_CARD`, UpiParser → `UPI`.

### SMS Import Improvements
- **Payment method now used during import**: Previously hardcoded `PaymentMethod.OTHER` for all imported SMS. Now maps `parsed.paymentMethodName` → `PaymentMethod` enum.
- **Smarter dedup**: Primary check = exact SMS body hash (identical SMS = duplicate). Fallback = amount + day + merchant name (for old records without rawSmsBody). Previously was just amount + day, which incorrectly deduped two different transactions with the same amount on the same day.

### Previous Session (same date, earlier)

#### Parser Improvements
- **`AlRajhiParser`**: Added content-based `canParse()` — now detects Al Rajhi SMS when sent from a regular phone number (not just the bank sender ID). Fixed `atPattern` regex to stop at newlines so "ALDREES 8" is correctly extracted. Detects payment method (Apple Pay, Samsung Pay, Google Pay) from `;Visa-Apple Pay` syntax in the card line.
- **`GenericParser`**: Now extracts merchant from `At: X` patterns and account last-4 from `Card:XXXX` — useful as a fallback for any bank SMS with these patterns.

#### Payment Method as First-Class Field
- Added `APPLE_PAY`, `SAMSUNG_PAY`, `GOOGLE_PAY` to `PaymentMethod` enum with `.label` property (all values now have labels; `.label` replaces `.name.replace("_", " ")` everywhere).
- `ParsedTransaction`: added `paymentMethodName: String?` (stores enum name e.g. "APPLE_PAY") and `rawBody: String?`.
- Payment method flows: `ParsedTransaction` → `PendingNotification.paymentMethod` → nav arg `paymentMethod` → `AddExpenseViewModel` sets the correct chip.
- Selected payment method chip always appears first in the LazyRow.

#### Raw SMS Preview in Add Expense
- `AddExpenseScreen` shows a collapsible **"Source SMS"** card at the bottom — visible only when `rawSmsBody` is not null (auto-detected expenses only).

#### pendingId Threading (Source SMS available everywhere)
- `PendingNotificationManager` posts system tray notification *inside* the save coroutine (after DB insert returns the ID).
- Source SMS card now appears when adding from: system tray notification, in-app banner, OR pending inbox — all three paths.

#### Account Matching Improvements
- Match logic requires **both** bank name and last-4 to match when both are known.
- When payment method is a digital wallet and the matched account is not CREDIT_CARD, the account is **upgraded in-place** to CREDIT_CARD.

#### DB Migrations
- **v7→v8**: `ALTER TABLE pending_notifications ADD COLUMN raw_body TEXT`
- **v8→v9**: `ALTER TABLE pending_notifications ADD COLUMN payment_method TEXT`

---

## Full Implementation Status

### Modules (9 total)
`:app` · `:core` · `:domain` · `:data` · `:feature:expenses` · `:feature:emi` · `:feature:notification` · `:feature:settings` · `:feature:onboarding`

### Screens
| Screen | Status | Notes |
|--------|--------|-------|
| Onboarding (3-step) | ✅ | Welcome → currency → notification access. SMS bulk import offered at end. |
| Expense List | ✅ | Date-grouped, search, category+payment filters, month nav, swipe-delete+undo |
| Add Expense | ✅ | Merchant* (mandatory), Description, Note, 4 types, collapsible Source SMS card |
| Edit Expense | ✅ | Pre-filled, recalculates homeAmount on currency change |
| Expense Detail | ✅ | "Teach App" rule card (SMS_AUTO/NOTIFICATION_AUTO with merchant only), collapsible SMS |
| Pending Inbox | ✅ | Bottom nav tab with badge count, persists until added or dismissed |
| EMI Create/List/Detail | ✅ | Split with optional interest, timeline view, cancel remaining |
| SMS Import | ✅ | Bulk (all / last 30d), deduped by (SMS body hash → amount+day+merchant fallback), payment method from parsers, merchant rules applied |
| Settings | ⚠️ | Currency picker, notification toggle, test notification button. **Missing**: theme toggle, category management |

### Domain Models (`:domain`)
```
Expense          — amount, homeAmount, exchangeRate, merchantName*, description, note,
                   category, paymentMethod, transactionType, date, sourceType, accountId,
                   rawSmsBody, emiGroupId, emiInstallmentNumber, isDeleted
Account          — id, bankName, lastFour, accountType, displayName
MerchantRule     — id, merchantPattern, categoryId, categoryName, createdAt
PendingNotification — id, amount, currencyCode, merchantName, bankName, accountLast4,
                      transactionType, detectedAtMillis, rawBody, paymentMethod
Category         — id, name, iconName, colorHex, isDefault, sortOrder
EmiGroup         — id, totalAmount, currencyCode, numberOfInstallments, installmentAmount,
                   interestRate, startDate, description, categoryId, paymentMethod
CurrencyRate     — currencyCode, rateToBase, lastUpdatedUtcMillis
```

### Enums
- `PaymentMethod(label)`: CASH, UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING, WALLET, APPLE_PAY, SAMSUNG_PAY, GOOGLE_PAY, OTHER
- `TransactionType`: EXPENSE, INCOME, TRANSFER, PAYMENT
- `AccountType(label)`: SAVINGS, CURRENT, CREDIT_CARD, DEBIT_CARD, FOREX_CARD, WALLET, OTHER
- `SourceType`: MANUAL, SMS_AUTO, NOTIFICATION_AUTO
- `TransactionDirection`: DEBIT, CREDIT, PAYMENT (parser-internal, in `feature/notification`)

### Repository Interfaces (`:domain`)
`ExpenseRepository` · `CategoryRepository` · `CurrencyRepository` · `EmiRepository` · `OnboardingRepository` · `AccountRepository` · `MerchantRuleRepository` · `PendingNotificationRepository`

### DB Migration History
| Version | Change |
|---------|--------|
| v1→v2 | Add Misc category |
| v2→v3 | Add account_number to expenses; rename DEBIT→EXPENSE, CREDIT→INCOME |
| v3→v4 | Create accounts table; add account_id to expenses |
| v4→v5 | Add raw_sms_body to expenses |
| v5→v6 | Create merchant_rules table + unique index |
| v6→v7 | Create pending_notifications table |
| v7→v8 | Add raw_body column to pending_notifications |
| v8→v9 | Add payment_method column to pending_notifications |

### Notification Pipeline (current)
```
SMS / Notification received
  → SmsReceiver or TransactionNotificationService
  → ParserRegistry.parse(sender, body).copy(rawBody = body)
  → PendingNotificationManager.enqueue(parsed)
      → repository.save() → returns savedId
      → _lastPendingId.value = savedId
      → TransactionAlertNotification.post(context, parsed, savedId)
  → _pending StateFlow → NotificationBanner (in-app)
  → _lastPendingId StateFlow → NotificationBanner passes to nav

Tray tap intent → MainActivity.handleIntent()
  → reads EXTRA_PENDING_ID, EXTRA_PAYMENT_METHOD
  → MainViewModel.setPendingRoute(route with pendingId + paymentMethod)
  → AppNavGraph LaunchedEffect → navigates

AddExpenseViewModel.init:
  → reads paymentMethod nav arg → sets PaymentMethod chip
  → reads account nav arg → matches or creates account (bank+last4 both required)
  → reads pendingId → loads PendingNotification → sets rawSmsBody + paymentMethod
```

### Parser Status (17 parsers, ordered by ParserRegistry priority)
| # | Parser | Detection | Notes |
|---|--------|-----------|-------|
| 1 | HdfcParser | Sender `hdfc` | UPI, NEFT, card payment, `Info:` merchant, broad account patterns |
| 2 | SbiParser | Sender `sbi` | `Info:` merchant, card ending XX83, PAYMENT type |
| 3 | IciciParser | Sender `icici` | `Info:` merchant, Credit Card XX9008, PAYMENT type |
| 4 | AxisParser | Sender `axis` | Multi-currency (SAR/USD/AED/EUR/GBP), UPI/P2M compact merchant, Forex cards |
| 5 | KotakParser | Sender `kotak` | ✅ |
| 6 | YesBankParser | Sender `yes` + body `YES BANK` | `Ac X2919`, UPI `/To:`, NEFT `/From:` |
| 7 | IdfcFirstBankParser | Sender `idfcfb` | 5 patterns: CC spend, savings debit/credit, card payment, interest |
| 8 | OneCardParser | Sender `onecrd` | Multi-currency, spend/payment/refund |
| 9 | AlRajhiParser | Sender OR body content | `At:` merchant, Apple/Samsung/Google Pay, `GOOGLE*PA` merchants |
| 10 | StcBankParser | Sender `stc` | ✅ Fallback WALLET |
| 11 | AlinmaParser | Sender `alinma` | ✅ |
| 12 | D360Parser | Sender `d360` | ✅ |
| 13 | EmiratesNbdParser | Sender OR body fingerprint | POS/Online Purchase, `Card: Visa/Credit/Debit/Mada`, `Merchant:`, multi-currency |
| 14 | FasTagParser | Sender `qwfstg` | Toll + parking locations, hardcoded WALLET |
| 15 | WalletParser | Sender Apple/Google/Samsung Pay | ✅ Fallback WALLET |
| 16 | UpiParser | Sender or `UPI` in body | ✅ Fallback UPI |
| 17 | GenericParser | Always (fallback) | At: merchant + Card: account extraction, PaymentMethodDetector |

### Intelligence Engine
- `MerchantRule` persisted in `merchant_rules` table
- `CategoryInference.infer()`: user rules first (contains, case-insensitive) → keyword matching
- "Teach App" card on Expense Detail (visible for SMS_AUTO/NOTIFICATION_AUTO with merchant)
- `ExpenseDetailViewModel`: `existingRule: StateFlow<MerchantRule?>`, `saveRule()`, `deleteRule()`

---

## Known Gaps

| Gap | Priority | Notes |
|-----|----------|-------|
| Duplicate detection (live notifications) | Medium | Bulk import deduped; live notifications not |
| Settings: theme toggle | Low | Not implemented |
| Settings: category management | Low | Not implemented |
| Paging (Paging 3) | Low | Currently loads all records |
| Bills section | Medium | Track credit card statements; needs Bill model + migration + screen |
| Offline exchange rate entry | Low | No UI to manually enter rate when API unavailable |
| DAO/ViewModel/UI tests | Medium | Parser tests exist; no DAO/ViewModel/UI tests yet |
| ProGuard/R8 rules | Medium | Release build not verified |

---

## Phase 2 Backlog
Analytics dashboard · Budgets & alerts · CSV/PDF export · Google Drive backup · Home screen widget · Bulk operations

## Phase 3 Backlog
iOS (KMP) · Smart categorization · Recurring detection · Receipt photos · Split expenses · Income tracking

---

## Handoff Checklist for Next Agent
1. Read `CLAUDE.md` for all conventions, architecture rules, and critical build gotchas
2. Run `./gradlew clean assembleDebug` before making any changes (always use `clean`)
3. DB is at **v9** — any new schema change needs `MIGRATION_9_10` + version bump in `@Database(version = 10)`
4. After parser changes: `./gradlew :feature:notification:testDebugUnitTest`
5. See `.claude/skills/build-verify.md` if KSP fails
6. **Update this file** at end of every session
