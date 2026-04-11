# Expense Analyst — Handoff

**Last updated**: 2026-04-11
**DB version**: 13
**Build**: `./gradlew clean assembleDebug` ✅ | Installed on SM-S948B ✅

---

## Session Summary (2026-04-11)

### 1. Saudi Energy + Ejar Bill Parsers

Two new `BillStatementParser` implementations:

**`SaudiEnergyStatementParser`** — English SMS
- Fingerprint: `se.com.sa` or `your bill for account` in body
- Extracts: amount from `"amount of X SAR"`, account number from `"account XXXXXXXXXX"`
- Sets `billerName = "Saudi Energy"`, `reference = accountNumber`

**`EjarStatementParser`** — Arabic SMS
- Fingerprint: `منصة إيجار` or `checkout.ejar.sa` in body
- Extracts: amount from `بقيمة X ريال`, contract from `عقد رقم XXXXXXXXXX`
- Sets `billerName = "Ejar"`, `reference = contractNumber`

Both registered in `BillStatementParserRegistry` before `GenericStatementParser`. Bill parser count: **9 total**.

---

### 2. `Bill.reference` Field (DB v12 → v13)

New nullable `reference: String?` field on `Bill` (domain), `BillEntity` (data), `ParsedBillStatement` (parser DTO).

**Migration** (`MIGRATION_12_13`):
```sql
ALTER TABLE bills ADD COLUMN reference TEXT
```

**Full stack threaded through**: `BillEntity` → `BillRepositoryImpl` (both mappers) → `BillStatementManager` (new bills + updates) → `BillDetailScreen` (shows "Reference: #XXXXXXXXXX" row when non-null).

---

### 3. Edit Bill Screen

New `EditBillScreen` / `EditBillViewModel` / `EditBillUiState` in `feature/expenses`.

Fields: biller name, reference, total due + currency, minimum due, due date (DatePicker), status (PENDING/PARTIAL/SETTLED dropdown).

Route: `NavRoutes.EDIT_BILL = "edit_bill/{billId}"` / `NavRoutes.editBill(billId)`.
`BillDetailScreen` has an edit pencil icon in the TopAppBar → navigates to `EditBillScreen`.

---

### 4. Account Management — Delete with Expense Remap

`AccountManagementScreen` (Settings) now fully implements delete with remap:

- Delete dialog loads full expense list via `ExpenseRepository.getExpensesByAccount()`
- Shows merchant, date, amount for each linked expense in a scrollable list
- Each expense row is tappable → navigates to `EditExpenseScreen` for review
- Remap dropdown: choose another account or "Unassign (no account)"
- On confirm: `ExpenseRepository.remapAccount(fromId, toId?)` then `AccountRepository.deleteAccount(id)`

**New DAO methods added** (`ExpenseDao`):
- `getExpensesByAccount(accountId)` — `@Transaction` query, returns `List<ExpenseWithCategory>`
- `countByAccount(accountId)` — for edit dialog subtitle
- `remapAccount(fromAccountId, toAccountId?)` — bulk UPDATE

**Compose gotcha discovered**: `LazyColumn` inside `AlertDialog`'s `text` slot renders nothing (the dialog's own `Column` is already in a scroll container; nested lazy lists measure at zero height). Fix: use `Column + verticalScroll(rememberScrollState())` with `heightIn(max = Xdp)`.

**`feature/settings/build.gradle.kts`**: Added `implementation(libs.kotlinx.datetime)` — required because `Expense.date` is `kotlinx.datetime.Instant` and the settings module now uses it directly via `DateTimeUtil`.

---

## Open Issues

| Issue | File | Notes |
|-------|------|-------|
| Hardcoded `"SAR"` home currency check | `ExpenseDetailScreen.kt` ~line 240 | Should use `uiState.homeCurrency` from `CurrencyRepository` |
| `DuckDuckGoApiService.kt` dead code | `data/` | Unused since Google Places replaced it — safe to delete |
| ProGuard/R8 rules | missing | Release build unverified |
| "Unknown Bank" account names | Account matching | SMS-inferred accounts have auto-generated names; no cleanup UX |

---

## First Action for Next Agent

**Fix hardcoded SAR in `ExpenseDetailScreen`** (~line 240):

```kotlin
// Wrong:
if (expense.currencyCode != "SAR") { ... }

// Fix:
if (expense.currencyCode != uiState.homeCurrency) { ... }
```

Steps:
1. Read `ExpenseDetailViewModel.kt` + `ExpenseDetailUiState.kt`
2. Inject `CurrencyRepository`, collect `getHomeCurrency()` into the `combine`
3. Add `homeCurrency: String = "SAR"` to `ExpenseDetailUiState`
4. Fix the condition in `ExpenseDetailScreen`
5. `./gradlew clean assembleDebug` + verify (set home currency to INR in Settings)

**Scope**: 3 files, no DB migration.

---

## Gotchas / Surprises

- **`LazyColumn` in AlertDialog**: Always use `Column + verticalScroll` for any scrollable content inside an `AlertDialog`'s text slot.
- **`kotlinx-datetime` classpath**: If a `:feature` module uses `Expense.date` (type `Instant`) directly, it needs `implementation(libs.kotlinx.datetime)` — transitivity is not sufficient for the Kotlin compiler to resolve the type.
- **Two-group regex**: `groupValues[1]` is `""` (not `null`) when only group 2 matches. Always `.takeIf { it.isNotBlank() }` on both groups in amount patterns.
- **KSP + clean**: Always `./gradlew clean assembleDebug` after adding new files. Never bare `assembleDebug`.
