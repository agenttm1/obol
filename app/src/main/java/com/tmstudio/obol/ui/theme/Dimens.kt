package com.tmstudio.obol.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Razmaci i veličine iz specifikacije (poglavlje 3) i mockupa. */
@Immutable
data class ObolSpacing(
    // Opća skala razmaka; mockupi koriste i neparne vrijednosti, zaokružene su na ovu skalu.
    val x2: Dp = 2.dp,
    val x4: Dp = 4.dp,
    val x6: Dp = 6.dp,
    val x8: Dp = 8.dp,
    val x10: Dp = 10.dp,
    val x12: Dp = 12.dp,
    val x14: Dp = 14.dp,
    val x16: Dp = 16.dp,
    val x20: Dp = 20.dp,
    val x24: Dp = 24.dp,
    val x32: Dp = 32.dp,

    val screenHorizontal: Dp = 20.dp,
    val screenTop: Dp = 26.dp,
    val cardPadding: Dp = 14.dp,
    val cardPaddingLarge: Dp = 16.dp,
    val sectionGap: Dp = 22.dp,
    val listRowGap: Dp = 10.dp,
    /** Svaki element koji se tapka mora biti barem ovoliko visok. */
    val minTouchTarget: Dp = 44.dp,
    val borderWidth: Dp = 1.dp,
    val navPaddingTop: Dp = 7.dp,
    val navPaddingHorizontal: Dp = 8.dp,
    val navPaddingBottom: Dp = 15.dp,
    val navIconSize: Dp = 21.dp,
    val navIconLabelGap: Dp = 4.dp,

    val logoSize: Dp = 27.dp,
    val iconSize: Dp = 20.dp,
    val iconSizeSmall: Dp = 18.dp,
    val iconSizeXs: Dp = 14.dp,
    val monogramTile: Dp = 40.dp,
    val monogramTileSmall: Dp = 38.dp,
    val monogramTileLarge: Dp = 42.dp,
    val monogramTileXl: Dp = 56.dp,
    val secondaryButtonHeight: Dp = 48.dp,
    val calendarCellHeight: Dp = 42.dp,
    val calendarDot: Dp = 5.dp,
    /** Širina stupca s datumom u listi naplata. */
    val dayColumn: Dp = 30.dp,
    val searchFieldHeight: Dp = 50.dp,
    /** Redak s prekidačem (probni period, promo). */
    val toggleRowMinHeight: Dp = 56.dp,
    val settingRowMinHeight: Dp = 52.dp,
    val dashLength: Dp = 5.dp,
    val dashGap: Dp = 4.dp,
    val dot: Dp = 8.dp,
    /** Minimalna širina stupca iznosa, da se cijene u listi poravnaju. */
    val amountColumn: Dp = 62.dp,
    val primaryButtonHeight: Dp = 52.dp,
)

/** Zaobljenja: kartice 16–19dp, pločice ikona 12–13dp, gumbi 13–16dp, bedževi 9–11dp. */
@Immutable
data class ObolShapes(
    val card: RoundedCornerShape = RoundedCornerShape(16.dp),
    val cardLarge: RoundedCornerShape = RoundedCornerShape(18.dp),
    /** Velike kartice na ekranu Detalji. */
    val cardXl: RoundedCornerShape = RoundedCornerShape(19.dp),
    val buttonMedium: RoundedCornerShape = RoundedCornerShape(14.dp),
    /** Mala oznaka u obliku pilule, npr. „za 3 dana". */
    val pill: RoundedCornerShape = RoundedCornerShape(9.dp),
    val iconTile: RoundedCornerShape = RoundedCornerShape(13.dp),
    val iconTileSmall: RoundedCornerShape = RoundedCornerShape(12.dp),
    val button: RoundedCornerShape = RoundedCornerShape(15.dp),
    val buttonLarge: RoundedCornerShape = RoundedCornerShape(16.dp),
    /** Pločica servisa u mreži „Popularno". */
    val tile: RoundedCornerShape = RoundedCornerShape(17.dp),
    val chip: RoundedCornerShape = RoundedCornerShape(11.dp),
    val badge: RoundedCornerShape = RoundedCornerShape(10.dp),
    val logo: RoundedCornerShape = RoundedCornerShape(8.dp),
    val dot: Shape = CircleShape,
)

val LocalObolSpacing = staticCompositionLocalOf { ObolSpacing() }
val LocalObolShapes = staticCompositionLocalOf { ObolShapes() }
