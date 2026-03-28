## skill: parser-body-fingerprint
agent: ParserAgent
created: 2026-03-29
last_used: 2026-03-29
tags: [parser, sms, regex, detection, bank]

# Parser Detection via Body Fingerprint (Unknown Sender)

## When to use this

When adding a parser for a bank or payment platform whose SMS arrives from a **numeric shortcode** or **unpredictable sender ID** (e.g. `+966XXXXXXXX`, `12345`, `MUBSHR`, or a regular phone number). Sender-only detection will miss these.

Also use this when a bank's sender ID varies by region or carrier (common for Saudi/UAE banks when the user has a non-local SIM).

The pattern: detect by **body content** instead of (or in addition to) the sender string.

## What to do

**Step 1 — Identify a unique body fingerprint**

Look for a combination that is:
- Present in every SMS from this bank
- Absent in every SMS from other banks (avoid common words like "amount" or "SAR")
- Ideally a structured label like `Reason:`, `Service:`, `From:`, `Biller:`

**Step 2 — Write the fingerprint regex**

```kotlin
private val bodyFingerprintPattern = Regex(
    """(?i)(?:Reason\s*:.*(?:Bills?\s*Payment)|Amount\s*:\s*SAR\s*\d)""",
    RegexOption.DOT_MATCHES_ALL
)
```

Rules for a good fingerprint regex:
- Use `(?i)` for case-insensitive matching
- Use `DOT_MATCHES_ALL` if the fingerprint may span multiple lines
- Combine 2+ signals with `(?:A|B)` to reduce false positives
- Keep it narrow — prefer `Amount\s*:\s*SAR\s*\d` over just `SAR`

**Step 3 — Add a sender pattern as well (even if partial)**

If ANY part of the sender is known, include it. Body-fingerprint-only detection is a last resort:

```kotlin
private val senderPattern = Regex("""(?i)mubasher|mubshr""")
```

**Step 4 — Implement `canParse()` to try sender first, then body**

```kotlin
override fun canParse(sender: String, body: String): Boolean =
    senderPattern.containsMatchIn(sender) || bodyFingerprintPattern.containsMatchIn(body)
```

**Step 5 — Register the parser BEFORE GenericParser in ParserRegistry**

Body-fingerprint parsers are more specific than Generic. They must run first.

## Example

**MubasherParser** (`feature/notification/parser/MubasherParser.kt`):

Mubasher App sends from a numeric shortcode. Body always contains:
```
Reason:Bills Payment - Mubasher App
Bill Payment
From:6805
Amount:SAR 240
Service:ENBD PAYMENTS
```

Fingerprint regex:
```kotlin
private val bodyFingerprintPattern = Regex(
    """(?i)(?:Reason\s*:.*Bills?\s*Payment|Amount\s*:\s*SAR\s*\d)""",
    RegexOption.DOT_MATCHES_ALL
)
```

**AlRajhiParser** uses the same pattern for Al Rajhi SMS arriving from regular phone numbers (some users receive Al Rajhi SMS from a contact, not the bank shortcode).

**EmiratesNbdParser** uses it for ENBD notifications that arrive as push notifications from the banking app rather than SMS.

## Pitfalls

- **Overly broad fingerprint** — `Amount:SAR` matches nearly any Saudi bank. Add a second discriminating signal.
- **Missing `DOT_MATCHES_ALL`** — if the fingerprint spans a line break, the regex will never match without this flag.
- **Putting body-fingerprint parser AFTER GenericParser** — GenericParser always returns true from `canParse()`, so the body-fingerprint parser will never run. Always register before Generic.
- **Not testing against edge cases** — test the fingerprint regex against at least 5 sample SMS from the target bank AND 5 samples from other banks (especially GenericParser candidates) to verify no false positives.

## Related skills

- `add-bank-parser.md` (if it exists; otherwise see `docs/NOTIFICATION_PARSING.md`)
