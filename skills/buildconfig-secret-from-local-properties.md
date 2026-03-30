---
skill: buildconfig-secret-from-local-properties
agent: DataAgent
created: 2026-03-30
last_used: 2026-03-30
tags: [buildconfig, gradle, kotlin-dsl, secrets, local-properties, android]

# Embed API Key at Build Time via BuildConfig

## When to use this

When an API key or secret must be bundled into the APK at build time without appearing in version control. The key lives in `local.properties` (already gitignored by default in Android projects) and is injected as a `BuildConfig` field so that runtime code can reference it as a plain constant.

## What to do

**Step 1 — Add key to `local.properties`**
```
GOOGLE_PLACES_API_KEY=AIzaSy...
```

**Step 2 — Update the module's `build.gradle.kts`**

Add inside `android { defaultConfig { ... } }`:
```kotlin
buildFeatures { buildConfig = true }
```

Add BEFORE the `android {}` block to read the key:
```kotlin
val googlePlacesApiKey: String = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.readLines()
    ?.find { it.startsWith("GOOGLE_PLACES_API_KEY=") }
    ?.substringAfter("=")
    ?.trim()
    ?: ""
```

Then inside `android { defaultConfig { ... } }`:
```kotlin
buildConfigField("String", "GOOGLE_PLACES_API_KEY", "\"$googlePlacesApiKey\"")
```

**Step 3 — Reference in Kotlin code**
```kotlin
import com.expenseanalyst.data.BuildConfig

val key = BuildConfig.GOOGLE_PLACES_API_KEY
if (key.isBlank()) return null  // guard for dev environments without the key
```

**Step 4 — Run a clean build**
```bash
./gradlew clean assembleDebug
```
`BuildConfig` is generated during compilation; clean is required after adding `buildConfigField`.

## Example

`data/build.gradle.kts`:
```kotlin
val googlePlacesApiKey: String = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.readLines()
    ?.find { it.startsWith("GOOGLE_PLACES_API_KEY=") }
    ?.substringAfter("=")
    ?.trim()
    ?: ""

android {
    defaultConfig {
        buildConfigField("String", "GOOGLE_PLACES_API_KEY", "\"$googlePlacesApiKey\"")
    }
    buildFeatures { buildConfig = true }
}
```

`MerchantSearchRepositoryImpl.kt`:
```kotlin
if (BuildConfig.GOOGLE_PLACES_API_KEY.isBlank()) return null
```

## Pitfalls

**`java.util.Properties` does NOT work in Gradle Kotlin DSL.**
In `.kts` files, the identifier `java` resolves to the Java Gradle plugin project extension (the `java { }` configuration block), NOT the standard library `java.util` package. Attempting `java.util.Properties()` throws `Unresolved reference 'util'` at configuration time.

Use the line-based `readLines().find { it.startsWith("KEY=") }` approach instead.

**`buildFeatures { buildConfig = true }` is required.**
Android Gradle Plugin 8+ disables `BuildConfig` generation by default. Without this, the generated `BuildConfig` class won't contain custom fields (it may not be generated at all).

**The generated class is in the module's package**, not the app's. For a field in `:data`, import `com.expenseanalyst.data.BuildConfig`, not the app's.

## Related skills

- `build-full-clean.md` — always run `./gradlew clean assembleDebug` after build script changes
