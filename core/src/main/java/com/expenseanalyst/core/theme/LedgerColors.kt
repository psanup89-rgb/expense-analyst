package com.expenseanalyst.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Money colours that Material's scheme has no slot for. Screens used to hardcode these
 * (`Color(0xFFFF5555)` for spend in 19 places, an orange for review, a purple for payments),
 * which is why a re-theme had to touch every feature module. Read them through
 * [MaterialTheme.expenseColors] so dark and light each get a version with enough contrast.
 */
@Immutable
data class ExpenseColors(
    val spend: Color,
    val received: Color,
    val review: Color,
    val payment: Color,
    val transfer: Color,
    /** Background of a category badge disc. */
    val badge: Color,
    /** Ring around a category badge. */
    val hairline: Color
)

val DarkExpenseColors = ExpenseColors(
    spend = LedgerSpend,
    received = LedgerReceived,
    review = LedgerReview,
    payment = LedgerPayment,
    transfer = LedgerTransfer,
    badge = Color(0xFF1D1C18),
    hairline = LedgerHairline
)

val LightExpenseColors = ExpenseColors(
    spend = PaperSpend,
    received = PaperReceived,
    review = PaperReview,
    payment = PaperPayment,
    transfer = PaperTransfer,
    badge = PaperPanelHigh,
    hairline = PaperHairline
)

val LocalExpenseColors = staticCompositionLocalOf { DarkExpenseColors }

val MaterialTheme.expenseColors: ExpenseColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExpenseColors.current
