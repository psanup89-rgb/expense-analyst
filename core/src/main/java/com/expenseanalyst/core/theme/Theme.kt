package com.expenseanalyst.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkNeonColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = OnNeonGreen,
    primaryContainer = NeonGreenContainer,
    onPrimaryContainer = NeonGreen,
    secondary = NeonYellow,
    onSecondary = OnNeonYellow,
    secondaryContainer = NeonYellowContainer,
    onSecondaryContainer = NeonYellow,
    tertiary = NeonRedDim,
    onTertiary = NeonRedContainer,
    tertiaryContainer = NeonRedContainer,
    onTertiaryContainer = NeonRedDim,
    error = NeonRed,
    onError = NeonRedContainer,
    errorContainer = NeonRedContainer,
    onErrorContainer = NeonRedDim,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = OnSurface,
    inverseOnSurface = Surface,
    inversePrimary = NeonGreenContainer,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest
)

@Composable
fun ExpenseAnalystTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkNeonColorScheme,
        typography = ExpenseAnalystTypography,
        shapes = ExpenseAnalystShapes,
        content = content
    )
}
