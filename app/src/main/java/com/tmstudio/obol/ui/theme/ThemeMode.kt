package com.tmstudio.obol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/** Odabir teme u Postavkama. Zadana je tamna, kao brend. */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
    ;

    @Composable
    fun isDark(): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> isSystemInDarkTheme()
    }
}
