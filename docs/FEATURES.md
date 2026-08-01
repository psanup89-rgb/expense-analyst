# Feature Specifications & Acceptance Criteria

**Last updated**: 2026-06-14
**Phase 1 + 1.5**: Complete
**Phase 2**: Partially started (F11 mostly complete, F12 complete, F13 complete, Loans/Lent complete, Auto-save + Needs Review complete)

---

## Phase 1 — MVP

### F1: Manual Expense Entry ✅
- [x] FAB on home screen opens Add Expense form
- [x] Fields: amount, currency, transaction type (Expense/Income/Transfer/Payment), merchant name (mandatory), category, payment method, account, date, description (optional), notes (optional)
- [x] Large numeric amount input with decimal support
- [x] Category displayed as scrollable grid of icons with labels
- [x] **Inline "Add new category"** in the category picker sheet — name field + icon grid, auto-selects on save (mirrors "Add new account" pattern)
- [x] Payment method shown as horizontal chips
- [x] Date picker defaults to today, allows past dates
- [x] Currency picker is searchable (code + name)
- [x] Validation: amount > 0, **merchant name not empty**, category selected, account selected
- [x] On save: expense appears in list immediately
- [x] If currency differs from home currency, `homeAmount` auto-calculated from cached exchange rate
- [ ] Manual rate entry when offline and no cached rate

---

### F2: Expense List ✅
- [x] Expenses newest-first, grouped by date headers ("Today", "Yesterday", "March 15, 2026")
- [x] Expense card: category icon, description/merchant, original amount, home amount (if different), payment method
- [x] Monthly summary card — shows Spent + Received totals in home currency
- [x] Month navigation (← month →) with "All months" option
- [x] Category filter chips (horizontal scrollable row)
- [x] Payment method filter chips (horizontal scrollable row)
- [x] Search bar (description, merchant, notes)
- [x] Swipe left to soft-delete
- [x] Tap to open detail view
- [x] Empty state
- [x] Undo snackbar after swipe-delete (4s delay, UNDO action in Snackbar)
- [ ] Pagination (currently loads all records; Paging 3 not integrated)

---

### F3: Edit Expense ✅
- [x] Expense detail → Edit button opens edit form
- [x] All Add Expense fields editable, pre-filled
- [x] Changing currency recalculates `homeAmount`
- [x] `updatedAtUtcMillis` updated on save
- [x] Changes reflected immediately in list
- [x] Cancel discards changes
- [x] **Source SMS card** shown for auto-imported expenses — expandable, shows raw SMS body text; "Open in Messages ↗" button deep-links to the sender's SMS conversation (falls back to messaging app main screen if SMS no longer in inbox)
- [x] Inline "Add new category" in category picker (same as Add Expense)

---

### F4: Notification Auto-Capture ✅
- [x] App registers as `NotificationListenerService`
- [x] Onboarding requests notification access with explanation
- [x] `ParserRegistry` dispatches to bank-specific parser by sender ID
- [x] Parsers extract: amount, type (debit/credit), merchant, currency
- [x] **Auto-save (DB v19)**: detected transactions save directly as `Expense`, no tap required. In-app banner shows "Saved · tap to edit" instead of a confirm prompt
- [x] **Needs Review tab**: expenses missing merchant, falling back to a generic category, or lacking payment method/account are flagged `needsReview=true` and surfaced in the Review bottom-nav tab with a badge; editing and saving clears the flag
- [x] "Dismiss" option on banner
- [x] Supported banks: HDFC, SBI, ICICI, Axis (incl. Forex cards), Kotak, Yes Bank, IDFC First Bank, OneCard (Federal Bank), Al Rajhi, STC Bank, Alinma, D360, Emirates NBD, FASTag (LivQuik), Google Wallet/Pay, Apple Pay, Samsung Pay, UPI apps, Mubasher (bill payment), generic fallback
- [x] Settings toggle to enable/disable auto-capture
- [x] Android system tray notification when transaction detected (tapping opens the saved expense's detail screen)
- [x] Bulk SMS import from device inbox (last 1 month / this year / all-time) with smart dedup (primary: SMS body hash; fallback: amount+day+merchant)
- [x] Merchant rules ("Teach App") — user-defined pattern→category, applied before keyword matching
- [x] PAYMENT transaction type for credit card / bill payments — still routed to the confirm-before-save "Pending Bill Statements" queue (unchanged), not auto-saved
- [ ] Duplicate detection for live notification capture (bulk import has it; live notifications do not)

---

### F5: Multi-Currency Support ✅
- [x] Home currency set during onboarding, changeable in settings
- [x] 150+ currencies in picker (via `CurrencyCatalog`)
- [x] Exchange rates from ExchangeRate-API, cached in Room, refreshed daily
- [x] Offline fallback: `SeedCurrencyRates` (~40 currencies)
- [x] `homeAmount` = `amount * (homeRate / foreignRate)`, stored on expense
- [x] Expense card shows both amounts
- [x] Monthly totals always in home currency
- [ ] Offline warning + manual rate entry when no cached rate for a currency

---

### F6: EMI / Installment Split ✅
- [x] "Convert to EMI" button on expense detail
- [x] Form: months (2–60), interest rate (optional), installment preview
- [x] Creates `EmiGroup` + N expense records (one per month)
- [x] EMI formula with and without interest
- [x] EMI List: active/completed tabs, progress cards
- [x] EMI Detail: installment timeline, paid/upcoming/cancelled status
- [x] "Cancel Remaining" soft-deletes future installments
- [ ] Editing an EMI installment shows group-member warning

---

### F7: Timezone Support ✅
- [x] All dates stored as UTC epoch milliseconds
- [x] Display converts via `TimeZone.currentSystemDefault()`
- [x] Date group headers use local timezone
- [x] EMI installment dates generated correctly across months

---

### F8: Onboarding ✅
- [x] Shown only on first launch (DataStore flag)
- [x] Step 1: Welcome screen
- [x] Step 2: Select home currency (searchable, toggle-select, pinned at top)
- [x] Step 3: Grant notification access (open system settings, or skip)
- [x] "Get Started" → saves home currency → marks onboarding complete → navigates to home
- [x] Gate in `MainActivity` prevents bypassing onboarding

---

### F9: Settings (partial)
- [x] Home currency picker
- [x] Theme toggle (dark/light mode)
- [x] Notification auto-capture toggle
- [x] Manage categories (add/edit/delete, icon picker)
- [x] Account Management (add/edit/delete with expense remap)
- [x] About section (version, credits)
- [x] Smart Category Detection toggle (Claude AI Tier 3, default off)
- [ ] Reorder categories

---

### F10: Dark Neon Tech Theme ✅
- [x] Deep dark backgrounds (#121212, #1E1E1E equivalents via Material 3 dark scheme)
- [x] Primary accent: Neon Lime Green (`#CCFF00`)
- [x] Material 3 color system adapted to neon aesthetic
- [x] No light mode for MVP

---

## Phase 2 (partially started)

| Feature | Status | Description |
|---------|--------|-------------|
| F11: Bills Section | Mostly ✅ | Full bill CRUD, lifecycle (PENDING→PARTIAL→SETTLED), 10 bill statement parsers (IDFC, Axis, EmiratesNBD, AlRajhi, HDFC, Tamara, SaudiEnergy, Ejar, Airtel, Generic), bill routing from SMS, bill SMS → pending inbox as BILL type (DB v14), PAYMENT bill linking in Add/Edit Expense, expense↔bill navigation, unlink payment from bill, Add Bill form matches Edit Bill. Remaining: subscription tracking, recurring bill auto-detection. |
| F12: Analytics Dashboard | ✅ Complete | Month navigation, summary cards, category breakdown, daily bar chart, top merchants, drill-down bottom sheet. |
| F13: Budgets | ✅ Complete | Biometric-gated budget section (Settings → Budget). Salary tracking (manual + auto-detect from INCOME transactions + history). Planned expenses (add/edit/soft-delete with category, carry-forward from previous month). Planned vs Actual comparison (category progress bars, unplanned expense flagging, summary card). Month navigation. DB v15: salary_entries + planned_expenses tables. |
| F14: Export | Not started | CSV + PDF with date range filter, Android share sheet |
| F15: Cloud Backup | Not started | Google Drive backup/restore via Google Sign-In, weekly auto-backup |
| Loans/Lent Tracking | ✅ Complete | New `:feature:loans` module. Track money lent to others (PENDING/SETTLED), WorkManager reminders with custom datetime, settlement creates INCOME+Refund expense (nets out of monthly totals). Entry: Settings → "Loans & Lending". DB v18: `lent_items` table. (Note: referred to as "F15" in `STATUS.md`/`HANDOFF.md`, overlapping the Cloud Backup number above — this table's F-numbers have never been fully reconciled between docs.) |
| Auto-save + Needs Review | ✅ Complete | Detected bank SMS/notifications auto-save as expenses (no tap). Expenses missing merchant/category/payment method/account are flagged and surfaced in a new Review bottom-nav tab. DB v19: `needs_review` column. See F4 above for full detail. |
| F16: Home Screen Widget | Not started | Glance widget: today's spend + monthly total |
| F17: Dynamic Colors | Not started | Material 3 dynamic color (opt-out of neon theme) |
| F18: Bulk Operations | Not started | Multi-select, bulk delete, bulk re-categorize |

---

## Phase 3 (not started)

F18: iOS App (KMP) · F19: Smart Categorization · F20: Recurring Detection · F21: Receipt Photos · F22: Split Expenses · F23: Income Tracking · F24: Multiple Accounts · F25: Tags ✅ (fully implemented: DB schema v10→v11, `TagRepository`, `TagDao`, searchable tag selector in Add/Edit Expense, tag chips in Expense Detail, search-by-tag in Expense List) · F26: Spending Insights · F27: Bill Reminders
