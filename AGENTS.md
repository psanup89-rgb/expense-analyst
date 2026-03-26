# Expense Analyst - Agent Instructions

## Project Overview
Android-first expense tracking app that reads bank SMS/notifications to auto-create categorized expenses. Built with Kotlin, Jetpack Compose, Room, and Clean Architecture (MVVM).

## Key Conventions

### Architecture
- **Multi-module**: `app`, `core`, `domain`, `data`, `feature/*`
- **Dependency rule**: Feature → Domain → (nothing). Feature → Core. Data → Domain. Data → Core. App → all.
- **Domain module is pure Kotlin** — no Android dependencies
- **MVVM**: Each screen has a `*Screen.kt` (Compose), `*ViewModel.kt`, and `*UiState.kt`

### Code Style
- **Language**: Kotlin (100%)
- **UI**: Jetpack Compose with Material 3
- **Formatting**: ktlint (standard rules)
- **Static analysis**: Detekt
- **Naming**: PascalCase for classes/composables, camelCase for functions/variables, SCREAMING_SNAKE for constants
- **Package**: `com.expenseanalyst.<module>.<feature>.<layer>`

### Database
- **Room** with entities in `data/local/entity/`
- All dates stored as **UTC epoch milliseconds** (`Long`)
- Display layer converts to device timezone via `java.time.ZoneId.systemDefault()`
- **Soft delete** via `isDeleted: Boolean` flag — never hard delete
- Store both `amount` (original currency) and `homeAmount` (converted) on expenses

### DI
- **Hilt** for dependency injection
- Each module has a `di/` package with `@Module` classes
- Use `@HiltViewModel` for all ViewModels

### Testing
- Unit tests: JUnit 5 + MockK
- Flow testing: Turbine
- Room tests: in-memory database
- UI tests: Compose UI Test
- Parser tests: parameterized with CSV fixtures in `src/test/resources/sms_samples/`
- **Target**: 80%+ coverage on `:domain` and `:data` modules

### Navigation
- Jetpack Navigation Compose with type-safe routes
- Route constants in `core/navigation/NavRoutes.kt`
- Bottom nav: Home · EMI · Settings (all three fully implemented and wired)
- Analytics is a planned Phase 2 module — no module exists yet

### Currency
- ISO 4217 currency codes (e.g., "INR", "USD", "SAR")
- Home currency is stored in DataStore
- Current fallback home currency is `SAR`
- Exchange rates fetched live via Ktor from ExchangeRate-API, cached in Room with 24h stale check; falls back to `SeedCurrencyRates` offline
- Shared conversion math lives in `domain/util/CurrencyConversion.kt`
- Changing home currency in Settings rewrites stored `homeAmount` and `exchangeRate` values
- Always display both original and home currency when different

### Notification Parsing
- `NotificationListenerService` in `feature/notification/service/`
- Bank-specific parsers in `feature/notification/parser/`
- Each parser implements `TransactionParser` interface
- `ParserRegistry` dispatches by sender ID
- See `docs/NOTIFICATION_PARSING.md` for SOP on adding new parsers

## File Structure Quick Reference
```
app/src/main/              → MainActivity, NavGraph, DI wiring
core/src/main/             → Theme, reusable components, utilities
domain/src/main/           → Models, repository interfaces, use cases
data/src/main/             → Room DB, DAOs, entities, API, repo impls
feature/expenses/src/main/ → Expense CRUD screens
feature/emi/src/main/      → EMI management screens
feature/notification/src/  → Notification service + parsers
feature/settings/src/main/ → Settings screens
feature/onboarding/src/    → Onboarding flow
feature/analytics/src/     → Planned for Phase 2, not present yet
docs/                      → Architecture, data models, SOPs
```

## Common Tasks

### Adding a new screen
1. Create `*Screen.kt`, `*ViewModel.kt`, `*UiState.kt` in appropriate `feature/` module
2. Add route to `core/navigation/NavRoutes.kt`
3. Register in `app/navigation/AppNavGraph.kt`
4. Add Hilt module if new dependencies needed

### Adding a new bank parser
See `docs/NOTIFICATION_PARSING.md` for full SOP.

### Adding a new category
Add to pre-seed list in `data/local/ExpenseAnalystDatabase.kt` callback.

### Running the app
```bash
./gradlew clean assembleDebug   # ALWAYS use clean when verifying after code changes
./gradlew installDebug          # Build and install on emulator/device
./gradlew testDebugUnitTest     # Run unit tests
./gradlew ktlintCheck detekt    # Code quality checks
./gradlew assembleRelease       # Signed APK (ProGuard rules not yet configured)
```

> If you see `KSP failed with exit code: PROCESSING_ERROR` — see `.claude/skills/build-verify.md`.

### Updating Currency Logic
1. Keep conversion math centralized in `domain/util/CurrencyConversion.kt`
2. If you change how home currency is derived or stored, verify both Settings and Home flows
3. Rebuild and manually verify list totals, row-level converted values, and add-expense save behavior

## Important Documents
- `HANDOFF.md` — Project status and next steps
- `docs/ARCHITECTURE.md` — Architecture decisions
- `docs/DATA_MODELS.md` — Database schema and models
- `docs/NOTIFICATION_PARSING.md` — Parser SOP
- `docs/FEATURES.md` — Feature specs with acceptance criteria
- `docs/TESTING.md` — Testing strategy
- `docs/SETUP.md` — Dev environment setup

## Local Skills
- `.agents/skills/expense_analyst_sop/`
- `.agents/skills/expense_analyst_notification_parsing/`
- `.agents/skills/expense_analyst_testing_sop/`
- `.agents/skills/expense_analyst_setup_release/`
