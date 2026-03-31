# Expense Analyst — Handoff Document

**Last updated**: 2026-03-31 (session 2)
**DB version**: 12 (bumped from 11 — `is_possible_duplicate` column on `pending_notifications`)
**Build status**: `./gradlew clean assembleDebug` ✅ passing
**Device**: Samsung Galaxy S26 Ultra (SM-S948B) — install pending (device disconnected at session end)
**App version**: 0.1.0 (alpha)

---

## What Was Accomplished This Session (session 2 — 2026-03-31)

### 1. New `AxisBankStatementParser` — Axis CC bill detection

**Message**:
```
Payment of INR 8523.16 for Axis Bank Credit Card no. XX3745 is due on 04-04-26
with minimum amount due of INR 8523.16. Ignore if paid.
```
Parser matches sender `AXISBK`/`axis` + body fingerprint `credit card.*due on`. Extracts total due, min due, due date (`dd-MM-yy` → epoch), card XX last-4. Registered in `BillStatementParserRegistry` before EmiratesNBD.

---

### 2. `AxisParser` — 3 new detection patterns

- **"Debit INR X" + "NACH debit"**: Changed `\bdebited\b` → `\bdebit(?:ed)?\b` so non-past-tense forms are caught
- **ACH merchant**: `ACH-DR-MONTHLYSMALLCAS-000` → added `achMerchantPattern` (`ACH-(?:DR|CR)-([A-Z0-9]+)-\d`)
- **NACH merchant**: `NACH debit towards MERCHANTNAME for INR` → added `nachMerchantPattern`

---

### 3. Soft Duplicate Detection (DB v11 → v12)

**Problem**: Same NACH debit SMS arrives twice (bank sends two different wordings for the same transaction). Currently both are saved to the pending inbox with no warning.

**Solution**: After the existing raw-body dedup check in `PendingNotificationManager.enqueue()`, a soft check runs:
- Queries `getExpensesSnapshot()` for any expense matching **same amount + same merchant (case-insensitive) + same calendar day**
- If match found: sets `isPossibleDuplicate = true` on both the `ParsedTransaction` (for the live banner) and the saved `PendingNotification` (for the inbox)

**DB change** (`MIGRATION_11_12`):
```sql
ALTER TABLE pending_notifications ADD COLUMN is_possible_duplicate INTEGER NOT NULL DEFAULT 0
```

**UI changes**:
- `NotificationBanner`: amber subtitle "⚠ Possible duplicate — already logged today"; "Save" → "Add Anyway"
- `PendingInboxScreen`: orange "⚠ Possible duplicate" badge chip on the card; "Add Expense" → "Add Anyway"

**Files changed**:
- `domain/.../model/PendingNotification.kt` — added `isPossibleDuplicate: Boolean = false`
- `feature/notification/.../parser/ParsedTransaction.kt` — added `isPossibleDuplicate: Boolean = false`
- `data/.../entity/PendingNotificationEntity.kt` — added column with `defaultValue = "0"`
- `data/.../repository/PendingNotificationRepositoryImpl.kt` — mapper updated
- `data/.../local/ExpenseAnalystDatabase.kt` — bumped to v12, added `MIGRATION_11_12`
- `feature/notification/.../service/PendingNotificationManager.kt` — soft-dupe logic
- `feature/notification/.../ui/NotificationBanner.kt` — amber warning + button text
- `feature/notification/.../ui/PendingInboxScreen.kt` — badge chip + button text

---

## Previous Session (session 1 — 2026-03-31)

### Crash Fix — Room Migration `MIGRATION_10_11` (DB v11)

**Problem**: App crashed on open with:
```
IllegalStateException: Migration didn't properly handle:
expenses(com.expenseanalyst.data.local.entity.ExpenseEntity)
```
The tags-system migration (added by a previous agent) contained two SQL statements that read a `note` column from `expenses` — but that column never existed (the entity uses `description`). SQLite threw `no such column: note`, which Room surfaced as the migration error.

**Fix**: Removed the two dead SQL statements from `MIGRATION_10_11`:
- `INSERT OR IGNORE INTO tags ... SELECT DISTINCT TRIM(note) FROM expenses ...`
- `INSERT INTO expense_tags ... INNER JOIN tags t ON TRIM(e.note) = t.name ...`

The table creation + default tag seeding in the same migration is correct and unchanged.

**File**: `data/src/main/java/com/expenseanalyst/data/local/ExpenseAnalystDatabase.kt`

---

### 2. New Parser: `IdfcFirstBankStatementParser` (Bill)

**Message format**:
```
Your Mayura Credit Card XX6887 bill due by 06 April, 2026
Total Due: INR 5031.37
Min Due: INR 5031.37
Pay: https://idfcfr.in/i20etK
IDFC FIRST Bank
```

**Why it was missed**: `GenericStatementParser.canParse()` requires both "statement/bill generated/bill ready" AND "total due" keywords. The IDFC format uses "bill due by" (not "bill generated"), so the generic parser's `statementKeyword` pattern failed to match.

**Fix**: New dedicated `IdfcFirstBankStatementParser` that detects sender matching `idfcfb|idfc first` + body containing "bill due by". Extracts Total Due, Min Due, due date (handles "dd MMMM, yyyy" / "d MMMM yyyy" formats), and card last-4.

**Registered**: First in `BillStatementParserRegistry` (before `EmiratesNbdStatementParser`).

---

### 3. EmiratesNBD — Credit Card Payment Detection

**Message format**:
```
Credit Card: Credited
Card : XX4388;Credit Card Visa
Amount: SAR 39.00
Balance: SAR 18,156.95
Date: 29-03-2026
```

**Why it was missed**: `EmiratesNbdParser.parse()` had an early-return guard `if (!purchasePattern.containsMatchIn(body)) return null`. "Credit Card: Credited" messages don't contain "POS Purchase" or "Online Purchase" so the guard fired immediately.

**Additionally**: The card pattern `Card:\s*(?:Visa|Credit|...)?\s*card\s*XX(\d{4})` didn't match the format `Card : XX4388;Credit Card Visa` (number comes before type descriptor).

**Fix**: Added `creditedPattern` + `creditedCardPattern` to `EmiratesNbdParser`. A new branch runs before the purchase guard, matches "Credit Card: Credited", extracts amount + card last-4, and returns `TransactionDirection.PAYMENT`.

**File**: `feature/notification/src/main/java/com/expenseanalyst/feature/notification/parser/EmiratesNbdParser.kt`

---

### 4. New Parser: `TamaraStatementParser` (Bill)

**Message format** (sender: "Tamara Due"):
```
Reminder! you have a payment of 516.51 SAR for your Samsung order due in 2 days.
Pay now to improve your credit limits: https://tamara.go.link/aQRmC
```

**Fix**: New `TamaraStatementParser` implementing `BillStatementParser`. Matches sender containing "Tamara" OR body containing "tamara.go.link". Extracts:
- Amount + currency from `payment of X SAR`
- Merchant from `for your Samsung order` → biller = "Tamara – Samsung"
- Due date computed as `System.currentTimeMillis() + N * 86_400_000` from "due in 2 days"

**Registered**: In `BillStatementParserRegistry` after `HdfcStatementParser`, before `GenericStatementParser`.

---

### 5. HdfcParser — "Spent" Keyword

**Message format**:
```
Spent Rs.2 On HDFC Bank Card 1041 At GOOGLE CLOUD On 2026-03-30:02:14:36
```

**Why it was missed**: `HdfcParser` checked `\b(?:debited|deducted|sent)\b` for debit detection. The HDFC "Spent Rs.X On HDFC Bank Card YYYY At MERCHANT" format uses "Spent" — not in the list.

**Fix**: Added `|spent` to the debit keyword regex. One-word change.

**File**: `feature/notification/src/main/java/com/expenseanalyst/feature/notification/parser/HdfcParser.kt`

---

## What Was NOT Finished

Nothing incomplete. APK built successfully but not installed — device was disconnected. Run `./gradlew installDebug` when device is reconnected.

**Carried-over known issues**:
- Hardcoded `"SAR"` in `ExpenseDetailScreen.kt` (~line 240) — checks `expense.currencyCode != "SAR"` instead of actual home currency
- `DuckDuckGoApiService.kt` in `:data` — dead code, unused since Google Places replaced it

---

## First Action for Next Agent

**Fix hardcoded home currency in `ExpenseDetailScreen`** (~line 240).

```kotlin
// Wrong:
if (expense.currencyCode != "SAR") { ... }

// Fix:
if (expense.currencyCode != uiState.homeCurrency) { ... }
```

Steps:
1. Read `ExpenseDetailViewModel.kt` and `ExpenseDetailUiState.kt`
2. Inject `CurrencyRepository`, collect `getHomeCurrency()` into the `combine`
3. Add `homeCurrency: String = "SAR"` to `ExpenseDetailUiState`
4. Fix the condition in `ExpenseDetailScreen`
5. `./gradlew clean assembleDebug` + install + verify (set home currency to INR in Settings)

**Scope**: 3 files, no migration needed.

---

## Surprises / Gotchas

### 1. `java.util.Properties` in Gradle Kotlin DSL
`java` resolves to the Java plugin extension, not `java.util`. Use line-based file reading instead:
```kotlin
rootProject.file("local.properties").readLines().find { it.startsWith("KEY=") }?.substringAfter("=")?.trim()
```
See skill: `buildconfig-secret-from-local-properties.md`

### 2. `@OptIn` on outer composable doesn't cover nested lambdas
`FlowRow` (`@ExperimentalLayoutApi`) inside a content lambda of a composable annotated with `@OptIn` still raises a compiler error at the lambda site. Fix: extract into a private composable with its own `@OptIn`.
See skill: `compose-experimental-in-nested-lambda.md`

### 3. Room migration `note` column bug (don't repeat)
`MIGRATION_10_11` originally included SQL reading `expenses.note` — a column that doesn't exist. The `expenses` table uses `description`. The migration ran, SQLite threw on the INSERT, and Room reported it as "Migration didn't properly handle: expenses". Always verify column names against the entity before writing migration SQL.
