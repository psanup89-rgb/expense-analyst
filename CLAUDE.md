# Expense Analyst — Agent Instructions

## Project Overview
Android-first expense tracking app that reads bank SMS/notifications to auto-create categorized expenses. Built with Kotlin, Jetpack Compose, Room, and Clean Architecture (MVVM). Phase 1 + Phase 1.5 complete. Phase 2 (analytics, budgets, export) not started.

---

## Architecture

- **Multi-module**: `app` · `core` · `domain` · `data` · `feature/expenses` · `feature/emi` · `feature/notification` · `feature/settings` · `feature/onboarding`
- **Dependency rule**: Feature → Domain. Feature → Core. Data → Domain. Data → Core. App → all. **Features never import from `:data`.**
- **Domain is pure Kotlin** — zero Android dependencies
- **MVVM**: Every screen has `*Screen.kt` + `*ViewModel.kt` + `*UiState.kt`

---

## Code Conventions

| Area | Convention |
|------|-----------|
| Language | Kotlin 100% |
| UI | Jetpack Compose + Material 3 |
| Formatting | ktlint (standard rules) |
| Static analysis | Detekt |
| Naming | PascalCase classes/composables, camelCase functions/vars, SCREAMING_SNAKE constants |
| Package | `com.expenseanalyst.<module>.<layer>` |

---

## Database

- **Room** — entities in `data/local/entity/`. **Current version: 9**. All migrations inline in `ExpenseAnalystDatabase.kt`.
- Dates: **UTC epoch milliseconds** (`Long`). Display converts via `TimeZone.currentSystemDefault()`
- **Soft delete** — `isDeleted: Boolean` flag. Never hard-delete.
- Expenses store both `amount` (original currency) and `homeAmount` (converted to home currency)
- `Expense` has: `merchantName` (primary, mandatory in UI), `description` (optional user notes), `accountId`, `rawSmsBody`
- `TransactionType`: `EXPENSE | INCOME | TRANSFER | PAYMENT`
- `AccountType`: `SAVINGS | CURRENT | CREDIT_CARD | DEBIT_CARD | FOREX_CARD | WALLET | OTHER`
- 7 entities: Expense, Category, EmiGroup, CurrencyRate, **Account**, **MerchantRule**, **PendingNotification**
- Pre-seeded categories: Food, Transport, Shopping, Bills, Entertainment, Health, Education, Groceries, Rent, Salary, Transfer, Other

---

## DI (Hilt)

- `@HiltViewModel` on all ViewModels
- Each module has a `di/` package with `@Module` classes
- `@AndroidEntryPoint` on `MainActivity` and `TransactionNotificationService`
- 7 repository interfaces in `:domain`: Expense, Category, Currency, EMI, Onboarding, **Account**, **MerchantRule**

---

## Navigation

- Routes defined in `core/navigation/NavRoutes.kt`
- All routes registered in `app/navigation/AppNavGraph.kt`
- Bottom nav: **Home · EMI · Settings** (shown only on those three destinations)
- Onboarding gate: `MainActivity` reads `OnboardingRepository.isOnboardingCompleted()` before rendering nav
- Notification pre-fill: `ADD_EXPENSE_ROUTE` has optional args `?amount=&currency=&merchant=&type=`

---

## Window Insets (important — do not regress)

`enableEdgeToEdge()` is called in `MainActivity`. The outer `Scaffold` in `MainActivity` handles the status-bar inset via `innerPadding.top` passed down to `NavHost`.

- **ExpenseListScreen**: Has no inner Scaffold/TopAppBar. Title is the first `LazyColumn` item.
- **All other screens with TopAppBar**: Must set `windowInsets = WindowInsets(0, 0, 0, 0)` on the `TopAppBar` to avoid double status-bar padding.

---

## Currency

- ISO 4217 codes (`"INR"`, `"USD"`, `"SAR"`)
- Home currency stored in DataStore; default fallback is `SAR`
- Live sync: ExchangeRate-API (`https://open.er-api.com/v6/latest/USD`) via Ktor. Falls back to `SeedCurrencyRates` offline
- Rates cached in Room; `isStale()` triggers refresh after 24 hours
- **All conversion math is in `domain/util/CurrencyConversion.kt`** — do not duplicate logic elsewhere
- Changing home currency rewrites `homeAmount`/`exchangeRate` on all stored expenses
- Always show both original and home currency amounts when they differ

---

## Notification Parsing

- Service: `feature/notification/service/TransactionNotificationService` (NotificationListenerService)
- Parsers: `feature/notification/parser/` — one file per bank, all implement `TransactionParser`
- Registry: `ParserRegistry` tries parsers in priority order; `GenericParser` is last resort
- `TransactionDirection`: `DEBIT | CREDIT | PAYMENT` (PAYMENT = bill/card payment confirmation)
- Parsed result flows via `PendingNotificationManager` → `NotificationBanner` UI **and** Android system tray notification (`TransactionAlertNotification.post()`)
- `MainViewModel.pendingRoute` receives tray notification taps; `AppNavGraph` navigates once
- **Parser bug to avoid**: two-group amount regex — `groupValues[1]` is `""` not `null` when only group 2 matches. Always use `.takeIf { it.isNotBlank() }` when extracting from either group.
- See `docs/NOTIFICATION_PARSING.md` for SOP on adding new parsers

---

## EMI

- `CreateEmiFromExpenseUseCase` takes an expense + months + optional interest rate → creates `EmiGroup` + N expense entries
- Installments are regular `Expense` records linked via `emiGroupId` and `emiInstallmentNumber`
- "Paid" status = installment date is in the past and not soft-deleted
- Cancel remaining = soft-delete all future installments

---

## File Structure

```
app/src/main/              → MainActivity, NavGraph, DI wiring, MainBottomNav
core/src/main/             → Theme, reusable components, CurrencyFormatter, DateTimeUtil, CurrencyCatalog
domain/src/main/           → Models, repository interfaces, use cases, CurrencyConversion
data/src/main/             → Room DB (6 entities/DAOs, v6), repositories, CurrencyApiService, SeedCurrencyRates
feature/expenses/          → Expense list, add, edit, detail screens + ViewModels
feature/emi/               → EMI create, list, detail screens + ViewModels
feature/notification/      → NotificationListenerService, parsers, banner UI
feature/settings/          → Settings screen + ViewModel
feature/onboarding/        → 3-step onboarding screen + ViewModel
docs/                      → ARCHITECTURE.md, DATA_MODELS.md, NOTIFICATION_PARSING.md, FEATURES.md, TESTING.md, SETUP.md
```

---

## Common Tasks

### Adding a new screen
1. Create `*Screen.kt`, `*ViewModel.kt`, `*UiState.kt` in the appropriate `feature/` module
2. Add route constant to `core/navigation/NavRoutes.kt`
3. Register composable in `app/navigation/AppNavGraph.kt`
4. Add `windowInsets = WindowInsets(0, 0, 0, 0)` to any `TopAppBar`

### Adding a new bank parser
See `docs/NOTIFICATION_PARSING.md`.

### Adding a new category
Add to the pre-seed callback in `data/local/ExpenseAnalystDatabase.kt`.

### Build commands
```bash
./gradlew clean assembleDebug   # ALWAYS use clean when verifying after code changes
./gradlew installDebug          # Build and install on connected emulator/device
./gradlew testDebugUnitTest     # Run unit tests
./gradlew ktlintCheck detekt    # Code quality
./gradlew assembleRelease       # Signed APK
```

### KSP build issues — CRITICAL
**Always run `./gradlew clean assembleDebug`**, never just `assembleDebug`, when verifying a build after adding/changing files. KSP incremental processing is disabled (`ksp.incremental=false` in `gradle.properties`) to prevent stale cross-module symbol errors, but a clean is still required for the first build after new files are added.

See `.claude/skills/build-verify.md` for the full diagnosis SOP and Android Studio manual fix steps.

---

## Testing

- Unit tests: JUnit 5 + MockK
- Flow testing: Turbine
- Room tests: in-memory DB (not yet written — gap)
- UI tests: Compose UI Test (not yet written — gap)
- Parser tests: JUnit 5 parameterized, CSV fixtures in `src/test/resources/sms_samples/`
- Target: 80%+ coverage on `:domain` and `:data`

---

## Key Documents
- `HANDOFF.md` — Current implementation status and Phase 2 backlog
- `docs/ARCHITECTURE.md` — Module dependency graph, architectural decisions
- `docs/DATA_MODELS.md` — Full Room schema and field descriptions
- `docs/NOTIFICATION_PARSING.md` — Parser SOP and regex patterns
- `docs/FEATURES.md` — Feature specs with acceptance criteria
- `docs/TESTING.md` — Testing strategy
- `docs/SETUP.md` — Dev environment setup
