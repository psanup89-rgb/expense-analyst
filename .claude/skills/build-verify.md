# Build Verification Skill

You are responsible for verifying that the Expense Analyst project builds correctly after making changes.

## CRITICAL: Always Use Clean Build

**Never run `./gradlew assembleDebug` alone to verify.** Always prefix with `clean`:

```bash
./gradlew clean assembleDebug
```

Without `clean`, Hilt/KSP's annotation processor may use stale outputs from a previous agent session, producing `KSP failed with exit code: PROCESSING_ERROR` even when the code is correct.

---

## KSP PROCESSING_ERROR Diagnosis

When you see:
```
KSP failed with exit code: PROCESSING_ERROR
```

This is **never** the actual error — it is a wrapper. Get the real error:

```bash
./gradlew assembleDebug --info 2>&1 | grep -E "error:|e: " | head -20
```

Or run the failing module in isolation with stacktrace:
```bash
./gradlew :data:kspDebugKotlin --stacktrace 2>&1 | tail -30
./gradlew :feature:settings:kspDebugKotlin --stacktrace 2>&1 | tail -30
./gradlew :feature:notification:kspDebugKotlin --stacktrace 2>&1 | tail -30
```

### Common root causes

| Symptom | Cause | Fix |
|---------|-------|-----|
| `[Hilt] could not be resolved` for a class in `:domain` | New `@Inject` class added to domain without domain `jar` built first | `./gradlew clean assembleDebug` |
| `[Hilt] ... is missing a binding` | A repository interface has no `@Binds` in any `@Module` | Add the binding to `data/di/RepositoryModule.kt` |
| `Unresolved reference: X` in generated Hilt code | KSP saw stale incremental symbols | `./gradlew clean assembleDebug` |
| Room schema export error | New entity added without schema path configured | Already configured; run clean build |

---

## Android Studio: Manual Fix When CLI Passes but Studio Fails

If `./gradlew clean assembleDebug` succeeds from CLI but Android Studio still shows errors:

1. **Build > Clean Project** (not just Build > Rebuild)
2. **File > Invalidate Caches…** → check "Clear file system cache and Local History" → **Invalidate and Restart**
3. After Studio restarts: **Build > Rebuild Project**

This clears Studio's own Gradle tooling cache which is separate from Gradle's build cache.

---

## Permanent Fixes Already Applied

The project already has these settings to minimize false KSP failures:

| Setting | Location | Value | Effect |
|---------|----------|-------|--------|
| `org.gradle.caching` | `gradle.properties` | `false` | Gradle build cache disabled — KSP always re-executes |
| `ksp.incremental` | `gradle.properties` | `false` | KSP full reprocessing every build — no stale symbol tables |
| `outputs.upToDateWhen { false }` | `build.gradle.kts` | (all `ksp*` tasks) | Gradle never marks KSP tasks up-to-date |

---

## Standard Build Verification Checklist

After any code change, run in order:

```bash
# 1. Clean + full build (catches Hilt/Room/KSP issues)
./gradlew clean assembleDebug

# 2. Unit tests
./gradlew testDebugUnitTest

# 3. Code quality (run before committing)
./gradlew ktlintCheck detekt
```

If any step fails:
1. Get the actual error (not just PROCESSING_ERROR) using `--info` or `--stacktrace`
2. Fix the root cause — do not retry the same failing command
3. If error is in Hilt-generated code, always try `clean` first before deeper investigation

---

## Build Time Expectations

| Task | Expected time |
|------|--------------|
| `./gradlew clean assembleDebug` | 15–25s |
| `./gradlew assembleDebug` (incremental) | 5–10s |
| `./gradlew testDebugUnitTest` | 10–20s |
