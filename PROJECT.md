# Expense Analyst — Project Definition

## Goal

Expense Analyst is an Android application that automatically detects and categorises personal financial transactions by reading bank SMS messages and push notifications. The core value proposition is zero-friction expense tracking: when a bank transaction happens, the app captures it from the SMS, parses the amount, merchant, currency, and payment method, and asks the user to confirm before saving — reducing manual entry to a single tap. The app also supports manual expense entry, EMI/instalment splitting, multi-currency with live exchange rates, and bill tracking from credit card statement SMS messages.

## Scope

**In scope**
- Automatic transaction capture from bank SMS (via `RECEIVE_SMS` + `READ_SMS` permissions)
- Automatic transaction capture from push notifications (via `NotificationListenerService`)
- Bulk historical SMS import with deduplication
- Manual expense entry (all transaction types: EXPENSE, INCOME, TRANSFER, PAYMENT)
- Expense list with search, category/payment filters, monthly navigation
- Expense detail with editable auto-categorisation rules ("Teach App")
- EMI/instalment splitting from any expense
- Bills tracking from credit card statement SMS (PENDING → PARTIAL → SETTLED lifecycle)
- Pending inbox (detected transactions awaiting user confirmation)
- Multi-currency support with daily live exchange rate sync (ExchangeRate-API)
- Account tracking (bank accounts and credit cards, matched from SMS sender + last-4 digits)
- 18 bank-specific SMS parsers covering Indian and Saudi banks
- 10 bill statement parsers (HDFC, EmiratesNBD, AlRajhi, IdfcFirstBank, AxisBank, Tamara, SaudiEnergy, Ejar, Airtel, Generic)
- Onboarding flow (currency selection, notification permission)
- Settings (home currency, notification access toggle, SMS import trigger)

**Explicitly out of scope**
- iOS (planned Phase 3 via KMP, not started)
- Email parsing (architecture discussed, not implemented)
- CSV/PDF export (Phase 2 — not started)
- Alerts / push reminders (Phase 2 — not started)
- Cloud backup / Google Drive sync (Phase 2)
- Web or desktop interface
- Multi-user / shared expenses

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 100% |
| UI | Jetpack Compose + Material 3 (dark neon theme) |
| Architecture | Clean Architecture — MVVM, multi-module |
| DI | Hilt (KSP) |
| Database | Room v18 (SQLite), 13 entities, inline migrations |
| Async | Kotlin Coroutines + Flow |
| HTTP | Ktor client (ExchangeRate-API for live rates) |
| Navigation | Jetpack Navigation Compose |
| State | `StateFlow<UiState>` per screen, data class UiState |
| Preferences | Jetpack DataStore |
| Build | Gradle 9.3.1 + AGP, KSP, Version Catalog (libs.versions.toml) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Test | JUnit 5 + MockK + Turbine |

## Module Structure

```
:app          → Single-activity host, navigation root, bottom nav, DI entry point
:core         → Theme, NavRoutes, shared utilities (formatting, dates, currencies)
:domain       → Pure Kotlin — models, repository interfaces, use cases, business logic
:data         → Room DB, DAOs, entities, mappers, repository impls, API service
:feature:expenses     → Add/Edit/Detail/List/Bills screens + ViewModels
:feature:emi          → EMI create/list/detail screens + ViewModels
:feature:notification → 17+4 parsers, SMS service, SmsReceiver, inbox, import UI
:feature:settings     → Settings screen, Account Management + ViewModels
:feature:analytics    → Analytics dashboard screen + ViewModel
:feature:budget       → Budget screen (salary, planned expenses, biometric gate) + ViewModel
:feature:loans        → Loans/Lent tracking screens + WorkManager reminders + ViewModels
:feature:onboarding   → 3-step onboarding screen + ViewModel
```

**Dependency rule (enforced by module boundaries):**
`:feature/*` → `:domain` + `:core` only. Never `:data`.
`:data` → `:domain` + `:core`. Never `:feature`.
`:domain` → nothing (pure Kotlin).

## Architectural Decisions Already Baked In

- **Soft delete everywhere**: `isDeleted: Boolean` flag on all deletable entities. No hard deletes.
- **UTC epoch milliseconds for all timestamps**: Display converts via `TimeZone.currentSystemDefault()`.
- **Dual currency storage**: Every expense stores both `amount` (original) and `homeAmount` (converted). All conversion math lives in `domain/util/CurrencyConversion.kt` — single source of truth.
- **Parser registry pattern**: `ParserRegistry` tries 17 parsers in priority order; `GenericParser` is always the last fallback. Same pattern for `BillStatementParserRegistry` (4 parsers).
- **SMS dedup two-tier**: Primary = raw SMS body hash. Fallback = amount + calendar day + merchant (for old records without `rawSmsBody`).
- **Account matching**: Requires both bank name AND last-4 when both are known. Single-field match only when one is absent.
- **KSP reliability**: `ksp.incremental=false` in `gradle.properties` + root `build.gradle.kts` marks every `ksp*Kotlin` task as never-up-to-date. Always run `./gradlew clean assembleDebug`, never just `assembleDebug`.

## Non-Goals

- This app does NOT replace a budgeting tool (no budgets, no spending goals in Phase 1)
- This app does NOT sync across devices (single-device, local storage only in Phase 1)
- This app does NOT read SMS from non-financial senders (keyword + sender filtering applied)
- This app does NOT modify, delete, or send SMS messages
- This app does NOT transmit personal financial data to any server (exchange rates only)
- This app does NOT support light mode (dark neon theme is the only theme currently)
