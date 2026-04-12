# Expense Analyst — Handoff

**Last updated**: 2026-04-12 (session 2)
**DB version**: 14
**Build**: `./gradlew clean assembleDebug` ✅ | Installed on SM-S948B ✅

---

## Session Summary (2026-04-12, session 2) — Bills polish + home currency + navigation

### 1. Add Bill form parity with Edit Bill

`AddBillSheetContent` in `BillsScreen.kt` now shows the same fields as `EditBillScreen`: Reference, Total Due (with home-currency suffix), Minimum Due, Due Date (DatePicker), Status (dropdown). `BillsUiState` gained 4 new fields; `BillsViewModel` gained corresponding handlers.

### 2. Bills always stored in home currency

- `BillsViewModel` injects `CurrencyRepository`; `init` block and `saveNewBill()` always use home currency code from repository (currency input removed from Add Bill form)
- `PendingInboxViewModel.confirmSaveBill()` now injects `CurrencyRepository` and uses home currency when creating Bill records
- `AddExpenseViewModel.saveExpense()` bill status comparison always uses `computedHomeAmount ?: parsedAmount` (no currency-matching detour)

### 3. Bidirectional expense ↔ bill navigation

- `ExpenseDetailUiState` gains `linkedBillId: Long?`
- "Linked Bill" row in PAYMENT expense detail is a tappable button → navigates to `BillDetailScreen`
- `AppNavGraph` wires `onViewBill` on `ExpenseDetailScreen`; payment tap in `BillDetailScreen` goes to `ExpenseDetailScreen` (not edit)

### 4. Unlink payment from bill

- `BillDetailViewModel.unlinkPayment(expenseId)`: clears `expense.billId`, recalculates bill status (PENDING / PARTIAL)
- `PaymentItem` gains `LinkOff` icon button + confirm dialog
- After unlink, the payment's "Linked Bill" row reverts to "Link to Bill" button

---

## Session Summary (2026-04-12, session 1) — Bill SMS routing + PAYMENT bill linking (DB v14)

### 1. Bill reminder SMS → pending inbox as BILL type

`BillStatementManager` no longer auto-saves bills silently. It now enqueues a `PendingNotification` with `pendingType = "BILL"`, `billerName`, `dueDateMillis`, `linkedBillId` (if open bill found).

**DB MIGRATION_13_14** adds 4 columns to `pending_notifications`:
```sql
ALTER TABLE pending_notifications ADD COLUMN pending_type TEXT NOT NULL DEFAULT 'TRANSACTION'
ALTER TABLE pending_notifications ADD COLUMN biller_name TEXT
ALTER TABLE pending_notifications ADD COLUMN due_date_millis INTEGER
ALTER TABLE pending_notifications ADD COLUMN linked_bill_id INTEGER
```

`PendingInboxScreen` branches on `item.pendingType`: BILL items render `PendingBillItem` (biller, amount, due date, "Add as Bill" / "Update Bill" actions); TRANSACTION items render the existing card unchanged. `PendingInboxViewModel` gained `confirmSaveBill()` / `confirmUpdateBill()` with confirm dialogs.

### 2. Routing fix — bill SMS no longer misclassified as spend

- `TransactionNotificationService` now tries `BillStatementParserRegistry` **first** (before `ParserRegistry`)
- `AirtelStatementParser` added (10th bill parser): handles Airtel Wi-Fi/Postpaid/Broadband "bill of Rs.X is pending" SMS
- `GenericParser` guards against bill-reminder phrases via `billReminderPattern` (returns null for "ignore if already paid", "bill of Rs.X is pending", "minimum amount due", "bill payment reminder")

### 3. PAYMENT bill linking in Add/Edit Expense

- `loadBillsForLinking()` in both `AddExpenseViewModel` and `EditExpenseViewModel`: called when type = PAYMENT, fuzzy-matches open bills by merchant name, pre-populates `linkedBill` in state
- "Linked Bill" section in `AddExpenseContent`: shows auto-linked bill chip or "Link to a Bill" button; user can unlink or swap via bill picker `ModalBottomSheet`
- `saveExpense()` updates bill status on save (SETTLED / PARTIAL), always comparing in home currency

---

## Session Summary (2026-04-11, session 2)

### 1. Source SMS in Edit Expense + "Open in Messages" deep link

- `EditExpenseViewModel` now populates `rawSmsBody` and `expenseSourceType` in the form state from the loaded `Expense`.
- `AddExpenseContent` (shared by Add & Edit) shows the "Source SMS" card when `rawSmsBody != null` **or** `sourceType != MANUAL`. Pre-existing auto-imported expenses with no stored body show "Auto-imported from SMS" instead of the expandable text.
- `RawSmsPreviewCard` enhanced: when expanded, an "Open in Messages ↗" `TextButton` appears. It queries `content://sms/inbox` by body text to find the sender's number, then launches `Intent(ACTION_VIEW, "sms:${address}")`. Falls back to opening the messaging app's main screen if no match found. `READ_SMS` permission was already declared.

### 2. `rawSmsBody` persistence bug fix

`AddExpenseViewModel.saveExpense()` was building the `Expense` object without `rawSmsBody`, so expenses added from the notification inbox were stored with `rawSmsBody = null` in the DB. Fixed by adding `rawSmsBody = state.rawSmsBody` to the `Expense(...)` constructor call. Bulk SMS import (`SmsImportViewModel`) was already correct.

### 3. `expenseSourceType` added to `AddExpenseUiState`

New field `expenseSourceType: SourceType? = null` — populated by `EditExpenseViewModel` from `expense.sourceType`. Allows the SMS card to show "Auto-imported from SMS" for pre-existing expenses that have no stored body text.

### 4. Inline "Add new category" in category picker sheet

Both Add Expense and Edit Expense now have an inline "Add new category" form inside the category bottom sheet, mirroring the existing "Add new account" pattern:

- "**+ Add new category**" `TextButton` appears above the search field.
- Tapping it shows an inline form: category name field + 5-column icon grid (15 icons, same set as Category Management) + Cancel / Save.
- On Save: `categoryRepository.addCategory()` is called; new category is **auto-selected** and the sheet closes.
- Both `AddExpenseViewModel` and `EditExpenseViewModel` have `CategoryRepository` injected and identical 5 methods: `showAddNewCategoryForm()`, `hideAddNewCategoryForm()`, `onNewCategoryNameChange()`, `onNewCategoryIconChange()`, `saveNewCategory()`.
- `AddExpenseUiState` gained 4 fields: `isAddingNewCategory`, `newCategoryName`, `newCategoryIconName`, `isSavingCategory`.
- No DB migration required.

---

## Session Summary (2026-04-11, session 1)

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

**Compose gotcha discovered**: `LazyColumn` inside `AlertDialog`'s `text` slot renders nothing. Fix: use `Column + verticalScroll(rememberScrollState())` with `heightIn(max = Xdp)`.

**`feature/settings/build.gradle.kts`**: Added `implementation(libs.kotlinx.datetime)` — required because `Expense.date` is `kotlinx.datetime.Instant` and the settings module now uses it directly.

---

## Open Issues

| Issue | File | Notes |
|-------|------|-------|
| `DuckDuckGoApiService.kt` dead code | `data/` | Unused since Google Places replaced it — safe to delete |
| ProGuard/R8 rules | missing | Release build unverified |
| "Unknown Bank" account names | Account matching | SMS-inferred accounts have auto-generated names; no cleanup UX |
| `BillStatementParserRegistry` comment stale | `BillStatementParserRegistry.kt` line 6 | Says "Tried only after ParserRegistry returns null" — actually tried first now |

---

## First Action for Next Agent

No critical bugs. Suggested next tasks in priority order:

1. **Delete `DuckDuckGoApiService.kt`** — dead code, safe to remove. Check for any remaining references with `grep -r "DuckDuckGo"`.

2. **ProGuard/R8 rules** — `./gradlew assembleRelease` is unverified; may crash on first run due to missing rules for Ktor/Room/Hilt.

3. **Phase 2 remaining features** — Budgets (F13), CSV/PDF export (F14), home screen widget (F16).

4. **Fix stale code comment** in `BillStatementParserRegistry.kt` line 6 (one-liner).

---

## Gotchas / Surprises

- **`LazyColumn` in AlertDialog**: Always use `Column + verticalScroll` for any scrollable content inside an `AlertDialog`'s text slot.
- **`LazyVerticalGrid` in `ModalBottomSheet`**: Works fine with a fixed `height(Xdp)` constraint — use this pattern for icon pickers inside sheets.
- **`kotlinx-datetime` classpath**: If a `:feature` module uses `Expense.date` (type `Instant`) directly, it needs `implementation(libs.kotlinx.datetime)` — transitivity is not sufficient.
- **Two-group regex**: `groupValues[1]` is `""` (not `null`) when only group 2 matches. Always `.takeIf { it.isNotBlank() }` on both groups in amount patterns.
- **KSP + clean**: Always `./gradlew clean assembleDebug` after adding new files. Never bare `assembleDebug`.
- **`rawSmsBody` on pre-2026-04-11 notification-path expenses**: null in DB (bug was fixed). Edit Expense infers auto-import from `sourceType` and shows "Auto-imported from SMS" label for those.
