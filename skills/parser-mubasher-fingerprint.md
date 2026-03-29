---
## skill: parser-mubasher-fingerprint
agent: ParserAgent
created: 2026-03-29
last_used: 2026-03-29
tags: [parser, mubasher, fingerprint, canparse, regex]

# Mubasher Parser: Body Fingerprint Must Require Service-Specific Fields

## When to use this

When modifying `MubasherParser.canParse()` or any body-fingerprint-based parser that handles Saudi bank transactions alongside Al Rajhi, Alinma, D360, or StcBank. The Mubasher parser uses body-fingerprint detection (no reliable sender ID) and competes with other Saudi bank parsers that also use `Amount:SAR` style formatting.

## What to do

The body fingerprint for `MubasherParser` must **always** require at least one Mubasher-specific field in addition to (or instead of) any generic amount pattern.

**Correct pattern** (as of 2026-03-29):
```kotlin
private val bodyFingerprintPattern = Regex(
    """(?i)(?:Reason\s*:.*(?:Bills?\s*Payment|Bill\s*Transfer|Payment\s*Transfer)|(?:Biller|Service)\s*:)""",
    RegexOption.DOT_MATCHES_ALL
)
```

This matches:
- `Reason:Bills Payment - Mubasher App` style headers, OR
- Any body containing `Biller:` or `Service:` fields (Mubasher-specific)

**Never use** a pattern like `Amount\s*:\s*SAR\s*\d` as the sole or fallback branch — it matches ANY Saudi bank SMS that uses the `Amount:SAR N` format (Al Rajhi internal transfers, Alinma alerts, etc.).

## Example

**Bug**: `MubasherParser.bodyFingerprintPattern` had:
```
(?:Reason\s*:.*(?:Bills?\s*Payment|...)|Amount\s*:\s*SAR\s*\d)
```
An Al Rajhi "Credit Transfer Internal" SMS with `Amount:SAR 5000` hit the second branch → incorrectly parsed as Mubasher PAYMENT instead of Al Rajhi TRANSFER.

**Fix**: Replace `Amount\s*:\s*SAR\s*\d` with `(?:Biller|Service)\s*:`.

Real Mubasher SMS always contain `Biller:` or `Service:` fields. The generic amount pattern did not.

## Pitfalls

- The Mubasher sender ID (`mub(?:asher|shr)?`) is reliable when present, but Mubasher notifications sometimes arrive from Al Rajhi's sender ID (`74100`) — hence body fingerprinting is needed.
- A broad body fingerprint causes Mubasher to fire as a "silent fallback" for any SAR transaction that upstream parsers return `null` for — particularly Al Rajhi transfers, which have no debit/credit keywords.
- If you add a new Mubasher SMS format, ensure any new fingerprint branch still requires a Mubasher-specific structural field.

## Related skills

- `parser-body-fingerprint.md` — general SOP for body-fingerprint based `canParse()`
- `transaction-direction-enum-extension.md` — if adding a new direction type
