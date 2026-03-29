# Skill: New Feature Module (Android Multi-Module)

## Overview

Adding a new Gradle module (`feature:analytics`, `feature:budgets`, etc.) requires touching 6 locations. Missing any one of them causes build failure or silent runtime issues.

---

## Checklist

### 1. Create `feature/<name>/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.expenseanalyst.feature.<name>"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    // add any feature-specific deps
}
```

### 2. Create `feature/<name>/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
```

### 3. Register in `settings.gradle.kts`

```kotlin
include(":feature:<name>")
```

### 4. Declare dependency in `app/build.gradle.kts`

```kotlin
dependencies {
    implementation(project(":feature:<name>"))
    // …
}
```

### 5. Add route constant in `core/navigation/NavRoutes.kt`

```kotlin
const val MY_SCREEN = "my_screen"
// If the route has arguments:
fun myScreen(id: Long) = "my_screen/$id"
```

### 6. Register composable in `app/navigation/AppNavGraph.kt`

```kotlin
import com.expenseanalyst.feature.<name>.ui.MyScreen

composable(NavRoutes.MY_SCREEN) {
    MyScreen(
        onBack = { navController.popBackStack() }
    )
}
```

---

## Post-Creation Build Command

Always run after adding a new module:

```bash
./gradlew clean assembleDebug
```

KSP incremental processing is disabled (`ksp.incremental=false`). A full clean is required for the first build after any new module or file is added.

---

## Notes

- **Features never import from `:data`** — all data access goes through `:domain` repository interfaces
- Every `@HiltViewModel` in the new module needs `@AndroidEntryPoint` on its host Activity — which is already set on `MainActivity` in `:app`
- Module DI: create `feature/<name>/src/main/java/…/di/<Name>Module.kt` with `@Module @InstallIn(SingletonComponent::class)` if the module needs its own bindings
- `TopAppBar` in the new screen must set `windowInsets = WindowInsets(0,0,0,0)` to avoid double status-bar padding

---

## Example From Codebase

`feature/analytics` created this session. All 6 steps were applied.
