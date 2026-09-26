package com.expenseanalyst.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LedgerDarkScheme = darkColorScheme(
    primary = LedgerAccent,
    onPrimary = LedgerOnAccent,
    primaryContainer = LedgerAccentContainer,
    onPrimaryContainer = LedgerAccent,
    secondary = LedgerIvory,
    onSecondary = LedgerGround,
    secondaryContainer = LedgerPanelHighest,
    onSecondaryContainer = LedgerIvory,
    tertiary = LedgerReceived,
    onTertiary = LedgerGround,
    tertiaryContainer = LedgerPanelHigh,
    onTertiaryContainer = LedgerReceived,
    error = LedgerSpend,
    onError = LedgerGround,
    errorContainer = Color(0xFF3A1E16),
    onErrorContainer = LedgerSpend,
    background = LedgerGround,
    onBackground = LedgerIvory,
    surface = LedgerGround,
    onSurface = LedgerIvory,
    surfaceVariant = LedgerPanelHigh,
    onSurfaceVariant = LedgerMuted,
    outline = LedgerHairline,
    outlineVariant = LedgerLine,
    inverseSurface = LedgerIvory,
    inverseOnSurface = LedgerGround,
    inversePrimary = PaperAccent,
    surfaceDim = LedgerGround,
    surfaceBright = LedgerPanelHighest,
    surfaceContainerLowest = LedgerGround,
    surfaceContainerLow = LedgerPanel,
    surfaceContainer = LedgerPanel,
    surfaceContainerHigh = LedgerPanelHigh,
    surfaceContainerHighest = LedgerPanelHighest
)

private val LedgerLightScheme = lightColorScheme(
    primary = PaperAccent,
    onPrimary = PaperOnAccent,
    primaryContainer = PaperAccentContainer,
    onPrimaryContainer = Color(0xFF2B3600),
    secondary = PaperInk,
    onSecondary = PaperGround,
    secondaryContainer = PaperPanelHighest,
    onSecondaryContainer = PaperInk,
    tertiary = PaperReceived,
    onTertiary = PaperPanel,
    tertiaryContainer = PaperPanelHigh,
    onTertiaryContainer = PaperReceived,
    error = PaperSpend,
    onError = PaperPanel,
    errorContainer = Color(0xFFF6DDD4),
    onErrorContainer = PaperSpend,
    background = PaperGround,
    onBackground = PaperInk,
    surface = PaperGround,
    onSurface = PaperInk,
    surfaceVariant = PaperPanelHigh,
    onSurfaceVariant = PaperMuted,
    outline = PaperHairline,
    outlineVariant = PaperLine,
    inverseSurface = PaperInk,
    inverseOnSurface = PaperGround,
    inversePrimary = LedgerAccent,
    surfaceDim = PaperPanelHighest,
    surfaceBright = PaperPanel,
    surfaceContainerLowest = PaperPanel,
    surfaceContainerLow = PaperSurface,
    surfaceContainer = PaperPanel,
    surfaceContainerHigh = PaperPanelHigh,
    surfaceContainerHighest = PaperPanelHighest
)

@Composable
fun ExpenseAnalystTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LedgerDarkScheme else LedgerLightScheme
    val expenseColors = if (darkTheme) DarkExpenseColors else LightExpenseColors

    CompositionLocalProvider(LocalExpenseColors provides expenseColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ExpenseAnalystTypography,
            shapes = ExpenseAnalystShapes,
            content = content
        )
    }
}
