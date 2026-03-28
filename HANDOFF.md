# Expense Analyst — Handoff Document

**Last updated**: 2026-03-28
**DB version**: 9
**Build status**: `./gradlew clean assembleDebug` passes
**Device tested**: Samsung S24 Ultra (SM-S948B), connected via ADB

---

## What Was Built in the Last Session (2026-03-28)

### Parser Improvements
- **`AlRajhiParser`**: Added content-based `canParse()` — now detects Al Rajhi SMS when sent from a regular phone number (not just the bank sender ID). Fixed `atPattern` regex to stop at newlines so "ALDREES 8" is correctly extracted. Detects payment method (Apple Pay, Samsung Pay, Google Pay) from `;Visa-Apple Pay` syntax in the card line.
- **`GenericParser`**: Now extracts merchant from `At: X` patterns and account last-4 from `Card:XXXX` — useful as a fallback for any bank SMS with these patterns.

### Payment Method as First-Class Field
- Added `APPLE_PAY`, `SAMSUNG_PAY`, `GOOGLE_PAY` to `PaymentMethod` enum with `.label` property (all values now have labels; `.label` replaces `.name.replace("_", " ")` everywhere).
- `ParsedTransaction`: added `paymentMethodName: String?` (stores enum name e.g. "APPLE_PAY") and `rawBody: String?`.
- `AlRajhiParser` sets `paymentMethodName` when a wallet is detected; `bankName` stays "Al Rajhi Bank" — the actual bank account, not the payment method.
- Payment method flows: `ParsedTransaction` → `PendingNotification.paymentMethod` → nav arg `paymentMethod` → `AddExpenseViewModel` sets the correct chip.
- Selected payment method chip always appears first in the LazyRow.

### Raw SMS Preview in Add Expense
- `ParsedTransaction.rawBody` is set immediately after parsing in `SmsReceiver`/`SmsTestReceiver` (`.copy(rawBody = body)`).
- `PendingNotification.rawBody` stored in DB column `raw_body` (migration v7→v8).
- `AddExpenseViewModel` loads `rawBody` from the pending notification via `pendingId`, sets `AddExpenseUiState.rawSmsBody`.
- `AddExpenseScreen` shows a collapsible **"Source SMS"** card at the bottom — visible only when `rawSmsBody` is not null (auto-detected expenses only). Manual adds never see it.

### pendingId Threading (Source SMS available everywhere)
- `PendingNotificationManager` now injects `@ApplicationContext` and posts the system tray notification *inside* the save coroutine (after DB insert returns the ID). This ensures `pendingId` is always available in the notification tap intent.
- `TransactionAlertNotification` has new extras: `EXTRA_PENDING_ID`, `EXTRA_PAYMENT_METHOD`.
- `MainActivity.handleIntent()` reads both extras and passes them to the nav route.
- Source SMS card now appears when adding from: system tray notification, in-app banner, OR pending inbox — all three paths.
- `SmsReceiver`, `SmsTestReceiver`, `TransactionNotificationService` no longer call `TransactionAlertNotification.post()` directly — the manager handles it after save.

### Account Matching Improvements
- Match logic requires **both** bank name and last-4 to match when both are known. Old OR logic caused "Apple Pay *7573" to incorrectly match "Al Rajhi Bank *7573".
- When payment method is a digital wallet and the matched account is not CREDIT_CARD, the account is **upgraded in-place** to CREDIT_CARD with a rebuilt display name.
- New accounts created via wallet payments default to `AccountType.CREDIT_CARD`.

### DB Migrations This Session
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
| SMS Import | ✅ | Bulk (all / last 30d), deduped by (amount, day), merchant rules applied |
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

### Parser Status
| Parser | Detection | Notes |
|--------|-----------|-------|
| AlRajhiParser | Sender OR body content | Merchant from At:, Apple/Samsung/Google Pay detection |
| StcBankParser | Sender | ✅ |
| AlinmaParser | Sender | ✅ |
| D360Parser | Sender | ✅ |
| HdfcParser | Sender | ✅ |
| SbiParser | Sender | ✅ |
| IciciParser | Sender | ✅ |
| AxisParser | Sender | ✅ Multi-currency |
| KotakParser | Sender | ✅ |
| YesBankParser | Sender | ✅ |
| WalletParser | Sender | ✅ |
| UpiParser | Sender | ✅ |
| GenericParser | Always (fallback) | ✅ At: merchant + Card: account extraction |

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
