package com.expenseanalyst.feature.notification.service

/**
 * Normalises the free text a user types into the notification's inline "Add note" reply
 * before it is persisted as [com.expenseanalyst.domain.model.Expense.description].
 *
 * Pure Kotlin with no Android dependencies so it can be unit tested directly.
 */
internal object NoteReplySanitizer {

    /**
     * Upper bound on a note added from the shade. The notification body and the
     * "Description" row on the expense detail screen are both single-line, and voice
     * dictation can produce runaway input. Editing in-app is not capped.
     */
    const val MAX_LENGTH = 200

    private val WHITESPACE = Regex("\\s+")

    /**
     * Collapses runs of whitespace (RemoteInput accepts multi-line and dictated input),
     * trims, and caps the length without splitting a surrogate pair.
     *
     * @return the cleaned note, or `null` when the reply carries no usable text.
     */
    fun sanitize(raw: CharSequence?): String? {
        val collapsed = raw?.toString().orEmpty().replace(WHITESPACE, " ").trim()
        if (collapsed.isEmpty()) return null
        if (collapsed.length <= MAX_LENGTH) return collapsed

        // Avoid cutting a surrogate pair in half, which would leave a broken character.
        val end = if (collapsed[MAX_LENGTH - 1].isHighSurrogate()) MAX_LENGTH - 1 else MAX_LENGTH
        return collapsed.substring(0, end)
    }
}
