# Expense Analyst — Agent Roles

This document defines the agent breakdown for this codebase. Each agent owns a specific domain, has clear responsibilities, and has explicit boundaries.

---

## Agent 1 — ParserAgent

**Domain**: `feature/notification/parser/` and `feature/notification/src/test/`

**Responsibilities**
- Add new bank SMS parsers (implement `TransactionParser` interface)
- Fix existing parser regex patterns (amount extraction, merchant extraction, account last-4)
- Add new bill statement parsers (implement `BillStatementParser` interface)
- Write and maintain parser unit tests (JUnit 5, CSV fixtures in `src/test/resources/sms_samples/`)
- Update `ParserRegistry` and `BillStatementParserRegistry` registration order
- Maintain `PaymentMethodDetector` inference logic

**Never touch**
- Any UI code (`*Screen.kt`, `*ViewModel.kt`)
- Database entities or DAOs
- Domain models or repository interfaces
- The notification service layer (`service/`)
- Navigation routes or AppNavGraph

**Handoff to FeatureAgent when**
- A new parser type requires a new domain model field (e.g. adding a new `ParsedTransaction` field)
- A new parser result needs to flow into a new UI concept

**Key files**
- `docs/NOTIFICATION_PARSING.md` — SOP for adding parsers
- `feature/notification/parser/ParserRegistry.kt` — always register before GenericParser
- `feature/notification/parser/PaymentMethodDetector.kt` — use this, never inline detection

---

## Agent 2 — FeatureAgent

**Domain**: `feature/expenses/`, `feature/emi/`, `feature/bills/`, `feature/settings/`, `feature/onboarding/`, and `app/`

**Responsibilities**
- Build and modify screens (`*Screen.kt`, `*ViewModel.kt`, `*UiState.kt`)
- Wire navigation (add routes to `NavRoutes.kt`, register in `AppNavGraph.kt`)
- Implement new use cases in `:domain` (add to `domain/usecase/`)
- Update bottom navigation items and `MainActivity`
- Add new domain model fields that carry through to UI
- Implement Phase 2 features (analytics, budgets, export)

**Never touch**
- Parser logic in `feature/notification/parser/` (that is ParserAgent's domain)
- Room migrations directly — coordinate with DataAgent for any schema change
- `CurrencyConversion.kt` — currency math is owned by DataAgent/architecture review

**Handoff to DataAgent when**
- A new screen requires a new DB column, table, or query

**Key files**
- `CLAUDE.md` — window insets rules, TopAppBar convention, navigation patterns
- `core/navigation/NavRoutes.kt` — all routes defined here
- `app/navigation/AppNavGraph.kt` — all composable registrations here

---

## Agent 3 — DataAgent

**Domain**: `:data` module, `:domain` models and repository interfaces, `ExpenseAnalystDatabase.kt`

**Responsibilities**
- Write Room migrations (always inline in `ExpenseAnalystDatabase.kt`, bump `@Database(version = N)`)
- Add/modify DAOs, entities, mappers
- Add/modify repository interfaces in `:domain` and implementations in `:data`
- Maintain `CurrencyConversion.kt` as the single source of truth for exchange rate math
- Maintain `CategoryInference.kt` for rule-based + keyword categorisation
- Manage `SeedCurrencyRates.kt` for offline fallback rate updates
- Maintain `ExpenseRepositoryImpl.kt` deduplication logic

**Never touch**
- Any `*Screen.kt` or `*ViewModel.kt` — that is FeatureAgent's domain
- Parser files — that is ParserAgent's domain
- `CategoryInference.kt` keyword lists without understanding the full inference chain

**Handoff to FeatureAgent when**
- A migration and new repository method are complete and ready for UI wiring

**Critical rules**
- DB is currently at **version 11**. Next migration must be `MIGRATION_11_12`
- Always add the new migration to the `addMigrations(...)` call in `ExpenseAnalystDatabase`
- Never hard-delete — always use soft delete (`isDeleted = true`)
- All timestamps are UTC epoch milliseconds (`Long`) — never `LocalDate` or `Date` in entities
- Run `./gradlew clean assembleDebug` after any schema change (KSP stale state)

**Key files**
- `data/local/ExpenseAnalystDatabase.kt` — version, migrations, pre-seed
- `domain/util/CurrencyConversion.kt` — never duplicate conversion logic elsewhere
- `data/schemas/` — Room schema snapshots (auto-generated, verify after migration)

---

## Agent 4 — QAAgent

**Domain**: All test files, `docs/TESTING.md`, build verification

**Responsibilities**
- Write missing parser tests (currently 13 of 17 parsers have no tests)
- Write DAO integration tests (in-memory Room DB)
- Write ViewModel unit tests (MockK + Turbine)
- Verify build passes after changes from other agents
- Maintain CSV fixture files in `feature/notification/src/test/resources/sms_samples/`
- Update `docs/TESTING.md` as coverage grows

**Never touch**
- Production source files (only `src/test/` and `src/androidTest/`)
- `build.gradle.kts` files except to add test dependencies

**Handoff rule**
- QAAgent should be the last to run before any commit that touches parsers or data layer

**Key commands**
```bash
./gradlew clean assembleDebug                         # Full build verification
./gradlew :feature:notification:testDebugUnitTest     # Parser tests only
./gradlew :domain:test                                # Domain use case tests
```

---

## Inter-Agent Handoff Protocol

1. Agent completing work updates `STATUS.md` (mark completed items, note new in-progress)
2. Agent completing work updates `HANDOFF.md` with session summary and "next agent should..." note
3. DB version changes must be noted explicitly in the handoff (`DB is now vN`)
4. Parser registry changes must note the new parser name, detection strategy, and position in registry
5. Any new nav route must be noted so FeatureAgent can register it in `AppNavGraph`
