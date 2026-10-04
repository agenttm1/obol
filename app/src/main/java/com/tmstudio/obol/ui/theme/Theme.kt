package com.tmstudio.obol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private fun materialColors(c: ObolColors) = if (c.isDark) darkScheme(c) else lightScheme(c)

private fun darkScheme(c: ObolColors) = darkColorScheme(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentSurface,
    onPrimaryContainer = c.accentText,
    secondary = c.textSecondary,
    onSecondary = c.bg,
    tertiary = c.trial,
    onTertiary = c.bg,
    tertiaryContainer = c.trialSurface,
    onTertiaryContainer = c.trialText,
    error = c.warning,
    onError = c.bg,
    errorContainer = c.warningSurface,
    onErrorContainer = c.warning,
    background = c.bg,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceAlt,
    onSurfaceVariant = c.textSecondary,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surfaceAlt,
    surfaceContainerHighest = c.surfaceAlt,
    surfaceContainerLow = c.navBg,
    surfaceContainerLowest = c.bg,
    outline = c.borderStrong,
    outlineVariant = c.divider,
    scrim = c.bg,
)

private fun lightScheme(c: ObolColors) = lightColorScheme(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.accentSurface,
    onPrimaryContainer = c.accentText,
    secondary = c.textSecondary,
    onSecondary = c.surface,
    tertiary = c.trial,
    onTertiary = c.surface,
    tertiaryContainer = c.trialSurface,
    onTertiaryContainer = c.trialText,
    error = c.warning,
    onError = c.surface,
    errorContainer = c.warningSurface,
    onErrorContainer = c.warning,
    background = c.bg,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceAlt,
    onSurfaceVariant = c.textSecondary,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surfaceAlt,
    surfaceContainerHighest = c.surfaceAlt,
    surfaceContainerLow = c.bg,
    surfaceContainerLowest = c.surface,
    outline = c.borderStrong,
    outlineVariant = c.divider,
    // Zatamnjenje iza dijaloga i donjih izbornika je tamno i u svijetloj temi.
    scrim = c.textPrimary,
)

/**
 * Tamna ili svijetla tema prema [darkTheme]; dinamičke boje se ne koriste.
 * Tokeni se čitaju preko [ObolTheme.colors], [ObolTheme.typography],
 * [ObolTheme.spacing] i [ObolTheme.shapes]; Material shema je mapirana na iste
 * vrijednosti da M3 komponente izgledaju ispravno bez dodatnog posla.
 */
@Composable
fun ObolTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkObolColors else LightObolColors
    val typography = ObolTypography()
    val shapes = ObolShapes()
    CompositionLocalProvider(
        LocalObolColors provides colors,
        LocalObolTypography provides typography,
        LocalObolSpacing provides ObolSpacing(),
        LocalObolShapes provides shapes,
    ) {
        MaterialTheme(
            colorScheme = materialColors(colors),
            typography = materialTypography(typography),
            shapes = Shapes(
                small = shapes.badge,
                medium = shapes.button,
                large = shapes.card,
                extraLarge = shapes.cardLarge,
            ),
            content = content,
        )
    }
}

object ObolTheme {
    val colors: ObolColors
        @Composable @ReadOnlyComposable get() = LocalObolColors.current
    val typography: ObolTypography
        @Composable @ReadOnlyComposable get() = LocalObolTypography.current
    val spacing: ObolSpacing
        @Composable @ReadOnlyComposable get() = LocalObolSpacing.current
    val shapes: ObolShapes
        @Composable @ReadOnlyComposable get() = LocalObolShapes.current
}
