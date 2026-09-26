package com.expenseanalyst.domain.util

/**
 * Single source of truth for tag-name rules.
 *
 * Names are compared **ignoring case**. The `tags.name` unique index is case-sensitive (SQLite's
 * default BINARY collation), so before this, typing "subscription" beside an existing
 * "Subscription" silently created a second tag — which is how the real "Subscription" /
 * "subscriptions" near-duplicate pair appeared. Every create and rename goes through [sameName].
 */
object TagNames {

    const val MAX_LENGTH = 40

    /** Trimmed, internal whitespace collapsed, capped at [MAX_LENGTH]. Case is preserved. */
    fun normalize(raw: String): String =
        raw.trim().replace(WHITESPACE_RUN, " ").take(MAX_LENGTH)

    fun isValid(raw: String): Boolean = normalize(raw).isNotEmpty()

    fun sameName(a: String, b: String): Boolean =
        normalize(a).equals(normalize(b), ignoreCase = true)

    private val WHITESPACE_RUN = Regex("""\s+""")
}
