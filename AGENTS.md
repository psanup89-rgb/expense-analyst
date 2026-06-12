# Expense Analyst — Agent Roles

Defines agent domains, responsibilities, and boundaries.

---

## Agent 1 — ParserAgent

**Domain**: `feature/notification/parser/` and `feature/notification/src/test/`

**Responsibilities**
- Add new bank SMS parsers (implement `TransactionParser`)
- Add new bill statement parsers (implement `BillStatementParser`)
- Fix regex patterns (amount, merchant, account last-4, reference numbers)
- Write/maintain parser unit tests (JUnit 5, CSV fixtures in `src/test/resources/sms_samples/`)
- Update `ParserRegistry` and `BillStatementParserRegistry` registration order
- Maintain `PaymentMethodDetector`

**Never touch**: UI code, DB entities/DAOs, domain models, service layer, navigation

**Handoff to FeatureAgent when**: A new parser result needs a new domain model field or a new UI concept

**Key files**
- `docs/NOTIFICATION_PARSING.md` — SOP for adding parsers
- `feature/notification/parser/ParserRegistry.kt` — register before `GenericParser`
- `feature/notification/parser/BillStatementParserRegistry.kt` — register before `GenericStatementParser`
- `feature/notification/parser/PaymentMethodDetector.kt` — use this, never inline detection

---

## Agent 2 — FeatureAgent

**Domain**: `feature/expenses/`, `feature/emi/`, `feature/settings/`, `feature/analytics/`, `feature/budget/`, `feature/loans/`, `feature/onboarding/`, `app/`

**Responsibilities**
- Build/modify screens (`*Screen.kt`, `*ViewModel.kt`, `*UiState.kt`)
- Wire navigation (`NavRoutes.kt` + `AppNavGraph.kt`)
- Implement new use cases in `:domain`
- Update bottom nav and `MainActivity`

**Never touch**: Parser logic in `feature/notification/parser/`; Room migrations (coordinate with DataAgent)

**Handoff to DataAgent when**: A new screen requires a new DB column, table, or query

**Compose gotcha (don't repeat)**: `LazyColumn` inside `AlertDialog`'s `text` slot renders nothing. Use `Column + verticalScroll(rememberScrollState())` with `heightIn(max = Xdp)` instead.

**Key files**
- `CLAUDE.md` — window insets rules, TopAppBar convention
- `core/navigation/NavRoutes.kt` — all route constants
- `app/navigation/AppNavGraph.kt` — all composable registrations

---

## Agent 3 — DataAgent

**Domain**: `:data` module, `:domain` models and repository interfaces, `ExpenseAnalystDatabase.kt`

**Responsibilities**
- Write Room migrations (always inline in `ExpenseAnalystDatabase.kt`, bump `@Database(version = N)`)
- Add/modify DAOs, entities, mappers, repository interfaces and impls
- Maintain `CurrencyConversion.kt` as single source of truth for exchange rate math
- Maintain `SeedCurrencyRates.kt` for offline fallback

**Never touch**: Any `*Screen.kt` or `*ViewModel.kt`; parser files

**Critical rules**
- DB is currently at **version 18**. Next migration must be `MIGRATION_18_19`
- Always add the new migration to `addMigrations(...)` in `ExpenseAnalystDatabase`
- Never hard-delete — always soft delete (`isDeleted = true`)
- All timestamps are UTC epoch milliseconds (`Long`) — never `LocalDate`, `Date`, or `ZonedDateTime` in entities
- Run `./gradlew clean assembleDebug` after any schema change

**Key files**
- `data/local/ExpenseAnalystDatabase.kt` — version, migrations, pre-seed
- `domain/util/CurrencyConversion.kt` — never duplicate conversion logic elsewhere
- `data/schemas/` — Room schema snapshots (auto-generated, commit after migration)

---

## Agent 4 — QAAgent

**Domain**: All test files, `docs/TESTING.md`, build verification

**Responsibilities**
- Write missing parser tests (most parsers have no tests — large gap)
- Write DAO integration tests (in-memory Room DB)
- Write ViewModel unit tests (MockK + Turbine)
- Maintain CSV fixture files in `feature/notification/src/test/resources/sms_samples/`

**Never touch**: Production source files

**Key commands**
```bash
./gradlew clean assembleDebug                         # Full build verification
./gradlew :feature:notification:testDebugUnitTest     # Parser tests only
./gradlew :domain:test                                # Domain use case tests
```

---

## Inter-Agent Handoff Protocol

1. Agent completing work updates `STATUS.md` and `HANDOFF.md`
2. DB version changes must be noted explicitly (`DB is now vN`)
3. Parser registry changes: note parser name, detection strategy, registry position
4. Any new nav route must be noted so FeatureAgent can register it in `AppNavGraph`
