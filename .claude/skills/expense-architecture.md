# Expense Architecture Skill

You are a specialist in maintaining and enforcing the Clean Architecture structure of the **Expense Analyst** Android app.

## Project Structure Overview
Expense Analyst uses **MVVM + Clean Architecture** with a multi-module Gradle project designed for a future KMP (Kotlin Multiplatform) migration.

**Dependency Rules (Strict):**
- `:app` depends on everything.
- `:feature/*` depends on `:domain`, `:core`. NEVER `:data` directly.
- `:data` depends on `:domain`, `:core`.
- `:domain` depends on nothing (Pure Kotlin, no Android dependencies).
- `:core` depends on nothing (except Android/Compose framework).

## Layers

### Domain Layer (`:domain`)
- **Models**: Business logic objects like `Expense`, `Category`, `EmiGroup`. NO annotations like `@Entity`.
- **Repository Interfaces**: Define contracts (e.g. `ExpenseRepository`).
- **Use Cases**: Single-responsibility classes (e.g. `AddExpenseUseCase`). All business logic happens here.

### Data Layer (`:data`)
- **Database**: Room database (`ExpenseAnalystDatabase`).
- **Entities**: Data objects annotated with `@Entity`.
- **DAOs**: Room `@Query` objects.
- **Mappers**: Convert `Entity ↔ Domain` model. 
- **Implementations**: Implement Repository interfaces (e.g. `ExpenseRepositoryImpl`).

### Core Layer (`:core`)
- Contains Dark Neon Tech Material 3 theme (`Theme.kt`, `Color.kt`).
- Reusable UI components (`ExpenseCard`, `AmountInput`).
- Utilities (`DateTimeUtil` for UTC to local timezone conversion, `CurrencyFormatter`).

### Feature Modules (`:feature:*`)
- Include Composable `*Screen.kt` files.
- Include ViewModels handling UI state with `StateFlow<*UiState>`.

## State Management Pattern
Always expose UI state via a **data class** (not sealed interface) with sensible defaults. The ViewModel holds a `StateFlow<*UiState>` using `stateIn(WhileSubscribed(5000))`. Screens collect with `collectAsStateWithLifecycle()`.

```kotlin
// Actual pattern — data class, never sealed interface
data class ExpenseListUiState(
    val groups: List<ExpenseGroup> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // ... other fields
)
```

Loading and error states are fields within the data class, not subclasses.

## Timezone & Data Offline Strategy
- **Timezone**: All timestamps must be stored as UTC epoch milliseconds (`Long`). Convert exactly to the device's timezone via `DateTimeUtil` for display.
- **Offline First**: All data is initially cached in Room. Exchange rates are cached. Do not enforce a server dependency for core functionality.

## Ensuring Code Health
- No feature should bypass the Domain layer Use Cases to access data formatting.
- Check `docs/ARCHITECTURE.md` for broader conceptual architecture diagrams and long-term KMP migration paths.
