---
## skill: banner-dismiss-on-save
agent: FeatureAgent
created: 2026-03-29
last_used: 2026-03-29
tags: [notification, banner, navigation, pendingmanager, cross-module]

# In-App Notification Banner: Dismiss on Successful Save

## When to use this

Any time you add a new navigation path that leads to `AddExpenseScreen` (or any screen that saves an expense originating from a notification), you must ensure the in-app notification banner is dismissed after a successful save.

The banner is controlled by `PendingNotificationManager._pending` (in-memory `StateFlow`). It is only cleared by `consume()` or `dismiss()`. The in-app banner "Save" tap calls `consume()` before navigating, but any other path (e.g. tray notification tap, deep link) that goes directly to `AddExpenseScreen` bypasses this — the banner will reappear when the user returns to the Home screen.

## What to do

1. Add `fun dismissBanner()` to `MainViewModel` if it doesn't already exist:
   ```kotlin
   fun dismissBanner() {
       pendingManager.dismiss()
   }
   ```
   `MainViewModel` is in `:app` and can inject `PendingNotificationManager` (from `:feature:notification`).

2. In `AppNavGraph.kt`, for every `AddExpenseScreen` composable's `onSaved` callback, call `mainViewModel?.dismissBanner()` **before** `navController.popBackStack()`:
   ```kotlin
   AddExpenseScreen(
       onBack = { navController.popBackStack() },
       onSaved = {
           mainViewModel?.dismissBanner()
           navController.popBackStack()
       }
   )
   ```

3. Do NOT call `dismissBanner()` from `AddExpenseViewModel` — it lives in `:feature:expenses` and cannot import `PendingNotificationManager` from `:feature:notification` (Clean Architecture boundary violation).

## Example

Bug: User taps system tray notification → navigates to `AddExpenseScreen` → saves expense → presses back → in-app banner still shows.

Fix:
- `MainViewModel.kt`: `fun dismissBanner() { pendingManager.dismiss() }`
- `AppNavGraph.kt`: `onSaved = { mainViewModel?.dismissBanner(); navController.popBackStack() }`

## Pitfalls

- Calling `dismiss()` when `_pending` is already null is safe (no-op).
- Do not call `dismissBanner()` in `onBack` — only in `onSaved`. If the user navigates back without saving, the banner should remain visible.
- `mainViewModel` in `AppNavGraph` is nullable (it can be null in preview/test contexts). Always use `?.` safe call.

## Related skills

- `new-feature-module.md` — when adding entirely new screen modules
- `viewmodel-confirm-dialog.md` — pattern for destructive-action confirmation
