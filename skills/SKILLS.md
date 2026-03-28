# Skills Library

> The master index of all reusable agent skills for this project.
> Before starting any task, scan this file to find relevant skills.
> Never solve from scratch what a skill already solves.

---

## Recently Added

1. **KSP Cross-Module Smart Cast** (`ksp-cross-module-smart-cast.md`) — Fixes "smart cast impossible" Kotlin compilation errors when accessing nullable properties from domain models in feature modules. Non-obvious root cause of cascading KSP/Hilt failures.
2. **New Domain Entity End-to-End** (`new-domain-entity.md`) — Full 8-step checklist for adding a new persisted concept: domain model → Room entity → DAO → migration → repository → DI. Missing any step causes build failures.
3. **Parser Body Fingerprint Detection** (`parser-body-fingerprint.md`) — How to write a parser that detects bank SMS by body content when the sender is a numeric shortcode or unpredictable. Used by MubasherParser, AlRajhiParser, EmiratesNbdParser.

---

## Index

| Skill | File | Agent | Tags | Last Used |
|-------|------|-------|------|-----------|
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
