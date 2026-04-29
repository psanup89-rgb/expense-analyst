# Biometric Authentication Skill

You are a specialist in adding biometric/device credential authentication to screens in the Expense Analyst app.

## Pattern: BiometricHelper + Screen Gate

The app uses `androidx.biometric:biometric:1.2.0-alpha05` with `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` authenticators. This combination allows fingerprint, face, PIN, pattern, or password — whatever the device has configured.

### Key Implementation Details

1. **BiometricHelper** is a singleton `object` in the feature module (not shared in `:core`). It wraps `BiometricManager` and `BiometricPrompt`.

2. **Activity cast**: `BiometricPrompt` requires a `FragmentActivity`. In Compose, get it via:
   ```kotlin
   val context = LocalContext.current
   val activity = context as? FragmentActivity
   ```
   This works because `MainActivity` extends `ComponentActivity` which extends `FragmentActivity`.

3. **Screen gate pattern**: The screen composable checks auth state and shows either:
   - Lock icon + "Retry" button (auth failed or not yet attempted)
   - Loading spinner (authenticated, data loading)
   - Actual content (authenticated, data loaded)

4. **LaunchedEffect for initial auth**: Trigger authentication once on screen entry:
   ```kotlin
   LaunchedEffect(Unit) {
       if (activity != null && BiometricHelper.canAuthenticate(context)) {
           BiometricHelper.authenticate(activity, onSuccess = { vm.onAuthenticated() }, onError = { })
       } else {
           vm.onAuthNotRequired()  // No lock screen set — skip auth
       }
   }
   ```

5. **ViewModel state**: Use two flags in UiState:
   - `isAuthenticated: Boolean = false` — gates content visibility
   - `authRequired: Boolean = true` — initial state

6. **Authenticator flags**: Do NOT set `setNegativeButtonText()` when using `DEVICE_CREDENTIAL` — they are mutually exclusive and will crash.

7. **Error handling**: Ignore `ERROR_USER_CANCELED` and `ERROR_NEGATIVE_BUTTON` — these are user-initiated dismissals, not failures.

## Dependency

In the feature module's `build.gradle.kts`:
```kotlin
implementation(libs.biometric)
```

In `gradle/libs.versions.toml`:
```toml
[versions]
biometric = "1.2.0-alpha05"

[libraries]
biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
```

## Reference Implementation

See `feature/budget/src/main/java/com/expenseanalyst/feature/budget/ui/`:
- `BiometricHelper.kt` — the helper object
- `BudgetScreen.kt` — the screen gate pattern
- `BudgetViewModel.kt` — `onAuthenticated()` / `onAuthNotRequired()` methods
