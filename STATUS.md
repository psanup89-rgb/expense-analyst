# Expense Analyst — Current Status

**Date**: 2026-08-19
**DB version**: 20
**Build**: `./gradlew clean assembleDebug` ✅ (requires JDK 21)
**Version**: v0.7.0-debug
**Repo**: `https://github.com/psanup89-rgb/expense-analyst` (public)
**Open issues**: None

---

## Current Phase

**Phase 1 + Phase 1.5 + Phase 2 (Analytics F12, Budget F13, Loans F15) complete. Phase 2 remaining: CSV export, widget.**

---

## What Is Complete

### Infrastructure
- [x] 13-module Clean Architecture: `app`, `core`, `domain`, `data`, `feature/expenses`, `feature/emi`, `feature/notification`, `feature/settings`, `feature/analytics`, `feature/budget`, `feature/onboarding`, `feature/loans` + `:domain`
- [x] Room DB v20 — 13 entities, full migration history v1→v20
- [x] Hilt DI — 13 repository interfaces
- [x] Jetpack Navigation Compose — all routes registered
- [x] Multi-currency: live rates (ExchangeRate-API via Ktor) + offline seed fallback
- [x] Home currency preference (DataStore)

### Expense Features
- [x] Manual expense entry (EXPENSE, INCOME, TRANSFER, PAYMENT)
- [x] Expense list: date-grouped, search, category + payment filters, monthly navigation
- [x] Expense detail + Edit expense (recalculates homeAmount on currency change)
- [x] Source SMS card (expandable) + "Open in Messages ↗" deep link
- [x] Inline "Add new category" in category picker sheet
- [x] Soft-delete with undo (swipe)
- [x] EMI/instalment splitting + EMI list/detail
- [x] Tags (many-to-many, searchable)

### Notification / SMS Pipeline
- [x] `TransactionNotificationService` (NotificationListenerService)
- [x] Auto-save: detected bank SMS/notifications save directly as `Expense` — no tap required (DB v19)
- [x] Needs Review tab (bottom nav): expenses missing merchant, falling back to a generic category, or lacking payment method/account are flagged `needsReview=true` and surfaced here with a badge count; each card shows *which* field(s) triggered the flag ("Missing: Merchant, Account", DB v20); tap → edit clears the flag, checkmark → mark reviewed without opening
- [x] Pending Bill Statements queue (repurposed old Pending Inbox, BILL type only) — accessible via Bills screen, unchanged confirm-before-save flow
- [x] In-app banner ("Saved · tap to edit") + system tray notification (tap → expense detail)
- [x] **"Add note" inline reply** on the tray notification: type a description straight into the shade (`RemoteInput`), written to `Expense.description` without opening the app; notification then shows the saved note and self-dismisses (~4s). Blank replies re-post the original untouched; a soft-deleted expense reports failure. Handled by `NoteReplyReceiver`
- [x] Bulk SMS import with two-tier dedup (body hash + amount/day/merchant fallback); onboarding offers last-1-month / this-year / all-time
- [x] Live notification dedup (60s window + body hash vs saved expenses)

### Parsers
**Transaction (18):** HDFC, SBI, ICICI, Axis, Kotak, YesBank, IdfcFirstBank, OneCard, AlRajhi, StcBank, Alinma, D360, EmiratesNBD, FASTag, Wallet, UPI, Mubasher, Generic

**Bill statement (10):** HDFC, EmiratesNBD, AlRajhi, IdfcFirstBank, AxisBank, Tamara, SaudiEnergy, Ejar, Airtel, Generic

### Bills
- [x] PENDING → PARTIAL → SETTLED lifecycle
- [x] Add/Edit Bill screens, bill detail with linked expenses
- [x] Bill SMS → pending inbox as BILL type (not auto-saved as expense)
- [x] PAYMENT expenses auto-link to open bills via `BillMatcher` (±5% tolerance)
- [x] Bidirectional Expense ↔ Bill navigation

### Account Management
- [x] Add/Edit accounts; delete with expense remap

### Merchant Category Intelligence
- [x] Tier 1: MerchantRule DB lookup
- [x] Tier 2: keyword matching (~100 keywords)
- [x] Tier 3: Claude AI (`claude-haiku-4.5`) — gated by Settings toggle + `CLAUDE_API_KEY` in `local.properties`
- [x] Auto-save MerchantRule on manual category selection

### Analytics (F12)
- [x] Month navigation, summary cards, category breakdown, daily bar chart, top merchants, drill-down sheet

### Budget (F13)
- [x] Biometric-gated budget screen (Settings entry)
- [x] Salary tracking + planned expenses + planned-vs-actual comparison
- [x] Month navigation, carry-forward from previous month

### Loans / Lent (F15)
- [x] Track money lent to others (PENDING / SETTLED status)
- [x] WorkManager reminders with custom datetime
- [x] Settlement creates INCOME+Refund expense (nets out of monthly totals)
- [x] Entry: Settings → "Loans & Lending"

---

## Not Started (Phase 2 remaining)

| Feature | Notes |
|---------|-------|
| CSV/PDF export (F14) | Export filtered list; PDF is a stretch goal |
| Home screen widget (F16) | Monthly spend summary via Glance API |
| ProGuard release smoke-test | `proguard-rules.pro` exists; signing config needed |
| Google Drive backup | Not started |
| Paging 3 | Not started; all records load in memory currently |

---

## Known Constraints

- **`local.properties`** (gitignored): `CLAUDE_API_KEY=<key>` + `CLAUDE_API_BASE_URL=<url>` for Tier 3. Default base URL is `https://api.anthropic.com`.
- **JDK 21**: All modules target `JavaVersion.VERSION_21`. JDK 17 fails at compile time.
- **`kotlinx.serialization` compiler plugin absent**: Use `bodyAsText()` + `JsonElement` for Ktor JSON parsing, not `@Serializable` data classes.
- **`rawSmsBody` null on pre-2026-04-11 notification-path expenses**: Edit Expense shows "Auto-imported from SMS" label; expandable text only for newer records.
