# Expense Analyst — Handoff

**Last updated**: 2026-09-21
**DB version**: 26
**Build**: `./gradlew clean assembleDebug` ✅
**Repo**: `https://github.com/psanup89-rgb/expense-analyst` (public)
**Release**: v0.7.10-debug (GitHub Release with APK)

---

## Session Summary (2026-09-21) — Bills were detected but unreachable

Two Notion issues ("Bills are not getting detected", "Transaction not detected") plus an
investigation that found the real bill problem was not detection at all.

### The bill bug worth remembering

34 statements had been detected since June and sat in `pending_notifications`. The `bills` table
had 0 rows. Cause: `BillsScreen` computes `isEmpty` from *saved* Bills and early-returns to a
"No bills yet" state, and the "Pending Bill Statements" card — the only route into `PENDING_INBOX`
anywhere in the app — lived below that return. No saved bill → no link → cannot confirm a
statement → `bills` stays empty → link never appears. Compounding it, bills were detected silently:
transactions get a banner and a tray notification, bills got neither, and `getCount()` existed but
nothing consumed it.

Fixed by rendering the card in the empty state with a count badge, and posting a tray notification
on detection (`postForBill` → `ACTION_OPEN_PENDING_INBOX`). Verified on device: badge showed 34 and
the inbox opened.

**Lesson for future UI work**: when a screen's only navigation to a sub-screen lives inside a list,
check what the empty state does to it.

### Also fixed

- Re-sent reminders deduped on biller + amount within 35 days (6 real Saudi Energy bills had
  become 17 rows). The existing `findRecentByRawBody` only catches byte-identical bodies.
- `BankNameFromSender` now the single source of truth for sender → bank name, collapsing three
  drifting copies that had split Al Rajhi across two biller names.
- `AlRajhiStatementParser` due-date regex required "date"/"by" after "due"; it was matching
  "Total amount due:" and parsing the amount as a date.
- New `OneCardStatementParser`; `AlRajhiParser` now handles "Debit/Credit Internal Transfer".

### Deliberately not done

- The existing 34 queued rows were left alone — they still carry the old biller spellings and the
  reminder duplicates. The dedup and naming fixes apply to newly arriving bills only. The owner
  chose to triage the backlog in-app now that the inbox is reachable.
- Saudi Energy's missing due dates are not a bug: those SMS carry no due date.

---

## Session Summary (2026-09-20) — Account-attribution bug fixes + SMS sender display

Owner spotted a phantom "Al Rajhi Bank · Forex Card" account with no real card behind it, and gave
the correct bank/type for several last-4-digit accounts that had been mis-labeled as Al Rajhi.
Investigation turned into a broader audit that found three distinct, previously-unnoticed bugs.

### 1. `AlRajhiParser` was stealing other banks' messages

Root cause, found while investigating why 22 genuine Emirates NBD \*4388 transactions were on an
"Al Rajhi Bank \*4388" account: `AlRajhiParser.canParse()` had a generic, bank-agnostic body
fingerprint (`purchase...SAR...balance/amount`) as one of its OR-alternatives. Since it's
registered early in `ParserRegistry` and this fingerprint matches practically any Saudi/Gulf bank's
card-purchase SMS shape, it claimed messages from Emirates NBD and D360 Bank whenever their sender
didn't literally contain "alrajhi" — before those banks' own (correct) parsers ever got a turn.
Fixed by removing the generic fingerprint, keeping only Al Rajhi-specific body shapes.

**General lesson for future parsers, recorded in CLAUDE.md**: a bank parser's `canParse()` body-only
fallback must be specific enough to that bank's actual wording — never a shape any bank's SMS could
produce.

### 2. OTP messages parsed as duplicate transactions

Found while auditing a "Bank D·360" naming-duplicate account: one row was literally an OTP text
("OTP: 7951. Amount: SAR 649.00...") that restated a real purchase's amount for context, and got
parsed as a second, separate expense. `ParserRegistry.parse()` now skips any message matching
`\bOTP\b`/"one time password"/"verification code" before trying any parser. The duplicate expense
itself was deleted from the owner's device (confirmed with them first).

### 3. Refund from a keyword merchant miscategorized

The owner separately noticed 3 Keeta refunds were categorized "Food & Drinks" instead of "Refund".
Root cause: `KeetaParser` hardcodes `merchant = "Keeta"`, and `"keeta"` is itself a Food & Drinks
keyword in `CategoryInference`'s keyword list — so merchant-keyword matching (which ran before the
SMS-body refund-wording check) always won. This silently broke the refund-nets-out-of-spend logic
(only `INCOME` + category=="Refund" rows get netted in `ExpenseListViewModel`) for any refund from
a merchant whose name doubles as a spending keyword — not just Keeta. Fixed by checking the SMS
body for refund/reversal/cashback wording *before* merchant-keyword matching in `CategoryInference.infer()`.

### 4. Unknown-bank account fragmentation

Separately fixed this session (owner noticed two "Unknown Bank" accounts after the first cleanup
pass): `AccountRepositoryImpl.findOrCreate` now always drops the last-4 digits for `bankName ==
"Unknown Bank"`, so every unidentifiable SMS collapses onto one single account rather than
fragmenting by whatever last-4 a given SMS happened to parse. New `MIGRATION_25_26` (DB v25→v26)
does a one-time consolidation of any pre-existing duplicates on upgrade.

### 5. SMS sender shown in Edit and View

`RawSmsPreviewCard` (Edit screen) now shows "From: \<sender\>" using `Expense.sourceSender` — only
populated for `SMS_AUTO` expenses; `NOTIFICATION_AUTO` ones have no sender by design (Android's
`NotificationListenerService` only exposes the notifying app's identity, not a telecom sender ID).
The "Original SMS" section on Expense Detail (**View**) was previously gated to `SMS_AUTO` only;
widened to any non-manual expense, since `NOTIFICATION_AUTO` is the majority of recent auto-saves
and never showed its source text there at all before this fix.

### Manual data correction on the owner's device (one-time, not a code fix)

Applied directly via a pulled/corrected/pushed database (full integrity checks before and after
each write): Al Rajhi \*2819/\*7573/\*8422 corrected from Savings to Debit/Credit Card as
appropriate; \*4087/\*7421/\*9855 moved to Riyad Bank/SAB/D360 Bank respectively (per owner's own
knowledge of their cards — no code signal could have derived this for `NOTIFICATION_AUTO`
captures, which carry no sender); the 22 Emirates NBD transactions and the "Bank D·360" naming
duplicate merged into their correct existing accounts; an empty ghost account (\*9731, zero
transactions) removed; the OTP-duplicate expense deleted.

---

## Session Summary (2026-09-20) — Refund auto-matching (DB v25)

Owner asked how the Refund category is detected today, then asked for account/payment method to
be pulled from the original expense when a refund matches one from the last 3 months.

Confirmed via `AskUserQuestion` before building, since this touches netting/categorization logic
with real ambiguity:
- Match on **amount + currency only** (not merchant — refund SMS rarely carry a clean merchant name).
- **Most-recent match wins** on ties, same precedent as the existing BNPL same-day match.
- Amount-matching runs **only after** the existing keyword-based Refund detection already fired —
  it does not expand what counts as a refund, only refines account/payment method + links it, once
  one is already identified as a refund. (This was explicitly scoped down from the owner's literal
  ask, which suggested amount-matching could also be a *detection* signal — flagged as a possible
  future follow-up, not built.)
- Matched original is **marked** (`refundOriginalExpenseId`) so it can't be claimed by a second
  refund later.

New `domain/util/RefundMatcher.kt`: given a refund amount/currency/date and the full expense
snapshot, filters to `EXPENSE`-type, non-deleted, same amount+currency (epsilon-tolerant), within
a 90-day window, excluding any expense already claimed via another expense's
`refundOriginalExpenseId`, and returns the most recent match. Wired into `PendingNotificationManager`
(live capture) and `SmsImportViewModel` (bulk import — needed its own in-batch claim tracking,
since a refund and its original purchase can both land in the same import run before either is
persisted, so the DB-only exclusion check wouldn't catch a same-batch double-claim).

**DB v25**: `MIGRATION_24_25` adds one nullable column, `expenses.refund_original_expense_id`.

Also investigated and documented (no code change) how account/payment method are resolved for
*every* other auto-captured transaction today: `PaymentMethodDetector.detect()` scans only the
current SMS's own text (falls back to `OTHER`), and account type is guessed from the current SMS
body too (defaults to `SAVINGS`), with `AccountRepository.findOrCreate` only reusing an existing
account on an exact bankName+lastFour match — refund SMS often omit the last 4 digits, which is
why refunds were landing in the wrong account before this session's fix.

---

## Session Summary (2026-09-19) — Delete option in Edit Expense

Owner request: delete was only reachable from Expense Detail; wanted it in Edit Expense too.

`AddExpenseContent` (the composable shared by both Add and Edit Expense) gained an opt-in
`showDeleteOption` param, off by default so Add Expense is unaffected. When on (Edit Expense only),
a delete icon appears in the top bar, opening the same confirm dialog used on Expense Detail.
`EditExpenseViewModel` gained `showDeleteConfirm()` / `dismissDeleteConfirm()` / `deleteExpense()`
(soft-delete via the existing `SoftDeleteExpenseUseCase`). New `EditExpenseScreen.onDeleted`
callback, wired in `AppNavGraph` to `navController.popBackStack(NavRoutes.EXPENSE_LIST, false)` —
deliberately popping all the way to the list rather than one step back, since a single pop would
land on the now-stale Expense Detail screen for an expense that no longer exists.

---

## Session Summary (2026-09-19) — Rule-tag bug fixes + account search

Bug reports from testing the v0.7.5 release (tags-on-rules feature), plus a small usability gap
on Manage Accounts. No DB change this session.

### 1. Merchant rule dialog: tag search hidden behind keyboard

The "Also apply these tags" section inside `RuleDialog` (`ExpenseDetailScreen.kt`) had no scroll
behavior — when the keyboard opened while typing a tag search, the dialog window shrank but the
suggestion/"+ Create" chips below the search field were clipped off-screen with no way to reach
them. Fixed by making the dialog's content `Column` scrollable and IME-aware:
`heightIn(max = 420.dp)` + `verticalScroll(rememberScrollState())` + `imePadding()`.

### 2. Merchant rule dialog: tags didn't persist across reopens

Real bug, and the likely actual cause behind "tags aren't saving" — not just a symptom of #1.
`ExpenseDetailViewModel.showRuleDialog()` initialized the tag picker from `it.existingRule?.tags`,
where `it` is `_ui`'s own raw state. But `_ui` never actually holds a computed `existingRule` —
that field only exists on the *derived* `uiState` (built in the `combine` chain via
`MerchantRuleMatcher.findMatch`). So `_ui.existingRule` was always its default `null`, and every
dialog open silently reset the tag picker to empty, regardless of what had actually saved
correctly to the DB. Fixed to read `uiState.value.existingRule` instead. Also added a "+ tags: ..."
line to the "Auto-category rule" info row on Expense Detail, so applied tags are visible without
reopening the dialog.

### 3. Manage Accounts: added search

The account list (`AccountManagementScreen.kt`) had no way to filter once it grew past a screen or
two. New search field above the list, filtering by bank name, display name, last-4-digits, or
account type (`AccountManagementViewModel.onSearchQueryChange`, new `searchQuery` field on
`AccountManagementUiState`).

### 4. Manage Accounts: delete-with-remap — already shipped

Checked, and the "map expenses to another account before deleting" flow the owner asked about was
already fully implemented (`DeleteRemapDialog`, `AccountManagementViewModel.confirmDelete` →
`ExpenseRepository.remapAccount`). No change made; likely just hadn't been exercised yet on a
device that had it installed.

---

## Session Summary (2026-09-19) — Amount/range search + tags on merchant rules (DB v24)

Two open items from the "Expense App Issues" Notion board (both confirmed done at the start of this
session had already shipped: reimbursement tracking and BNPL/split-payment exclusion, marked Done
in Notion). The two genuinely open items were planned and implemented this session.

### 1. Search by amount or range

The expense list search box (`ExpenseListViewModel`) now auto-detects whether the typed query is
an amount lookup rather than free text: a bare number (`4386`) is an exact match, `"4000 to 4500"`
is an inclusive range, anything else falls back to the existing description/merchant/tag substring
search. New `domain/util/AmountSearchParser.kt`. Per the owner's explicit choice, matches the
expense's **original-currency amount** (`expense.amount`), not `homeAmount` — a foreign-currency
expense is found by the figure it was actually charged, not its converted value. Only the `"X to Y"`
range wording is supported (no hyphen-range syntax).

### 2. Tags on merchant rules ("Teach App")

A merchant rule can now carry tags alongside its category (`MerchantRule.tags`, new
`merchant_rule_tags` join table, DB v24 `MIGRATION_23_24` — mirrors the existing `expense_tags`
pattern). Per the owner's explicit answer to "should this only apply via the manual dialog, or also
on auto-capture?" — **both**:

- Manual: `RuleDialog` in `ExpenseDetailScreen` now embeds the same `TagSelector` used on Add/Edit
  Expense (widened from `private` to `internal` in `AddExpenseScreen.kt` to allow reuse).
- Auto-capture: whenever a rule matches an incoming bank SMS, its tags are applied to the
  auto-saved expense — both on live capture (`PendingNotificationManager.enqueue()`) and bulk SMS
  import (`SmsImportViewModel`).

New `domain/util/MerchantRuleMatcher.findMatch()` consolidates the "does this merchant match this
rule's pattern" predicate, previously duplicated independently in three places
(`CategoryInference` Step 1, `ExpenseDetailViewModel`'s `existingRule` lookup, and would have been
a fourth/fifth duplicate in the two auto-capture sites had it not been extracted first).

**Side-effect fix, needed for the auto-capture tags to actually land**: bulk SMS import
(`ExpenseRepositoryImpl.addExpenses`) previously called `ExpenseDao.insertAll` for a `Unit` return
and never persisted any `Expense.tags` on the inserted rows — any tags from a matched rule would
have been silently dropped. `insertAll` now returns the generated ids so tags can be written via
`TagDao.setTagsForExpense` for bulk inserts too, same as the single-`addExpense` path already did.

New tests: `AmountSearchParserTest` (11), `MerchantRuleMatcherTest` (5).

---

## Session Summary (2026-09-13) — Reimbursement tracking + BNPL/split-payment exclusion (DB v22)

Two open issues from the "Expense App Issues" Notion board, both explicitly framed as "suggest an
approach" rather than a spec — see `/Users/anup/.claude/plans/when-a-new-expense-luminous-walrus.md`
for the full design writeup and the owner's exact decisions on each open question.

### 1. Reimbursement tracking

`Expense.isReimbursable: Boolean` + `reimbursedDate: Instant?` — considered and rejected both a
category (the "Refund" precedent: `category.name == "Refund"` string-matched independently in two
ViewModels, no unique index on `categories.name`, netting applied only to headline totals not
category/daily breakdowns) and a `TransactionType` (wrong primitive — describes direction, not
settlement status). Mirrors the `needsReview` flag, the one place this app already does a plain
status-flag-on-`Expense` correctly.

**Owner's decision, don't re-litigate:** does NOT net out of totals — the reimbursement money
arrives as its own separately-detected `INCOME` transaction, so totals are already correct once
that lands. Manual toggle only, no auto-matching of incoming credit to a flagged expense.

New Settings → Reimbursements screen, targeted `ExpenseDao.updateReimbursedDate` (guarded on
`is_reimbursable = 1`), toggle in Add/Edit Expense, status row in Expense Detail.

### 2. BNPL / split payments (Tabby, Tamara)

The reported failure: buying via Tabby/Tamara generates two SMS — one from the merchant for the
full amount (already recorded as a real expense, shouldn't be) and one from Tabby/Tamara confirming
the split (should be the one that's recorded). New `TabbyTamaraParser` detects the second SMS and,
in `PendingNotificationManager.handleBnplConfirmation()`, either reclassifies the already-captured
merchant expense (targeted `ExpenseDao.reclassifyAsSplitPayment`, category → "Split Payments",
type → `PAYMENT`) or creates a new expense directly if no match is found — matched by amount +
merchant + same calendar day, reusing the existing `isSameCalendarDay` dedup helper.

**This path bypasses the normal dedup entirely** (`ParsedTransaction.isBnplConfirmation = true`
short-circuits `enqueue()` before the standard amount+merchant+day check) — that check would
otherwise treat the confirmation as a duplicate of the merchant's own SMS and silently discard it,
defeating the whole point.

**`TransactionType.PAYMENT` was already excluded from every spend total** (confirmed by exploring
`ExpenseListViewModel`/`AnalyticsViewModel` — neither has ever summed it), so no new netting logic
was needed. What was missing: Analytics never showed `PAYMENT` rows *at all*, in any chart. Added a
synthetic "Split Payments" bucket to the category breakdown, computed separately from the
`EXPENSE`-only sum that drives `totalExpense` — with `CategorySpend.isExcludedFromTotal` so the UI
renders "Not counted in total spent" instead of a percentage that would otherwise be meaningless
(the amount isn't part of the denominator). Drill-down scoped narrowly to only the Split Payments
category name, so no other category's drill-down semantics changed.

**The owner's explicit fallback, already built, zero code needed**: `ExpenseDetailScreen` →
"Convert to EMI" → `CreateEmiFromExpenseUseCase`, already reachable from any expense. When the
BNPL SMS isn't captured, this is the manual path — and it's a *deliberately different* accounting
treatment (each installment counts in its own month) from true BNPL (excluded forever). Two tools
for two situations, not an inconsistency.

### ⚠️ `TabbyTamaraParser` needs a real sample before it can be trusted

No real Tabby/Tamara purchase-confirmation SMS was available. Extrapolated from
`TamaraStatementParser`'s one confirmed real sample — which is a payment-*due* reminder, a
different message shape entirely — plus typical public BNPL wording. `TabbyTamaraParserTest`
proves the parser matches its own assumed format, not that the format is real. First thing to
check once an actual message is available; expect the regexes to need adjustment.

### Bonus fix: a pre-existing doc gap, unrelated to this session's features

`docs/NOTIFICATION_PARSING.md` and `CLAUDE.md` both listed 18 transaction parsers and never
included `KeetaParser`, which shipped in an earlier session. True count before this session's
addition was already 19. Fixed both while adding `TabbyTamaraParser` as the 20th.

### Verification

Full clean build green. `TabbyTamaraParserTest` (9 tests) passes; all pre-existing notification
tests (283 total) still pass — confirms `KeetaParser`'s registration slot and `GenericParser`'s
fallback position weren't disturbed by inserting the new parser ahead of it. Migration verified by
executing the SQL directly (same method as the Fuel/Leisure session) rather than relying on `:data`
tests, which still don't run (`useJUnitPlatform()` still missing there — a standing gap, not
addressed this session either).

**`CreateEmiFromExpenseUseCaseTest` still fails** — same pre-existing interest-calculation
mismatch flagged two sessions ago (expects `1064.65`, gets `1066.19`). Confirmed unrelated by
running domain tests before touching anything this session. Still unresolved; still needs an
owner decision on which side (formula or test) is wrong.

---

## Session Summary (2026-09-12) — Fuel and Leisure categories (DB v21)

The owner had hand-created two categories, Fuel and Leisure. Since there is no colour picker
anywhere in the app, every user-created category is hardcoded grey `#9E9E9E` with icon
`more_horiz` — so both looked generic in-app and showed a plain letter badge on notifications.

### What shipped
- **`MIGRATION_20_21`** — UPDATE-then-conditional-INSERT, *not* the `INSERT OR IGNORE` pattern.
- **`CategoryInference` restructure** — `Fuel` inserted before `Transport`, `Leisure` before
  `Entertainment`, with keywords moved across and Gulf brands added.
- **New `fallbacks` map** in `CategoryInference` — fixes a real regression (see below).
- **Two notification glyphs** + `CategoryNotificationIcon` entries.
- **`currency_exchange` picker drift fixed** — the seeded Refund category's icon was missing from
  `availableCategoryIcons`, so it could not be re-selected and was unrecoverable once the edit
  dialog touched the icon grid.
- 16 new tests. Version 0.7.1/3 → **0.7.2/4**.

### Three findings worth not rediscovering

1. **`categories.name` has no unique index** — the table declares no indices at all (schema 20:
   `"indices": []`). `INSERT OR IGNORE`, the pattern `MIGRATION_16_17` used to add Refund,
   therefore has no conflict target and *cannot* dedupe. It would have silently created a second
   "Fuel" beside the owner's existing row. `MIGRATION_16_17` still carries this latent bug: anyone
   who hand-made a "Refund" category before v17 has two. `DATA_MODELS.md` had documented this
   column as UNIQUE, which is almost certainly how the wrong pattern got established — corrected.
2. **`CategoryInference.infer()` returned `null` on a matched-but-unresolvable category**, and the
   callers fall back to Misc. Splitting Fuel out of Transport therefore introduced a regression:
   delete the Fuel category and every petrol SMS would land in Misc instead of Transport. The
   `fallbacks` map fixes it and incidentally hardens every other rule against a deleted category.
3. **`is_default` is a hard delete gate**, not a marker — `CategoryManagementViewModel:125` returns
   early and the button is disabled at `CategoryManagementScreen:238`. The migration never sets it
   on the UPDATE path, or the owner would permanently lose the ability to delete two categories
   they created themselves. Fresh installs get `1` via the INSERT path; that asymmetry is deliberate.

### Verification
`CategoryInferenceTest` (14) and `CategoryNotificationIconTest` (2) pass; all 22 parser test classes
still green. Because `:data` has no `useJUnitPlatform()` and cannot run tests, the migration SQL was
instead **executed directly against synthetic v20 databases** across five scenarios — both rows
already present, neither present, a renamed "Fuel & Petrol", a user-chosen icon, and the migration
run twice. All five produced the intended result with no duplicates.

### Not verified on device
No device was connected, so **the two glyphs have never been seen rendered**. AAPT2 validated the
path syntax, which rules out crashes, but not whether a fuel pump reads as a fuel pump at 34dp.
Install over the existing build (never uninstall, or the migration won't run) and fire a test SMS
per the recipe in the 2026-08-19 section below, using `At:ADNOC` and `At:VOX Cinemas`.

### Flagged, not done
`ClaudeApiService.kt:83-84` caps tier-3 AI categorisation to a hardcoded nine-name set excluding
Fuel, Leisure, Rent, Salary, Transfer, Misc and Refund — so AI inference can never return the new
categories. Needs both `VALID_CATEGORIES` and the prompt text updating.

---

## Session Summary (2026-08-22) — Colored category icon on the transaction notification

User feedback on the "Add note" notification (shipped 2026-08-19): compared to a Google Wallet
tap-to-pay confirmation on the same phone, the notification looked plain — text only, no icon.
Asked for a colored icon on the right, "like a different expense app."

### What shipped
- `TransactionAlertNotification.categoryLargeIcon()`: rasterizes a circular badge via `Canvas` —
  the resolved `Category.colorHex` as fill, plus either its glyph or an initial-letter fallback —
  and sets it via `setLargeIcon()`. Threaded through every posting path (initial notification,
  blank-reply retry, saved-note confirmation, failed-note confirmation) via new `EXTRA_CATEGORY_*`
  intent extras, so the icon never disappears mid-flow.
- **New `CategoryNotificationIcon.kt`**: maps `Category.iconName` → a notification-module drawable
  resource ID, covering the 14 built-in seeded categories. Explicitly documented as a subset of
  `core/util/CategoryIconMapper.kt` (which maps the same ~100-name vocabulary to Compose
  `ImageVector`s) rather than a silent duplicate — `ImageVector` can't be used outside a
  Composition, so this is a second, smaller table by necessity, not an accident.
- **14 new vector drawables**, hand-authored from primitive shapes (rects, circles, triangles,
  simple arcs) rather than reproductions of the in-app Material icons — chosen deliberately to
  minimize the risk of malformed path data written blind, with no device available to preview
  against until late in the session. `ic_cat_refund.xml` intentionally reuses the transfer-arrows
  glyph rather than risk a more elaborate curved variant; the two categories are differentiated by
  color and label, not icon shape.
- **Bonus fix, same root cause**: `CategoryIconMapper` had no branch for `"currency_exchange"` —
  the seeded Refund category's icon was silently falling back to the generic "more" icon
  everywhere in the app (category picker, expense list), not just notifications. Added
  `Icons.Filled.CurrencyExchange`.
- Version 0.7.0/2 → **0.7.1/3**.

### Not yet verified on device
Same caveat as the 2026-08-19 session, compounded: the phone dropped off wireless ADB
(`device not found`) partway through, so **none of the 14 icons have been seen rendered**. The
build compiled clean and AAPT2 validated every drawable's path syntax, which rules out crashes,
but geometric correctness (does the fork actually look like a fork at 34dp) is unverified. Next
agent/session: reconnect the device, fire a test transaction per category to eyeball all 14, and
fix any that look wrong — use the `SmsTestReceiver` adb broadcast recipe in the 2026-08-19 section
below (vary the sender/merchant to land in different categories via `CategoryInference`).

---

## Session Summary (2026-08-19) — "Add note" inline reply on the transaction notification

Auto-saved expenses always got `description = ""` and there was no way to record *what the spend was
for* without opening the app and editing. The notification now carries an inline `RemoteInput` reply
that writes the description straight from the shade.

### What shipped
- **`TransactionAlertNotification`**: `postForExpense` now attaches an "Add note" action. Added
  `postNoteSaved` / `postNoteFailed` / `repostForNoteRetry` / `cancel`, plus private
  `contentIntentFor` (shared by all posting paths so the `ACTION_OPEN_EXPENSE_DETAIL` route can't
  drift) and `buildNoteAction`. **Legacy `post()` and `postNotification()` are byte-identical** —
  verified by diff, so the stale-tray path is provably untouched.
- **`NoteReplyReceiver`** (new): `@AndroidEntryPoint` BroadcastReceiver, `goAsync()` for the write,
  `exported="false"`, no intent-filter (reached only via an explicit PendingIntent).
- **`NoteReplySanitizer`** (new): pure Kotlin — whitespace collapse, trim, 200-char cap that won't
  split a surrogate pair. Isolated precisely because it's the only genuinely unit-testable piece.
- **`ExpenseDao.updateDescription`** + `ExpenseRepository.updateDescription` + impl.
- No DB migration — `description` already existed in v20.
- 11 new tests (`NoteReplySanitizerTest` 7, `ReplyRequestCodeTest` 4), all passing.
- Version bumped to **0.7.0 / versionCode 2**. It had been stuck at `0.1.0` / `1` through every
  release since v0.1.5.

### Three decisions worth not re-litigating
1. **Targeted UPDATE, not `updateExpense`.** A full-row round-trip would null `account_number`
   (`ExpenseMapper.toEntity` hardcodes it), rewrite the tag join table, and race an open Edit Expense
   screen. `getExpenseByIdWithCategory` also has no `is_deleted` filter, so the guard belongs in SQL.
   The rows-affected return doubles as the receiver's success signal.
2. **`needsReview` is not cleared by a note.** `NeedsReviewEvaluator.evaluate()` doesn't take
   `description` as an input, so clearing it would desync the persisted flag from what the evaluator
   would compute, and would wrongly decrement the Review badge.
3. **Replace, not append.** The confirmation notification carries no reply action and self-dismisses,
   so there's no second-reply affordance. Re-editing happens in-app.

### Not yet verified on device
The build and unit tests are green, but **no on-device testing has run** — no device was attached
during the session. The two tests that would actually expose a defect:
- **Cold start**: post a notification, `adb shell am force-stop com.expenseanalyst`, then reply from
  the shade. Proves Hilt cold-start field injection and `goAsync()` process survival.
- **Multiple notifications**: fire three distinct test SMS, reply to the *middle* one, confirm the
  note lands on the right expense. Proves `replyRequestCode` isolation.

Drive both through the existing debug receiver:
```bash
adb shell am broadcast -a com.expenseanalyst.TEST_SMS --es sender "ALRAJHI" --es body "Your account has been debited SAR 150.00 At:Noon Ref:12345 Bal:SAR 5000.00"
```

### Pre-existing failure noticed (not caused here)
`:domain:test` → `CreateEmiFromExpenseUseCaseTest > invoke calculates correct installment with
interest` fails: expected `1064.65`, actual `1066.1854641401003`. Confirmed by stashing all changes
and re-running against pristine `HEAD`. The EMI interest formula and the test's expected value
disagree; **someone needs to decide which is right.** Untouched this session.

---

## Session Summary (2026-08-01) — Needs Review reasons (DB v20)

Needs Review cards now show *which* field(s) caused the flag, e.g. "Missing: Merchant, Account", instead of just being an unexplained flagged item.

- **Root cause investigated first**: the boolean `needsReview` flag was computed once at capture time (`PendingNotificationManager.kt`) from 4 conditions (blank merchant / generic category / unresolved payment method / no account last-4), but only the boolean was persisted — the reasons were discarded, so the list couldn't say why.
- **Correctness finding**: 3 of the 4 reasons (category, payment method, account) can be recomputed later from persisted fields, but the merchant reason cannot — `PendingNotificationManager` always backfills a blank merchant with the bank name before saving, so `expense.merchantName` is never blank in practice. Recomputing live would have silently hidden the most common trigger. Persisting the actual reasons at capture time was the only accurate option.
- **DB v20**: `needs_review_reasons` TEXT column on `expenses` (comma-separated `ReviewReason` enum names), `MIGRATION_19_20`.
- **New**: `domain/util/NeedsReviewEvaluator.kt` — `ReviewReason` enum + `evaluate()`/`encode()`/`decode()`, single source of truth used by both `PendingNotificationManager` (write) and `NeedsReviewScreen` (display, via `Expense.reviewReasons`, which is only ever decoded from what was persisted, never re-evaluated).
- Added `Expense.accountLastFour` (from the already-joined `AccountEntity` relation) and `Expense.reviewReasons: List<ReviewReason>`.
- New test: `domain/src/test/.../util/NeedsReviewEvaluatorTest.kt`.

---

## Session Summary (2026-06-14) — Docs sync (DB v19) + Gradle/AGP/Kotlin/Room bump + release v0.6.1

### 1. Docs brought up to date with DB v19 (auto-save + Needs Review, commit `8be2fa3`)

`STATUS.md`, `HANDOFF.md`, `PROJECT.md`, `CONSTRAINTS.md`, `AGENTS.md`, `CLAUDE.md`, `docs/FEATURES.md`, `docs/DATA_MODELS.md` were all still describing DB v18 and the old tap-to-save Pending Inbox flow. Updated to reflect:
- DB v19 (`needs_review` column on `expenses`), next migration is `MIGRATION_19_20`
- Bottom nav is now **Home · Review · Bills · EMI · Settings** (5 destinations, not 3 — `CLAUDE.md` was wrong)
- Old Pending Inbox is repurposed as "Pending Bill Statements" (BILL type only, reached via Bills screen)
- `lent_items` table (DB v18, previously undocumented) added to `docs/DATA_MODELS.md`

### 2. Build tooling bump (no app-facing change)

`gradle/libs.versions.toml`: AGP 9.1.0 → 9.2.1, Kotlin 2.1.0 → 2.2.10, KSP 2.1.0-1.0.29 → 2.3.2, Room 2.7.0 → 2.7.2. `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.3.1 → 9.4.1. New `gradle/gradle-daemon-jvm.properties` (Gradle's daemon toolchain pin, auto-generated).

### 3. Release v0.6.1 published

Debug APK (`app-debug.apk`) built via `./gradlew clean assembleDebug` and attached to GitHub Release `v0.6.1-debug`, following the established convention (no release-signing keystore exists yet — see Open Question in `NEXT_STEPS.md`).

---

## Session Summary (2026-06-13) — Issue #15 fix + build environment setup

### 1. SaudiEnergyStatementParser — unpaid-reminder SMS misclassified as expense (#15)

SMS: `"We would like to remind you that your issued bill for account No. 30166041401 in the amount of 82.92 SAR has not been paid."`

| Bug | Fix |
|-----|-----|
| Fingerprint `your\s+bill\s+for\s+account` didn't match "your **issued** bill for account" | Made `issued` optional: `your\s+(?:issued\s+)?bill\s+for\s+account\b` |
| Account pattern `account\s+(\d+)` skipped "No." prefix → account number not extracted | Added optional `(?:[Nn]o\.?\s+)?` before digits |
| `GenericParser.billReminderPattern` had no guard for "has not been paid" phrasing | Added `(?:issued\s+)?bill.{0,80}has\s+not\s+been\s+paid` as defense-in-depth |

New `SaudiEnergyStatementParserTest` (7 tests). New test in `GenericParserTest` verifying null result for the exact issue #15 SMS.

**Files changed**: `SaudiEnergyStatementParser.kt`, `GenericParser.kt`, `SaudiEnergyStatementParserTest.kt` (new), `GenericParserTest.kt`

### 2. GitHub issues closed

Issues #10 (Loans), #14 (GenericParser CC auth), #15 (SaudiEnergy) closed with explanatory comments. **No open issues remain.**

### 3. Build environment (first-time Mac setup)

- JDK 21 required (project targets `JavaVersion.VERSION_21`). Installed via `brew install --cask temurin@21`
- `local.properties` created: `sdk.dir=/Users/anoop-ksa0043/Library/Android/sdk`
- Build env: `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home`, `ANDROID_HOME=/Users/anoop-ksa0043/Library/Android/sdk`
- APK installed on device `192.168.100.113` (wireless ADB — owner's device)

---

## Session Summary (2026-06-06) — Loan/Lent tracking (#10) + GenericParser fix (#14)

### 1. GenericParser — CC auth SMS not detected (#14)

Three bugs fixed: `authorized` added to weak-debit keywords; `*` added to merchant char class with `\s+on\s+\d` stop; currency detection now scans amount-match text first.

**Files changed**: `GenericParser.kt`, `GenericParserTest.kt`

### 2. Loans/Lent tracking feature (#10) — new `:feature:loans` module

DB v18: `lent_items` table (16 columns). Domain: `LentItem.kt`, `LentRepository.kt`. Data: entity + DAO + mapper + impl. WorkManager reminders via `LentReminderWorker` + `LentReminderScheduler`. UI: `LoanListScreen`, `AddLoanScreen`, `LoanDetailScreen`. Settlement creates INCOME+Refund expense to net out of monthly totals. Entry point: Settings → "Loans & Lending".

---

## Session Summary (2026-05-11) — Bug sweep: Issues #4–#9, #11, #12

- **AxisBankStatementParser** (#4, #7): fingerprint + due-date pattern extended for newer CC reminder format
- **KeetaParser** (#5, #8): refund/cancellation regex + canParse `[Keeta]` prefix handling
- **EmiratesNbdParser** (#6, #9): fingerprint widened for generic short-code SMS + POS Reversal
- **PAYMENT routing** (#6): `PendingNotificationManager` sets merchant = "BillPayments" for PAYMENT with blank merchant; auto-links to open bill via `BillMatcher`
- **BillMatcher** (#11): strict amount-tolerance matching (±5% or minimumDue); replaces loose substring match
- **Refunds reduce monthly Spent** (#12): `ExpenseListViewModel` + `AnalyticsViewModel` subtract INCOME+Refund from gross totals

---

## Open Issues

**None.** All GitHub issues resolved.

---

## First Action for Next Agent

Priority order:

0. **Verify the "Add note" reply on a real device** (see 2026-08-19 session summary) — cold start and
   multi-notification cases especially. And decide whether the EMI interest formula or its test
   expectation is the wrong one.
1. **CSV/PDF export (F14)** — most-requested Phase 2 feature. Export filtered expense list as CSV; PDF is a stretch goal.
2. **Home screen widget (F16)** — monthly spend summary widget using Glance API.
3. **ProGuard release smoke-test** — `./gradlew assembleRelease` needs a signing config (`keystore.properties` + signing block in `app/build.gradle.kts`). `proguard-rules.pro` already exists.

---

## Gotchas / Surprises

- **JDK 21 required** — project targets `JavaVersion.VERSION_21`. JDK 17 fails with `invalid source release: 21`.
- **Always `./gradlew clean assembleDebug`** — never bare `assembleDebug`. KSP incremental is disabled.
- **`LazyColumn` in AlertDialog**: use `Column + verticalScroll` instead.
- **`kotlinx-datetime` classpath**: `:feature` modules using `Expense.date` (type `Instant`) need `implementation(libs.kotlinx.datetime)` explicitly.
- **Two-group regex**: `groupValues[1]` is `""` not `null` when only group 2 matches. Always `.takeIf { it.isNotBlank() }`.
- **Dedup sourceType coverage**: Both `PendingNotificationManager` and `SmsImportViewModel` dedup checks must include `NOTIFICATION_AUTO` alongside `SMS_AUTO`.
- **Room migration index naming**: `CREATE INDEX` names must match Room's convention `index_<tableName>_<col1>` or the `@Index` annotation must use the exact same custom name.
- **Claude API proxy**: Uses `Authorization: Bearer <key>` header (not `x-api-key`). Model: `claude-haiku-4.5`.
- **RemoteInput needs a MUTABLE PendingIntent** — with `FLAG_IMMUTABLE`, `getResultsFromIntent()` silently returns null. Use `PendingIntentCompat`, never the bare API-31 `FLAG_MUTABLE` constant.
- **A RemoteInput reply leaves a progress spinner** on the notification until the app re-notifies the same id or cancels it. Every branch of a reply receiver must end in a notify or cancel, or it hangs there forever.
- **`updateExpense` is lossy**: `ExpenseMapper.toEntity` hardcodes `accountNumber = null`, so any full-row round-trip drops `account_number`. It also rewrites the tag join table. Prefer a targeted `@Query` UPDATE for single-field writes.
- **`getExpenseByIdWithCategory` has no `is_deleted = 0` filter** — unlike every other query in that DAO. Guard soft-deletes yourself.
