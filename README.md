# Expense Analyst

Android expense tracker that auto-detects bank transactions from SMS and notifications.

## For Agents: Start Here

Read in this order — you can start coding after the first two:

| File | When to read |
|------|-------------|
| [`CLAUDE.md`](CLAUDE.md) | **Always** — conventions, architecture rules, build commands, critical gotchas |
| [`HANDOFF.md`](HANDOFF.md) | **Always** — current implementation state, last session summary, what's next |
| [`docs/DATA_MODELS.md`](docs/DATA_MODELS.md) | When touching Room DB, entities, or domain models |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | When touching module structure or layer boundaries |
| [`docs/NOTIFICATION_PARSING.md`](docs/NOTIFICATION_PARSING.md) | When adding/modifying bank SMS parsers |
| [`docs/FEATURES.md`](docs/FEATURES.md) | When implementing new features or checking Phase 2 backlog |
| [`docs/TESTING.md`](docs/TESTING.md) | When writing or running tests |
| [`docs/SETUP.md`](docs/SETUP.md) | When setting up the dev environment |

## Quick Facts

- **Stack**: Kotlin · Jetpack Compose · Room · Hilt · Ktor · Clean Architecture (MVVM)
- **Min SDK**: 26 · **Target SDK**: 35
- **DB version**: 9
- **Build**: `./gradlew clean assembleDebug` (always use `clean`)
- **Install**: `./gradlew installDebug`

## Project Status

Phase 1 + Phase 1.5 complete. See [`HANDOFF.md`](HANDOFF.md) for current state and Phase 2 backlog.
