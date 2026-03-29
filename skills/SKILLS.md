# Skills Library

> The master index of all reusable agent skills for this project.
> Before starting any task, scan this file to find relevant skills.
> Never solve from scratch what a skill already solves.

---

## Recently Added

1. **Delegated Property Smart Cast** (`delegated-property-smart-cast.md`) — Kotlin cannot smart-cast nullable fields on `by collectAsStateWithLifecycle()` delegated properties. Capture to local `val` first. Affects all Compose screens with conditional UI.
2. **New Feature Module** (`new-feature-module.md`) — 6-step checklist for creating a new Android Gradle module: `build.gradle.kts`, `AndroidManifest.xml`, `settings.gradle.kts`, `app/build.gradle.kts`, `NavRoutes.kt`, `AppNavGraph.kt`.
3. **Analytics Drill-Down Pattern** (`analytics-drilldown-pattern.md`) — Full pattern for tap-to-drill-down on dashboard cards/bars/rows using a `DrillDownFilter` sealed class, 5-way `combine()` in ViewModel, and `ModalBottomSheet` in Compose.

---

## Index

| Skill | File | Agent | Tags | Last Used |
|-------|------|-------|------|-----------|
| Delegated Property Smart Cast | `delegated-property-smart-cast.md` | FeatureAgent | kotlin, compose, smart-cast, state | 2026-03-29 |
| New Feature Module | `new-feature-module.md` | FeatureAgent | gradle, modules, architecture, hilt | 2026-03-29 |
| Analytics Drill-Down Pattern | `analytics-drilldown-pattern.md` | FeatureAgent | analytics, compose, bottomsheet, combine | 2026-03-29 |
| KSP Cross-Module Smart Cast | `ksp-cross-module-smart-cast.md` | DataAgent, FeatureAgent | kotlin, ksp, hilt, compilation | 2026-03-29 |
| New Domain Entity End-to-End | `new-domain-entity.md` | DataAgent | room, hilt, domain, migration, architecture | 2026-03-29 |
| Parser Body Fingerprint Detection | `parser-body-fingerprint.md` | ParserAgent | parser, sms, regex, detection | 2026-03-29 |
| Confirmation Dialog Pattern | `viewmodel-confirm-dialog.md` | FeatureAgent | compose, viewmodel, dialog, ux | 2026-03-29 |
| Build Verification (Full Clean) | `build-full-clean.md` | All agents | build, ksp, gradle, debug | 2026-03-29 |

---

## Skills by Agent Role

### ParserAgent
- **Parser Body Fingerprint Detection** (`parser-body-fingerprint.md`) — Write `canParse()` based on body content when sender is unknown/numeric

### FeatureAgent
- **Delegated Property Smart Cast** (`delegated-property-smart-cast.md`) — Capture nullable delegated state to local `val` before null-checking in Compose
- **New Feature Module** (`new-feature-module.md`) — 6-step checklist when creating a new Gradle module
- **Analytics Drill-Down Pattern** (`analytics-drilldown-pattern.md`) — Sealed filter + 5-way combine + ModalBottomSheet drill-down
- **Confirmation Dialog Pattern** (`viewmodel-confirm-dialog.md`) — Request/Confirm/Cancel state pattern for destructive action dialogs
- **KSP Cross-Module Smart Cast** (`ksp-cross-module-smart-cast.md`) — Fix smart cast compilation errors on domain model properties

### DataAgent
- **New Domain Entity End-to-End** (`new-domain-entity.md`) — Full checklist: domain model → Room migration → DI wiring
- **KSP Cross-Module Smart Cast** (`ksp-cross-module-smart-cast.md`) — Fix smart cast compilation errors
- **Build Verification** (`build-full-clean.md`) — Always `clean assembleDebug`; when to run module-specific KSP tasks

### Shared (All Agents)
- **Build Verification (Full Clean)** (`build-full-clean.md`) — Standard build commands, when to clean, how to isolate KSP errors

---

## Notes

- The `.claude/skills/` directory contains older Claude Code skill files (`bank-parser.md`, `build-verify.md`, `expense-architect.md`, etc.) — these are invocable via `/skill-name` in Claude Code CLI sessions. The files in `skills/` (this directory) are the agent memory system skills index and are not the same thing.
