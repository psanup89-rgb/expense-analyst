---
name: Expense Analyst Testing SOP
description: Use when adding tests, expanding coverage, or running regression checks for Expense Analyst.
---

# Expense Analyst Testing SOP

Use this skill for test planning or implementation.

## Read First
- `../../../docs/TESTING.md`
- `../../../HANDOFF.md`

## Current State
- Test dependencies are configured.
- Test source sets are still mostly absent in the current repo.
- Manual regression is still the main safety net.

## Priority Order
1. Domain and mapper unit tests
2. Data repository / DAO tests
3. Multi-currency regression coverage
4. Compose UI tests for Home, Add Expense, and Settings
5. Notification parser parameterized tests once Phase 1D starts

## Manual Smoke Minimum
- Add a home-currency expense
- Add a foreign-currency expense
- Change home currency in Settings
- Verify totals and row-level home values persist after relaunch
