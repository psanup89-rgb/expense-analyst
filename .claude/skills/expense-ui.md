# Expense UI Skill

You are a specialist in Jetpack Compose UI work for the **Expense Analyst** Android app — screens, components, navigation, and theming.

## Context

The app uses **Jetpack Compose + Material 3** with a multi-module architecture. All UI code lives in `feature/` modules and the `:core` module.

```
core/src/main/com/expenseanalyst/core/
  theme/          ← ExpenseAnalystTheme, Color, Typography, Shape
  components/     ← Reusable composables (AmountText, CategoryChip, etc.)
  navigation/     → NavRoutes.kt (route constants)

feature/<name>/src/main/com/expenseanalyst/feature/<name>/
  ui/
    <Name>Screen.kt      ← Top-level composable, hoisted state
    <Name>ViewModel.kt   ← @HiltViewModel, exposes StateFlow<UiState>
    <Name>UiState.kt     ← Sealed class or data class
    components/          ← Screen-specific composables
```

## Screen Anatomy

Every screen follows this pattern:

```kotlin
// *UiState.kt
data class ExpenseListUiState(
    val expenses: List<ExpenseGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// *ViewModel.kt
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()
    // ...
}

// *Screen.kt
@Composable
fun ExpenseListScreen(
    onAddExpense: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseListContent(uiState, onAddExpense, onExpenseClick)
}

@Composable
private fun ExpenseListContent(
    uiState: ExpenseListUiState,
    onAddExpense: () -> Unit,
    onExpenseClick: (Long) -> Unit
) { /* ... */ }
```

## Navigation

Routes defined in `core/navigation/NavRoutes.kt`:
```kotlin
object NavRoutes {
    const val EXPENSE_LIST = "expense_list"
    const val ADD_EXPENSE = "add_expense"
    const val EDIT_EXPENSE = "edit_expense/{expenseId}"
    const val EXPENSE_DETAIL = "expense_detail/{expenseId}"
    const val EMI_LIST = "emi_list"
    const val EMI_DETAIL = "emi_detail/{emiGroupId}"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
}
```

NavGraph registered in `app/navigation/AppNavGraph.kt`. Bottom nav in `app/ui/MainBottomNav.kt`.

**Bottom nav tabs**: Home (ExpenseList), EMI, Analytics (Phase 2), Settings

## Material 3 Theming (Dark Neon Tech)

```kotlin
// Colors follow Material 3 dynamic color system, but specialized for a Dark Neon Tech 'Lumina Ledger' look.
// Backgrounds are deeply dark (#121212, #1E1E1E)
// Brand accents use Neon Lime Green (#CCFF00) and Neon Yellow (#FFD600)
// Always use MaterialTheme.colorScheme.* — never hardcode colors

MaterialTheme.colorScheme.primary        // Neon Lime Green brand color
MaterialTheme.colorScheme.secondary      // Neon Yellow accents
MaterialTheme.colorScheme.surface        // Dark Gray card backgrounds (#1E1E1E)
MaterialTheme.colorScheme.onSurface      // Light Gray/White text on cards
MaterialTheme.colorScheme.surfaceVariant // Slightly lighter dark background
```
Note: Ensure components utilize soft glowing drop-shadows where applicable to emulate the neon aesthetic.

## Key Reusable Components (to build in `:core`)

| Component | Usage |
|-----------|-------|
| `AmountText` | Displays amount with currency symbol, optionally dual-currency |
| `CategoryIcon` | Icon + label composable for expense categories |
| `PaymentMethodChip` | Selectable chip for payment method |
| `ExpenseCard` | List item showing amount, merchant, category, date |
| `EmptyStateView` | Illustration + message + optional CTA |
| `LoadingSkeleton` | Shimmer placeholder for loading states |
| `ErrorBanner` | Snackbar-like error display |

## Key Screens

### Home / Expense List
- `LazyColumn` with sticky date headers ("Today", "Yesterday", "March 20, 2026")
- Monthly total card at top (collapsible)
- Filter chips (category, payment method, date range)
- FAB: add expense
- Empty state with illustration when no expenses

### Add/Edit Expense
- Large amount input field at top
- Currency selector chip (inline, opens searchable bottom sheet)
- Category grid (LazyVerticalGrid, 4 columns, icon + label)
- Payment method chips (horizontal LazyRow)
- Date picker (DatePickerDialog)
- Description, merchant, notes text fields
- Save button (disabled until required fields filled)

### EMI Conversion Sheet
- `ModalBottomSheet`
- Fields: number of installments, optional interest rate
- Live preview: shows monthly installment amount
- Confirm button creates EMI group

### Notification Confirmation Banner
- Animated slide-in from top (or as snackbar)
- Shows: "Rs 450 at Swiggy detected"
- Buttons: "Save" (pre-fills add expense) and "Dismiss"
- Auto-dismisses after 30 seconds

### Currency Picker
- `ModalBottomSheet` with search bar
- `LazyColumn` of currencies: flag emoji + code + name
- Keyboard visible by default

## Accessibility Checklist
- All images/icons: `contentDescription`
- Touch targets: 48dp minimum
- Color contrast: 4.5:1 minimum (Material 3 ensures this)
- Semantic roles: `Modifier.semantics { }`
- Screen reader: test with TalkBack

## Preview Conventions

```kotlin
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ExpenseCardPreview() {
    ExpenseAnalystTheme {
        ExpenseCard(/* sample data */)
    }
}
```

Always provide both light and dark previews for all composables.

## Performance Tips
- Use `key()` in `LazyColumn` items for stable IDs
- `collectAsStateWithLifecycle()` instead of `collectAsState()`
- Avoid reading state in lambdas that recompose frequently
- Use `derivedStateOf` for computed state
- `Modifier.animateItemPlacement()` for list insertions/removals
