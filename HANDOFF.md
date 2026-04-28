# Expense Analyst — Handoff

**Last updated**: 2026-04-29
**DB version**: 15
**Build**: `./gradlew clean assembleDebug` ✅ | Device not connected
**Repo**: `https://github.com/psanup89-rgb/expense-analyst` (public)
**Release**: v0.1.0-budget (GitHub Release with APK)

---

## Session Summary (2026-04-29) — Budget feature (F13)

### Budget section — new `:feature:budget` module

Full budget tracking feature accessible from Settings, protected by biometric/device credential authentication.

**DB MIGRATION_14_15** creates two new tables:
- `salary_entries` — monthly salary with unique index on (month, year), optional link to INCOME expense
- `planned_expenses` — planned expense items per month with soft delete

**Features:**
- Salary tracking: manual entry, auto-detect from INCOME transactions, salary history view
- Planned expenses: add/edit/soft-delete with category, carry-forward from previous month
- Planned vs Actual: category comparison progress bars, unplanned expense flagging, summary card (total planned, actual, savings/overspend)
- Month navigation (← month →)
- Biometric gate: `BiometricPrompt` with `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` authenticators; falls through if no lock screen set

**New files (17):**
- Domain: `SalaryEntry.kt`, `PlannedExpense.kt`, `BudgetRepository.kt`
- Data: `SalaryEntryEntity.kt`, `PlannedExpenseEntity.kt`, `SalaryDao.kt`, `PlannedExpenseDao.kt`, `BudgetRepositoryImpl.kt`
- Feature: `BudgetScreen.kt`, `BudgetViewModel.kt`, `BudgetUiState.kt`, `BiometricHelper.kt`, `BudgetComponents.kt`, `BudgetCards.kt`, `BudgetDialogs.kt`, `build.gradle.kts`, `AndroidManifest.xml`

**Modified files:** `ExpenseAnalystDatabase.kt` (v15), `ExpenseDao.kt` (+getIncomeByDateRange), `DatabaseModule.kt`, `RepositoryModule.kt`, `NavRoutes.kt`, `AppNavGraph.kt`, `SettingsScreen.kt`, `app/build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml` (+biometric)

---

## Session Summary (2026-04-28) — Data security, bulk import fix, repo migration

### 1. Data Security Rules added to CLAUDE.md

Added a "Data Security Rules" section to CLAUDE.md (after Project Overview) codifying rules for handling SMS data, PII, credentials, and prompt injection defence. These rules apply to all agent sessions.

### 2. Bulk SMS import — bill statements no longer enqueued to pending inbox

**Bug**: Colleague reported that after bulk SMS import, items appeared in both the expenses list AND the pending notification inbox.

**Root cause**: `SmsImportViewModel.startBulkImport()` called `billStatementManager.process(statement)` for bill-type SMS, which always creates a `PendingNotification` with `pendingType="BILL"`. This is wrong for bulk import — bills should just be counted, not enqueued.

**Fix**: Removed `billStatementManager.process()` call and cleaned up the unused `BillStatementManager` injection from `SmsImportViewModel`. The `billsFound` counter still works so the import summary reports bill SMS correctly.

File changed: `feature/notification/src/main/java/com/expenseanalyst/feature/notification/ui/SmsImportViewModel.kt`

### 3. Git repo migrated to new GitHub account

- Remote changed from `anoop-p-ksa0043/expense-analyst` → `psanup89-rgb/expense-analyst`
- Repo made public
- GitHub releases created: `v1.0.0-debug`, `v1.0.1-debug` (with APK assets)
- `gh auth setup-git` configured for the new account

---

## Session Summary (2026-04-13) — Claude AI Tier 3, bill fixes, merchant memory

### 1. Airtel bill generation SMS — `AirtelStatementParser`

Previous fingerprint only matched "bill of Rs.X" and "bill payment reminder". Airtel's **bill generation** format ("Bill for your Airtel Wi-Fi 20025035912 has been generated. Amount to be paid: Rs 2119.28") was slipping through to `GenericParser`, which saw the word "paid" and classified it as a DEBIT spend.

Fixes:
- `bodyFingerprint` extended with `bill\s+for\s+your\s+airtel`
- `parse()` now uses a dedicated `totalDuePattern` (`amount to be paid: Rs X`) before falling back to raw Rs.X match
- `dueDatePattern` added to extract `Due Date: dd-MMM-yyyy` when present
- `GenericParser.billReminderPattern` extended with `bill.{0,70}has\s+been\s+generated` as defence-in-depth

### 2. Source SMS in pending bill card — `PendingInboxScreen`, `BillStatementManager`, `TransactionNotificationService`

`PendingBillItem` now shows a collapsible "Source SMS" row (identical expand/collapse UX to the expense inbox card).

Changes:
- `TransactionNotificationService`: passes `statement.copy(rawBody = effectiveBody)` so the raw SMS text travels into the pending record
- `BillStatementManager`: uses `statement.rawBody` (was hardcoded `null`)
- `PendingInboxScreen`: `PendingBillItem` renders an `AnimatedVisibility` SMS block when `item.rawBody` is non-null

### 3. Auto-save MerchantRule on manual category selection

Previously a rule was only saved when: (a) the user explicitly tapped "Teach App" in Expense Detail, or (b) Tier 3 web search returned a result.

Now: whenever the user manually picks a category in the **Add** or **Edit** Expense category sheet and the merchant name is non-blank, a `MerchantRule` is silently upserted. Tier 1 will catch that merchant instantly on all future transactions.

- `AddExpenseViewModel.onCategorySelect()` — launches `merchantRuleRepository.saveRule()`
- `EditExpenseViewModel.onCategorySelect()` — same; `MerchantRuleRepository` injected (new dep)

### 4. Tier 3 category inference: Google Places → Claude AI

Replaced the Google Places API (paid, geographic restriction) with a call to an Anthropic-compatible endpoint using `claude-haiku-4.5`.

| What | Before | After |
|---|---|---|
| Service class | `GooglePlacesApiService` (deleted) | `ClaudeApiService` (new) |
| Auth header | `X-Goog-Api-Key` | `Authorization: Bearer <key>` |
| API | Google Places Text Search | Claude Messages API `/v1/messages` |
| Model | — | `claude-haiku-4.5` |
| Config keys | `GOOGLE_PLACES_API_KEY` | `CLAUDE_API_KEY` + `CLAUDE_API_BASE_URL` |
| Category mapping | `mapPlaceTypesToCategory()` (place type → string) | Claude returns category name directly |
| Hallucination guard | — | Response validated against `VALID_CATEGORIES` set |

`local.properties` now requires:
```
CLAUDE_API_KEY=<key>
CLAUDE_API_BASE_URL=https://your-proxy.example.com
```
Quotes around values are stripped by `localProp()` in `data/build.gradle.kts`. Base URL defaults to `https://api.anthropic.com` if blank. The endpoint at `api.gameron.me` uses `Authorization: Bearer` (not `x-api-key`); this is handled in `ClaudeApiService`.

`InferenceSource.WEB_SEARCH` renamed to `AI_SEARCH`. Settings label updated to "Use Claude AI".

---

## Session Summary (2026-04-12, session 3) — Open issue cleanup

### 1. ProGuard/R8 rules — `app/proguard-rules.pro` created

File was entirely missing (referenced in `build.gradle.kts` but not present). Created with rules for:
- **Kotlin / JVM**: annotations, metadata, enum valueOf/values, companion objects
- **Kotlin Coroutines**: MainDispatcherFactory, CoroutineExceptionHandler, volatile fields
- **kotlinx.datetime**: full keep
- **Hilt / Dagger**: `@HiltViewModel`, `@AndroidEntryPoint`, `@Module`, `@Provides`, `@Binds`, generated component classes
- **Room**: `@Entity`, `@Dao`, `@Database`, `@TypeConverter`; explicit keeps on all `data.local.entity`, `data.local.dao`, and `domain.model` packages
- **Ktor + OkHttp**: full keep + dontwarn for Android engine dependencies
- **DataStore**: keep + dontwarn
- **Miscellaneous**: suppression of bouncycastle, conscrypt, openjsse build-time warnings
- `assembleDebug` ✅ after adding rules

### 2. "Unknown Bank" account names — fixed in `GenericParser`

`GenericParser.parse()` now calls `bankNameFromSender(sender)` instead of the hardcoded `bankName` property. The private method maps recognisable sender substrings to real bank names (same lookup table as `SmsImportViewModel.bankDisplayNameFromSender()`), extended to cover: HDFC, ICICI, Axis, SBI, Kotak, Yes Bank, IndusInd, PNB, Al Rajhi, Alinma, STC, D360, Emirates NBD, IDFC First, OneCard, Canara, Bank of Baroda, Union Bank, Citi, Amex, Paytm, Airtel. Falls back to "Unknown Bank" only if no pattern matches. Applies to both live notification path and bulk SMS import.

### 3. Stale comment — `BillStatementParserRegistry.kt` line 5

Updated comment from "Tried only after [ParserRegistry] returns null" → "Tried FIRST by [TransactionNotificationService], before [ParserRegistry]…" to match actual routing behaviour since 2026-04-12 session 1.

### 4. `DuckDuckGoApiService.kt` — already absent

File not found anywhere in the codebase. Either deleted in a prior commit or never committed. Issue closed with no action.

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

No known bugs. All previous open issues resolved (see session 3 summary above).

---

## First Action for Next Agent

No critical bugs. Suggested next tasks in priority order:

1. **Phase 2 remaining features** — CSV/PDF export (F14), home screen widget (F16).

2. **ProGuard release smoke-test** — `./gradlew assembleRelease` requires a signing config. Set up `keystore.properties` + signing block in `app/build.gradle.kts` and verify release APK starts on device without crashing.

---

## Gotchas / Surprises

- **`LazyColumn` in AlertDialog**: Always use `Column + verticalScroll` for any scrollable content inside an `AlertDialog`'s text slot.
- **`LazyVerticalGrid` in `ModalBottomSheet`**: Works fine with a fixed `height(Xdp)` constraint — use this pattern for icon pickers inside sheets.
- **`kotlinx-datetime` classpath**: If a `:feature` module uses `Expense.date` (type `Instant`) directly, it needs `implementation(libs.kotlinx.datetime)` — transitivity is not sufficient.
- **Two-group regex**: `groupValues[1]` is `""` (not `null`) when only group 2 matches. Always `.takeIf { it.isNotBlank() }` on both groups in amount patterns.
- **KSP + clean**: Always `./gradlew clean assembleDebug` after adding new files. Never bare `assembleDebug`.
- **`rawSmsBody` on pre-2026-04-11 notification-path expenses**: null in DB (bug was fixed). Edit Expense infers auto-import from `sourceType` and shows "Auto-imported from SMS" label for those.
- **Claude API proxy (api.gameron.me)**: Uses `Authorization: Bearer <key>` header (not `x-api-key`). Model name is `claude-haiku-4.5` (not `claude-haiku-3-5`). Test with `curl` before assuming the model string is correct on a new proxy.
- **`local.properties` quoted values**: The `localProp()` helper in `data/build.gradle.kts` strips surrounding double-quotes, so `CLAUDE_API_KEY="sk-..."` and `CLAUDE_API_KEY=sk-...` both work.
