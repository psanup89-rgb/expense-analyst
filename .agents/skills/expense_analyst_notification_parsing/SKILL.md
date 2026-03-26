---
name: Expense Analyst Notification Parsing SOP
description: Use when implementing or reviewing the bank SMS and notification parsing workflow for Expense Analyst.
---

# Expense Analyst Notification Parsing SOP

Use this skill when working on notification/SMS parsing.

## Read First
- `../../../docs/NOTIFICATION_PARSING.md`
- `../../../docs/FEATURES.md`
- `../../../HANDOFF.md`

## Current State
- Notification parsing is still a planned Phase 1D workstream.
- The repo does not yet have the full parser/service stack described in the SOP doc.

## Workflow
1. Confirm the parser/service files you need actually exist before extending them.
2. Follow the sender-registry-parser flow described in `docs/NOTIFICATION_PARSING.md`.
3. Add parser tests with CSV fixtures before expanding support.
4. Keep all parsing local; never introduce server-side dependence for raw SMS content.

## Output Expectations
- New parser class
- Registry update
- Fixture-backed tests
- Handoff/doc update if supported banks or packages change
