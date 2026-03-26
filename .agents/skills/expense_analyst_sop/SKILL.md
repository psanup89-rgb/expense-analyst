---
name: Expense Analyst Architecture & Build SOP
description: Standard Operating Procedure for developing the Expense Analyst Android app. Read this before contributing.
---

# Expense Analyst - Standard Operating Procedure

Use this skill first when taking over the Expense Analyst repo.

## Start Here
1. Read `HANDOFF.md` for the current repository state and next recommended work.
2. Read `AGENTS.md` for coding rules and module conventions.
3. Use the area-specific docs only as needed:
   - `../../../docs/ARCHITECTURE.md`
   - `../../../docs/DATA_MODELS.md`
   - `../../../docs/FEATURES.md`
   - `../../../docs/SETUP.md`
   - `../../../docs/TESTING.md`

## Current Repo State
- Phase 1A and 1B are complete.
- Phase 1C has a working core slice: cached rates, searchable currency picker, SAR default fallback, Settings home-currency control, stored conversion repair, and home-currency totals.
- Edit/detail, onboarding, notification parsing, and EMI flows are still incomplete.

## Hard Rules
- `:feature:*` modules may depend on `:domain` and `:core`, never `:data`.
- `:domain` stays pure Kotlin.
- Keep conversion math centralized in `domain/util/CurrencyConversion.kt`.
- Preserve the current dark neon visual language unless the user asks otherwise.

## Build Rules
- Java/JVM target stays at 21.
- Compile SDK stays at 35 unless the whole project is intentionally upgraded.
- After meaningful code changes, run `./gradlew assembleDebug`.
- If Android Studio shows only a generic KSP message, verify with CLI before assuming the code is broken.

## Related Skills
- Notification parsing SOP: `../expense_analyst_notification_parsing/SKILL.md`
- Testing SOP: `../expense_analyst_testing_sop/SKILL.md`
- Setup/release SOP: `../expense_analyst_setup_release/SKILL.md`
