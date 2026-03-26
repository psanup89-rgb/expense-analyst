---
name: Expense Analyst Setup & Release SOP
description: Use when setting up the local Android environment, troubleshooting builds, or preparing APK outputs for Expense Analyst.
---

# Expense Analyst Setup & Release SOP

Use this skill for environment setup, build troubleshooting, or release packaging.

## Read First
- `../../../docs/SETUP.md`
- `../../../AGENTS.md`

## Current Expectations
- Android Studio with JDK 21
- Compile SDK 35
- `./gradlew assembleDebug` is the baseline verification command

## Build Troubleshooting
1. Run `./gradlew clean assembleDebug` before chasing IDE-only failures.
2. If Android Studio shows a generic KSP error, inspect the first compiler error or run with `--stacktrace`.
3. After source changes, rebuild and relaunch the app rather than trusting a stale running session.

## Release Notes
- Release APK signing is still manual.
- Keep release instructions in `docs/SETUP.md` aligned with any future signing-config changes.
