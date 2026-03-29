package com.expenseanalyst.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val LightNeonColorScheme = lightColorScheme(
    primary = NeonGreenDim,
    onPrimary = LightOnNeonGreen,
    primaryContainer = LightNeonGreenContainer,
    onPrimaryContainer = OnNeonGreen,
    secondary = NeonYellowDim,
    onSecondary = OnNeonYellow,
    secondaryContainer = NeonYellowContainer,
    onSecondaryContainer = OnNeonYellow,
    tertiary = LightNeonRed,
    onTertiary = LightOnNeonGreen,
    tertiaryContainer = LightNeonRedContainer,
    onTertiaryContainer = LightNeonRed,
    error = LightNeonRed,
    onError = LightOnNeonGreen,
    errorContainer = LightNeonRedContainer,
    onErrorContainer = LightNeonRed,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainerHigh,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightOnSurface,
    inverseOnSurface = LightSurface,
    inversePrimary = NeonGreen,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest
)

@Composable
fun ExpenseAnalystTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkNeonColorScheme else LightNeonColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpenseAnalystTypography,
        shapes = ExpenseAnalystShapes,
        content = content
    )
}
