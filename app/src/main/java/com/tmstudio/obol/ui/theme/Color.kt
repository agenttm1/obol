package com.tmstudio.obol.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Dizajn tokeni boja. Tamna tema je iz specifikacije (poglavlje 3); svijetla je
 * dodana nakon v1 i čuva ista značenja.
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
    /** Mint brenda — podloga loga, ista u obje teme. */
    val brand: Color,
    val switchThumb: Color,
    val switchThumbOff: Color,
    val switchTrackOffBorder: Color,
    /**
     * Koliko se boje servisa iz kataloga zatamnjuju (0 = nimalo). Katalog ima
     * boje za tamnu podlogu; na svijetloj bi žuta ili mint bile nečitljive.
     */
    val serviceInkDarkening: Float,
    val isDark: Boolean,
) {
    /**
     * Prigušena podloga ispod boje servisa ili značenja (pločica monograma,
     * ikona probnog perioda). Mockupi koriste ručno birane nijanse; ovo je
     * njihova aproksimacija preko prozirnosti.
     */
    fun tintedBackground(tint: Color): Color = tint.copy(alpha = TINT_ALPHA)

    /** Boja servisa prilagođena podlozi teme. */
    fun serviceInk(color: Color): Color =
        if (serviceInkDarkening == 0f) color else lerp(color, Color.Black, serviceInkDarkening)

    private companion object {
        const val TINT_ALPHA = 0.14f
    }
}

/** Boje kategorija za točkice i monograme; u svijetloj temi prolaze kroz [ObolColors.serviceInk]. */
@Immutable
data class CategoryColors(
    val entertainment: Color,
    val music: Color,
    val gaming: Color,
    val streamingOther: Color,
    val storage: Color,
    val tools: Color,
)

private val CategoryPalette = CategoryColors(
    entertainment = Color(0xFFFF8168),
    music = Color(0xFF2FD39B),
    gaming = Color(0xFF7FB0FF),
    streamingOther = Color(0xFFA99CF5),
    storage = Color(0xFFE8C766),
    tools = Color(0xFF8FB8FF),
)

private val BrandMint = Color(0xFF2FD39B)

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
    category = CategoryPalette,
    brand = BrandMint,
    switchThumb = Color(0xFFF3F5F7),
    switchThumbOff = Color(0xFFF3F5F7),
    switchTrackOffBorder = Color(0xFF1E252E),
    serviceInkDarkening = 0f,
    isDark = true,
)

/**
 * Svijetla tema. Svi parovi teksta i podloge imaju kontrast barem 4,5:1
 * (WCAG AA); mint, koral i amber su zato tamniji nego u tamnoj temi.
 */
val LightObolColors = ObolColors(
    bg = Color(0xFFF5F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEDF0F3),
    navBg = Color(0xFFFFFFFF),
    border = Color(0xFFE3E7EC),
    borderStrong = Color(0xFFB9C2CC),
    divider = Color(0xFFEBEEF2),
    textPrimary = Color(0xFF0C1014),
    textSecondary = Color(0xFF56626F),
    textTertiary = Color(0xFF7D8896),
    accent = Color(0xFF077A53),
    onAccent = Color(0xFFFFFFFF),
    accentSurface = Color(0xFFE4F6EE),
    accentBorder = Color(0xFFB4E3CF),
    accentText = Color(0xFF2E6551),
    warning = Color(0xFFB53A25),
    warningSurface = Color(0xFFFDEDE9),
    warningBorder = Color(0xFFF4C7BD),
    trial = Color(0xFF8F5F00),
    trialSurface = Color(0xFFFFF5E1),
    trialBorder = Color(0xFFEDD7A1),
    trialText = Color(0xFF7A5A1F),
    category = CategoryPalette,
    brand = BrandMint,
    switchThumb = Color(0xFFFFFFFF),
    switchThumbOff = Color(0xFF7D8896),
    switchTrackOffBorder = Color(0xFFB9C2CC),
    serviceInkDarkening = 0.35f,
    isDark = false,
)

val LocalObolColors = staticCompositionLocalOf { DarkObolColors }
