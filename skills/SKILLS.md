# Skills Library

> The master index of all reusable agent skills for this project.
> Before starting any task, scan this file to find relevant skills.
> Never solve from scratch what a skill already solves.

---

## Recently Added

1. **Ktor JSON Without Serialization Plugin** (`ktor-json-parsing-without-serialization-plugin.md`) — `kotlinx.serialization` compiler plugin is absent from `:data` module. Using `body<T>()` crashes at runtime. Always use `bodyAsText()` + `JsonElement` tree API for new Ktor response types.
2. **Google Places API (New)** (`google-places-api-new.md`) — New API keys require `POST /v1/places:searchText` (not legacy `findplacefromtext`). API key in header, response is `places[].types`, no status field.
3. **Banner Dismiss on Save** (`banner-dismiss-on-save.md`) — When `AddExpenseScreen` is reached via a tray notification tap (not the in-app banner), `PendingNotificationManager._pending` must be cleared on successful save. Fix lives in `MainViewModel.dismissBanner()` called from `AppNavGraph.onSaved`. Cross-module boundary prevents doing this in `AddExpenseViewModel`.

---

## Index

| Skill | File | Agent | Tags | Last Used |
|-------|------|-------|------|-----------|
| Ktor JSON Without Serialization Plugin | `ktor-json-parsing-without-serialization-plugin.md` | DataAgent | ktor, json, serialization, android | 2026-03-30 |
| Google Places API (New) | `google-places-api-new.md` | DataAgent | google-places, api, merchant, category | 2026-03-30 |
| Banner Dismiss on Save | `banner-dismiss-on-save.md` | FeatureAgent | notification, banner, navigation, cross-module | 2026-03-29 |
| Transaction Direction Enum Extension | `transaction-direction-enum-extension.md` | ParserAgent, FeatureAgent | parser, enum, checklist | 2026-03-29 |
| Mubasher Parser Fingerprint | `parser-mubasher-fingerprint.md` | ParserAgent | parser, mubasher, fingerprint, regex | 2026-03-29 |
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
- **Mubasher Parser Fingerprint** (`parser-mubasher-fingerprint.md`) — Body fingerprint must require Mubasher-specific fields; never use generic `Amount:SAR` pattern alone
- **Transaction Direction Enum Extension** (`transaction-direction-enum-extension.md`) — Checklist for adding new `TransactionDirection` values
- **Parser Body Fingerprint Detection** (`parser-body-fingerprint.md`) — Write `canParse()` based on body content when sender is unknown/numeric

### FeatureAgent
- **Banner Dismiss on Save** (`banner-dismiss-on-save.md`) — Dismiss in-app banner via `MainViewModel.dismissBanner()` in `AppNavGraph.onSaved`
- **Transaction Direction Enum Extension** (`transaction-direction-enum-extension.md`) — Downstream file checklist for new direction values
- **Delegated Property Smart Cast** (`delegated-property-smart-cast.md`) — Capture nullable delegated state to local `val` before null-checking in Compose
- **New Feature Module** (`new-feature-module.md`) — 6-step checklist when creating a new Gradle module
- **Analytics Drill-Down Pattern** (`analytics-drilldown-pattern.md`) — Sealed filter + 5-way combine + ModalBottomSheet drill-down
- **Confirmation Dialog Pattern** (`viewmodel-confirm-dialog.md`) — Request/Confirm/Cancel state pattern for destructive action dialogs
- **KSP Cross-Module Smart Cast** (`ksp-cross-module-smart-cast.md`) — Fix smart cast compilation errors on domain model properties

### DataAgent
- **Ktor JSON Without Serialization Plugin** (`ktor-json-parsing-without-serialization-plugin.md`) — Use `bodyAsText()` + `JsonElement` for all new Ktor response types; `body<T>()` crashes at runtime
- **Google Places API (New)** (`google-places-api-new.md`) — POST endpoint, header-based auth, `places[].types` response structure
- **New Domain Entity End-to-End** (`new-domain-entity.md`) — Full checklist: domain model → Room migration → DI wiring
- **KSP Cross-Module Smart Cast** (`ksp-cross-module-smart-cast.md`) — Fix smart cast compilation errors

### Shared (All Agents)
- **Build Verification (Full Clean)** (`build-full-clean.md`) — Standard build commands, when to clean, how to isolate KSP errors

---

## Notes

- The `.claude/skills/` directory contains older Claude Code skill files (`bank-parser.md`, `build-verify.md`, `expense-architect.md`, etc.) — these are invocable via `/skill-name` in Claude Code CLI sessions. The files in `skills/` (this directory) are the agent memory system skills index and are not the same thing.
- Total skills in index: **13**
