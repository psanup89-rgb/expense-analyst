# Architecture Guide

## Overview
Expense Analyst uses **MVVM + Clean Architecture** with a multi-module Gradle project. The architecture prioritizes separation of concerns, an offline-first data model, and a clean migration path toward future KMP sharing.

## Module Dependency Graph

```
┌─────────────────────────────────────────────────────┐
│                      :app                            │
│   (Android app, MainActivity, NavGraph, DI wiring)   │
├─────────────────────────────────────────────────────┤
│                Feature Modules                       │
│  :feature:expenses  :feature:emi  :feature:settings  │
│  :feature:notification  :feature:onboarding          │
│  :feature:analytics (planned for Phase 2)            │
├─────────────────────────────────────────────────────┤
│              :domain (Pure Kotlin)                    │
│    Models, Repository Interfaces, Use Cases          │
├─────────────────────────────────────────────────────┤
│              :data (Android)                          │
│    Room DB, DAOs, Entities, API, Repo Impls          │
├─────────────────────────────────────────────────────┤
│              :core (Android)                          │
│    Theme, Components, Utilities, Navigation          │
└─────────────────────────────────────────────────────┘
```

### Dependency Rules
- `app` → everything
- `feature/*` → `domain`, `core` (NEVER `data` directly)
- `data` → `domain`, `core`
- `domain` → nothing (pure Kotlin, no Android deps)
- `core` → nothing (except Android/Compose framework)

## Layers

### Domain Layer (`:domain`)
Pure Kotlin module. Contains:
- **Models**: `Expense`, `Category`, `EmiGroup`, `CurrencyRate`, `Account`, `MerchantRule`, `PendingNotification`
- **Repository interfaces**: `ExpenseRepository`, `CategoryRepository`, `CurrencyRepository`, `EmiRepository`, `OnboardingRepository`, `AccountRepository`, `MerchantRuleRepository`, `PendingNotificationRepository`, `AppPreferencesRepository`
- **Use cases**: CRUD use cases for expenses/categories, currency helpers, EMI creation
- **Shared conversion logic**: `domain/util/CurrencyConversion.kt`

The domain module remains Android-free and is the only safe place for reusable business rules that must be shared across multiple features.

### Data Layer (`:data`)
Android module. Contains:
- **Room database**: `ExpenseAnalystDatabase`
- **Entities / relations / DAOs** for all 7 entities (Expense, Category, EmiGroup, CurrencyRate, Account, MerchantRule, PendingNotification)
- **Mappers** between Room entities and domain models
- **Repository implementations** for all 9 domain repository interfaces
- **DataStore-backed preferences** for home currency
- **Offline seed rates** via `SeedCurrencyRates`
- **Live currency sync**: `CurrencyApiService` fetches from ExchangeRate-API (`open.er-api.com`), cached in Room, refreshed daily via `isStale()` check

### Core Layer (`:core`)
Android module. Contains:
- **Theme**: `Theme.kt`, `Color.kt`, `Type.kt`, `Shape.kt`
- **Utilities**: `DateTimeUtil`, `CurrencyFormatter`, category icon mapping, navigation routes
- **Navigation**: `NavRoutes`

At the current project state, `:core` is still mostly theme + utility infrastructure rather than a large reusable-component library.

### Feature Modules
Each feature is an Android library module containing:
- **Screens**: Compose `@Composable` functions
- **ViewModels**: `@HiltViewModel` classes with `UiState`
- **Feature-specific workflow logic**

All feature modules are fully implemented:
- `:feature:expenses` — Expense list (date-grouped, filtered, searchable), Add/Edit/Detail screens
- `:feature:settings` — Home currency picker, notification toggle, SMS import trigger, about section
- `:feature:emi` — Create EMI from expense, EMI list (active/completed), EMI detail with installment timeline
- `:feature:onboarding` — 3-step flow: welcome → currency picker → notification access + SMS import
- `:feature:notification` — 17 bank parsers, NotificationListenerService, SMS import (bulk/browse), notification banner, PaymentMethodDetector

### App Module (`:app`)
- `ExpenseAnalystApp`: Application class with Hilt
- `MainActivity`: Single activity host
- `AppNavGraph`: central navigation
- `MainBottomNav`: Home / EMI / Settings

All routes are implemented: Home, Add/Edit/Detail Expense, EMI Create/List/Detail, Settings, SMS Import, Onboarding. Bottom nav: Home · EMI · Settings.

## Data Flow

```
UI (Compose Screen)
  ↓ user action
ViewModel (handles UI state)
  ↓ calls
Use Case (business logic)
  ↓ calls
Repository Interface (domain layer)
  ↓ implemented by
Repository Impl (data layer)
  ↓ calls
DAO / preferences / future API service
  ↓ returns
Flow<List<Entity>> mapped to Flow<List<DomainModel>>
  ↑ collected by ViewModel → UiState → UI recomposes
```

## Current Currency Pattern
- `Expense.amount` always stores the original transaction amount.
- `Expense.homeAmount` and `Expense.exchangeRate` are stored for home-currency reporting.
- Home currency currently defaults to `SAR` if no preference has been written yet.
- When home currency changes, the app recalculates stored conversions and the home list also repairs stale values on load.

## Key Patterns

### State Management
All screens use a **data class** for UI state (not sealed interface). The ViewModel exposes a `StateFlow<*UiState>` that the Compose screen collects with `collectAsStateWithLifecycle()`.

```kotlin
// Actual pattern — data class with defaults, never sealed interface
data class ExpenseListUiState(
    val groups: List<ExpenseGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // ... other fields with sensible defaults
)
```

### Repository Pattern
```kotlin
// Domain layer - interface
interface ExpenseRepository {
    fun getExpenses(): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Long
}

// Data layer - implementation (uses extension functions, not class-based mappers)
class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao
) : ExpenseRepository {
    override fun getExpenses(): Flow<List<Expense>> =
        dao.getAllExpensesWithCategory().map { list -> list.map { it.toDomain() } }
}
```

### Mapper Pattern
Mappers are Kotlin extension functions (not mapper classes):
```kotlin
// data/mapper/ExpenseMapper.kt
fun ExpenseWithCategory.toDomain(): Expense = Expense(...)   // Entity → Domain
fun Expense.toEntity(createdAt: Long, updatedAt: Long): ExpenseEntity = ExpenseEntity(...)  // Domain → Entity
```
The `ExpenseWithCategory` relation (Room `@Embedded` + `@Relation`) handles the category join.

### Use Case Pattern
```kotlin
class AddExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Long = repository.addExpense(expense)
}
```
Use cases are thin delegates that allow features to depend on `:domain` without knowing about `:data`.

## Timezone Strategy
- **Storage**: All timestamps as UTC epoch milliseconds (`Long`)
- **Display**: Convert to device timezone using `DateTimeUtil`
- **Grouping**: Date headers ("Today", "Yesterday", "March 15") use local timezone
- **Library**: `kotlinx-datetime` for KMP compatibility

## Offline-First Strategy
- All data stored in Room (local SQLite)
- Exchange rates cached locally, refreshed daily when online
- No mandatory server dependency — app works fully offline
- Cloud backup (Phase 2) is optional sync, not primary storage

## Future KMP Migration Path (Phase 3)
- `:domain` module → `shared/commonMain` (already pure Kotlin)
- `:data` module → split into `shared/commonMain` (repos) + platform-specific (Room builder)
- Feature UI → Compose Multiplatform for iOS
- Platform-specific: `expect`/`actual` for notification service, file paths, timezone
