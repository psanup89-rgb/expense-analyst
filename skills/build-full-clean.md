## skill: build-full-clean
agent: DataAgent, FeatureAgent, ParserAgent
created: 2026-03-29
last_used: 2026-03-29
tags: [build, ksp, hilt, room, gradle, debug]

# Build Verification (Always Full Clean)

## When to use this

Every time you want to verify the project compiles, especially after:
- Adding any new Kotlin file that uses `@Inject`, `@HiltViewModel`, `@AndroidEntryPoint`, `@Entity`, `@Dao`, or `@Module`
- Adding a new Room entity or DAO method
- Adding a new parser or service class
- Adding a new repository interface or implementation
- Modifying `ExpenseAnalystDatabase.kt`

Never use `assembleDebug` alone after adding new files — it will report success while leaving stale KSP-generated code that causes runtime crashes.

## What to do

**Standard build (verify only):**
```bash
./gradlew clean assembleDebug
```

**Build + install on connected device:**
```bash
./gradlew installDebug
```
(Does not need `clean` if `assembleDebug` already passed — the APK is already built.)

**Parser tests only (faster, after parser changes):**
```bash
./gradlew :feature:notification:testDebugUnitTest
```

**Module-specific KSP debug (when diagnosing KSP errors):**
```bash
./gradlew :data:kspDebugKotlin          # for DB/repository errors
./gradlew :feature:expenses:kspDebugKotlin   # for feature errors
./gradlew :feature:notification:kspDebugKotlin
```

## Example

After adding `BillEntity`, `BillDao`, `BillRepository`, and `BillRepositoryImpl`:
```
$ ./gradlew clean assembleDebug
> BUILD SUCCESSFUL in 6s
281 actionable tasks: 256 executed, 25 up-to-date
```

New schema file generated automatically:
```
data/schemas/com.expenseanalyst.data.local.ExpenseAnalystDatabase/10.json
```
Commit this file along with the migration code.

## Pitfalls

- **"Cannot resolve type X" from KSP** — almost always means: (a) a `clean` was not run after adding the new file, OR (b) there is a Kotlin compilation error in the new file preventing KSP from generating code. Run individual module KSP tasks to isolate.
- **KSP errors cascade** — one broken class causes Hilt to fail ALL bindings in that module. Don't be alarmed by 10 errors — fix the first one and rebuild.
- **Schema file not committed** — when a Room migration runs successfully, Room auto-generates a `<version>.json` schema file. Commit it. Reviewers and CI check schema history.
- **`installDebug` without a prior passing `assembleDebug`** — if the build hasn't been verified clean, the installed APK may contain stale code.

## Related skills

- `ksp-cross-module-smart-cast.md`
- `new-domain-entity.md`
