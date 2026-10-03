package com.tmstudio.obol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private fun materialColors(c: ObolColors) = darkColorScheme(
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

/**
 * Obol ima samo tamnu temu u v1 i ne koristi dinamičke boje.
 * Tokeni se čitaju preko [ObolTheme.colors], [ObolTheme.typography],
 * [ObolTheme.spacing] i [ObolTheme.shapes]; Material shema je mapirana na iste
 * vrijednosti da M3 komponente izgledaju ispravno bez dodatnog posla.
 */
@Composable
fun ObolTheme(content: @Composable () -> Unit) {
    val colors = DarkObolColors
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
