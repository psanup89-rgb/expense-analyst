# Feature Specifications & Acceptance Criteria

**Last updated**: 2026-03-26
**Phase 1 + 1.5**: Complete
**Phase 2**: Not started

---

## Phase 1 — MVP

### F1: Manual Expense Entry ✅
- [x] FAB on home screen opens Add Expense form
- [x] Fields: amount, currency, transaction type (Expense/Income/Transfer/Payment), merchant name (mandatory), category, payment method, account, date, description (optional), notes (optional)
- [x] Large numeric amount input with decimal support
- [x] Category displayed as scrollable grid of icons with labels
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

---

### F4: Notification Auto-Capture ✅
- [x] App registers as `NotificationListenerService`
- [x] Onboarding requests notification access with explanation
- [x] `ParserRegistry` dispatches to bank-specific parser by sender ID
- [x] Parsers extract: amount, type (debit/credit), merchant, currency
- [x] In-app banner: "₹450 at Swiggy detected. Tap to save."
- [x] Tapping banner opens Add Expense pre-filled with parsed data
- [x] "Dismiss" option on banner
- [x] Supported banks: HDFC, SBI, ICICI, Axis (incl. Forex cards), Kotak, Yes Bank, Al Rajhi, STC Bank, Alinma, D360, Google Wallet/Pay, Apple Pay, Samsung Pay, UPI apps, generic fallback
- [x] Settings toggle to enable/disable auto-capture
- [x] Android system tray notification when transaction detected (tapping pre-fills AddExpense)
- [x] Bulk SMS import from device inbox (all-time or last 30 days) with (amount+day) dedup
- [x] Merchant rules ("Teach App") — user-defined pattern→category, applied before keyword matching
- [x] PAYMENT transaction type for credit card / bill payments (purple, excluded from expense totals)
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
- [ ] Theme: Light / Dark / System toggle
- [x] Notification auto-capture toggle
- [ ] Manage categories (add/edit/delete/reorder)
- [x] About section (version, credits)

---

### F10: Dark Neon Tech Theme ✅
- [x] Deep dark backgrounds (#121212, #1E1E1E equivalents via Material 3 dark scheme)
- [x] Primary accent: Neon Lime Green (`#CCFF00`)
- [x] Material 3 color system adapted to neon aesthetic
- [x] No light mode for MVP

---

## Phase 2 (not started)

| Feature | Description |
|---------|-------------|
| F11: Bills Section | Track credit card statements, utility bills, subscriptions from SMS. Match payments (PAYMENT type) to open bills and mark as paid. Needs new `Bill` model + DB migration + new screen. |
| F12: Analytics Dashboard | Monthly pie chart by category, daily bar chart, trend line, top merchants |
| F13: Budgets & Alerts | Monthly budget per category, progress bars, push notifications at 80%/100% |
| F14: Export | CSV + PDF with date range filter, Android share sheet |
| F15: Cloud Backup | Google Drive backup/restore via Google Sign-In, weekly auto-backup |
| F16: Home Screen Widget | Glance widget: today's spend + monthly total |
| F17: Dynamic Colors | Material 3 dynamic color (opt-out of neon theme) |
| F18: Bulk Operations | Multi-select, bulk delete, bulk re-categorize |

---

## Phase 3 (not started)

F18: iOS App (KMP) · F19: Smart Categorization · F20: Recurring Detection · F21: Receipt Photos · F22: Split Expenses · F23: Income Tracking · F24: Multiple Accounts · F25: Tags · F26: Spending Insights · F27: Bill Reminders
