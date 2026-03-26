# Expense Analyst Dev Skill

You are the primary developer for the **Expense Analyst** Android app — an expense tracker that auto-captures transactions from bank SMS/notifications.

## Project Quick Reference

**Working directory**: `/Users/anup/AI Workspace/expense-analyst`

**Tech stack**: Kotlin, Jetpack Compose + Material 3, MVVM + Clean Architecture, Room 2.7, Ktor, Hilt, Jetpack Navigation Compose, JUnit 5 + MockK + Turbine

**Module structure** (multi-module Gradle):
```
:app              → MainActivity, NavGraph, AppModule (DI wiring)
:core             → Theme, reusable Compose components, NavRoutes, utilities
:domain           → Pure Kotlin: models, repo interfaces, use cases (NO Android deps)
:data             → Room DB, DAOs, entities, Ktor API clients, repo implementations
:feature:expenses → Expense list, add, edit, detail screens
:feature:emi      → EMI list, detail, create sheet
:feature:notification → NotificationListenerService, SMS parsers, confirmation UI
:feature:settings → Settings, currency config, category management
:feature:onboarding → First-launch flow
:feature:analytics → Charts/reports (Phase 2)
:shared           → KMP shared module (Phase 3 prep)
```

**Dependency rule**: Feature → Domain. Feature → Core. Data → Domain. Data → Core. App → all. Domain has NO Android dependencies.

## Key Conventions

### File/Package Naming
- Package root: `com.expenseanalyst.<module>.<feature>.<layer>`
- PascalCase: classes, composables, objects
- camelCase: functions, variables, parameters
- SCREAMING_SNAKE: constants
- Each screen: `*Screen.kt` (Compose), `*ViewModel.kt`, `*UiState.kt`

### Database Rules
- All dates: UTC epoch milliseconds (`Long`)
- Display: convert via `java.time.ZoneId.systemDefault()`
- Soft delete via `isDeleted: Boolean` — never hard delete
- Store both `amount` (original currency) and `homeAmount` (converted)

### DI (Hilt)
- All ViewModels: `@HiltViewModel`
- Each module: `di/` package with `@Module` classes

### Testing
- JUnit 5 + MockK for unit tests
- Turbine for Flow testing
- Room in-memory DB for DAO tests
- Compose UI Test for screen tests
- Parser tests: parameterized with CSV fixtures in `src/test/resources/sms_samples/`
- Coverage target: 80%+ on `:domain` and `:data`

## Key Documents
- `HANDOFF.md` — current project status and next steps
- `docs/ARCHITECTURE.md` — architecture decisions
- `docs/DATA_MODELS.md` — DB schema and entity definitions
- `docs/NOTIFICATION_PARSING.md` — parser SOP
- `docs/FEATURES.md` — feature specs with acceptance criteria
- `docs/TESTING.md` — testing strategy
- `docs/SETUP.md` — dev environment setup

## Common Tasks

### Adding a new screen
1. Create `*Screen.kt`, `*ViewModel.kt`, `*UiState.kt` in the appropriate `feature/` module
2. Add route constant to `core/src/main/com/expenseanalyst/core/navigation/NavRoutes.kt`
3. Register composable in `app/src/main/com/expenseanalyst/app/navigation/AppNavGraph.kt`
4. Add Hilt `@Module` if new dependencies are needed

### Adding a new use case
1. Define interface/class in `domain/src/main/com/expenseanalyst/domain/usecase/`
2. Implement in the same file (use cases are concrete classes, not interfaces)
3. Inject repository via constructor
4. Write unit tests with MockK

### Running the project
```bash
./gradlew clean assembleDebug    # ALWAYS use clean when verifying after code changes (see build-verify skill)
./gradlew installDebug           # Build + install on emulator/device
./gradlew testDebugUnitTest      # Run all unit tests
./gradlew ktlintCheck detekt     # Code quality checks
./gradlew assembleRelease        # Build signed APK
```

> **If you see `KSP failed with exit code: PROCESSING_ERROR`**: read `.claude/skills/build-verify.md` before doing anything else.

## Implementation Phases
- **Phase 1A**: Foundation (modules, DI, Room, Navigation, theme)
- **Phase 1B**: Core CRUD (expenses repo, use cases, list/add/edit screens)
- **Phase 1C**: Multi-currency (Ktor currency API, offline cache, picker)
- **Phase 1D**: Notification parsing (service, bank parsers, confirmation UI)
- **Phase 1E**: EMI feature (groups, installments, cancel)
- **Phase 1F**: Polish (search, accessibility, Paging 3, signed APK)
- **Phase 2**: Analytics, budgets, export, cloud backup, widgets
- **Phase 3**: iOS via KMP, smart categorization, receipt photos

## Current Status
Phase 1 is feature-complete. All screens are wired in `AppNavGraph` with real implementations. Phase 1F polish done: undo snackbar, notification auto-capture toggle, about section. Remaining Phase 1F: Paging 3, Settings theme toggle + category management, duplicate notification detection, test coverage. Phase 2 (analytics, budgets, export) not started. See `HANDOFF.md` for authoritative status.

Check `HANDOFF.md` for the authoritative current status and known gaps.
