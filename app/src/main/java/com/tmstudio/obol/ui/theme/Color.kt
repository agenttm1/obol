package com.tmstudio.obol.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Dizajn tokeni boja iz specifikacije (poglavlje 3). Tamna tema je jedina u v1.
 *
 * Značenje boja se ne krši: mint (accent) je ušteda i potvrda, koral (warning)
 * poskupljenje i opasnost, amber (trial) probni period. Nikad dekorativno.
 */
@Immutable
data class ObolColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val navBg: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSurface: Color,
    val accentBorder: Color,
    val accentText: Color,
    val warning: Color,
    val warningSurface: Color,
    val warningBorder: Color,
    val trial: Color,
    val trialSurface: Color,
    val trialBorder: Color,
    val trialText: Color,
    val category: CategoryColors,
) {
    /**
     * Prigušena podloga ispod boje servisa ili značenja (pločica monograma,
     * ikona probnog perioda). Mockupi koriste ručno birane nijanse; ovo je
     * njihova aproksimacija preko prozirnosti.
     */
    fun tintedBackground(tint: Color): Color = tint.copy(alpha = TINT_ALPHA)

    private companion object {
        const val TINT_ALPHA = 0.14f
    }
}

/** Boje kategorija za točkice i monograme. */
@Immutable
data class CategoryColors(
    val entertainment: Color,
    val music: Color,
    val gaming: Color,
    val streamingOther: Color,
    val storage: Color,
    val tools: Color,
)

val DarkObolColors = ObolColors(
    bg = Color(0xFF0C1014),
    surface = Color(0xFF161C23),
    surfaceAlt = Color(0xFF1E252E),
    navBg = Color(0xFF0F141A),
    border = Color(0xFF1E252E),
    borderStrong = Color(0xFF37424F),
    divider = Color(0xFF1A212A),
    textPrimary = Color(0xFFF3F5F7),
    textSecondary = Color(0xFF97A3B2),
    textTertiary = Color(0xFF5E6B7A),
    accent = Color(0xFF2FD39B),
    onAccent = Color(0xFF06170F),
    accentSurface = Color(0xFF11241C),
    accentBorder = Color(0xFF1F4436),
    accentText = Color(0xFF93C6B2),
    warning = Color(0xFFFF8168),
    warningSurface = Color(0xFF1C1316),
    warningBorder = Color(0xFF3A2228),
    trial = Color(0xFFFFC75A),
    trialSurface = Color(0xFF1F1810),
    trialBorder = Color(0xFF3D3118),
    trialText = Color(0xFFE0C184),
    category = CategoryColors(
        entertainment = Color(0xFFFF8168),
        music = Color(0xFF2FD39B),
        gaming = Color(0xFF7FB0FF),
        streamingOther = Color(0xFFA99CF5),
        storage = Color(0xFFE8C766),
        tools = Color(0xFF8FB8FF),
    ),
)

val LocalObolColors = staticCompositionLocalOf { DarkObolColors }
