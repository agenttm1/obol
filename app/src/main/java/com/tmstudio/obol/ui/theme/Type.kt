package com.tmstudio.obol.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tmstudio.obol.R

// Manrope je varijabilni font (os wght), jedna .ttf datoteka pokriva sve težine.
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Manrope = FontFamily(
    manrope(FontWeight.Normal),
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

/** Tabularne brojke — obavezne za sve iznose novca, inače se stupci ne poravnavaju. */
fun TextStyle.tabularNums(): TextStyle = copy(fontFeatureSettings = "tnum")

private fun style(
    size: Float,
    weight: FontWeight,
    letterSpacingEm: Float = 0f,
    lineHeight: Float = size * 1.3f,
) = TextStyle(
    fontFamily = Manrope,
    fontSize = size.sp,
    fontWeight = weight,
    letterSpacing = letterSpacingEm.em,
    lineHeight = lineHeight.sp,
)

/** Tipografija iz specifikacije (poglavlje 3) i mockupa. */
@Immutable
data class ObolTypography(
    /** Ukupni mjesečni iznos. */
    val hero: TextStyle = style(50f, FontWeight.ExtraBold, -0.035f, lineHeight = 50f).tabularNums(),
    /** Znak valute uz [hero]. */
    val heroCurrency: TextStyle = style(21f, FontWeight.Bold),
    val screenTitle: TextStyle = style(22f, FontWeight.ExtraBold, -0.02f),
    /** Ime aplikacije u zaglavlju Pregleda. */
    val brandTitle: TextStyle = style(22f, FontWeight.ExtraBold, -0.03f),
    /** Naslov ekrana s povratnom strelicom. */
    val subScreenTitle: TextStyle = style(19f, FontWeight.ExtraBold, -0.02f),
    /** Ime servisa na ekranu Detalji. */
    val detailTitle: TextStyle = style(21f, FontWeight.ExtraBold, -0.02f),
    /** Cijena na ekranu Detalji. */
    val amountLarge: TextStyle = style(30f, FontWeight.ExtraBold, -0.03f).tabularNums(),
    val amountMedium: TextStyle = style(16f, FontWeight.Bold).tabularNums(),
    val amountSmall: TextStyle = style(13f, FontWeight.Bold).tabularNums(),
    /** Zbroj mjeseca u Kalendaru. */
    val amountSummary: TextStyle = style(24f, FontWeight.ExtraBold, -0.03f).tabularNums(),
    /** Naslov mjeseca u Kalendaru. */
    val monthTitle: TextStyle = style(18f, FontWeight.ExtraBold, -0.01f),
    /** Broj dana u mreži kalendara; dan s naplatom koristi [dayStrong]. */
    val day: TextStyle = style(13f, FontWeight.SemiBold).tabularNums(),
    val dayStrong: TextStyle = style(13f, FontWeight.ExtraBold).tabularNums(),
    /** Datum na početku retka liste, npr. „04.". */
    val rowDay: TextStyle = style(12f, FontWeight.ExtraBold).tabularNums(),
    /** Naslov skupine, npr. „POPULARNO" — piše se velikim slovima na mjestu poziva. */
    val sectionLabel: TextStyle = style(13f, FontWeight.Bold, 0.02f),
    /** Naslov skupine postavki, npr. „PODSJETNICI" — piše se velikim slovima na mjestu poziva. */
    val groupLabel: TextStyle = style(12f, FontWeight.ExtraBold, 0.06f),
    /** Vrijednost u retku postavke, npr. „3 dana". */
    val settingValue: TextStyle = style(13f, FontWeight.Bold),
    /** Sitni tekst na dnu ekrana, npr. verzija aplikacije. */
    val footnote: TextStyle = style(11.5f, FontWeight.SemiBold),
    /** Oznaka polja, npr. „CIJENA" — piše se velikim slovima na mjestu poziva. */
    val fieldLabel: TextStyle = style(11f, FontWeight.Bold, 0.04f),
    /** Tekst koji korisnik upisuje. */
    val input: TextStyle = style(15f, FontWeight.Medium),
    /** Veliki iznos u polju za unos. */
    val inputAmount: TextStyle = style(20f, FontWeight.ExtraBold).tabularNums(),
    /** Znak valute ili vrijednost odabira uz polje. */
    val inputSuffix: TextStyle = style(15f, FontWeight.Bold),
    /** Istaknuta vrijednost u retku postavke. */
    val valueStrong: TextStyle = style(13f, FontWeight.ExtraBold).tabularNums(),
    val button: TextStyle = style(15f, FontWeight.ExtraBold),
    /** Mali sekundarni gumb, npr. „Promijeni". */
    val chip: TextStyle = style(12.5f, FontWeight.Bold),
    val captionSmall: TextStyle = style(11.5f, FontWeight.Medium, lineHeight = 11.5f * 1.45f),
    val cardTitle: TextStyle = style(15f, FontWeight.Bold),
    val sectionTitle: TextStyle = style(14f, FontWeight.Bold),
    val bodyLarge: TextStyle = style(14f, FontWeight.SemiBold),
    val body: TextStyle = style(13f, FontWeight.SemiBold),
    val bodySoft: TextStyle = style(13f, FontWeight.Medium),
    val bodyRegular: TextStyle = style(13f, FontWeight.Normal),
    val caption: TextStyle = style(12f, FontWeight.SemiBold),
    val captionSoft: TextStyle = style(12f, FontWeight.Medium),
    val captionStrong: TextStyle = style(11.5f, FontWeight.Bold),
    /** Tekstualna poveznica, npr. „Kalendar" uz naslov sekcije. */
    val link: TextStyle = style(12f, FontWeight.Bold),
    /** Velika oznaka — tekst se piše VELIKIM SLOVIMA na mjestu poziva. */
    val overline: TextStyle = style(11f, FontWeight.Bold, 0.1f),
    val navLabel: TextStyle = style(10.5f, FontWeight.Bold),
    /** Sitna oznaka uz iznos, npr. „prva naplata". */
    val badge: TextStyle = style(10.5f, FontWeight.Bold),
    val badgeSoft: TextStyle = style(10.5f, FontWeight.SemiBold),
    /** Slovo u pločici servisa. */
    val monogram: TextStyle = style(17f, FontWeight.ExtraBold),
    val monogramSmall: TextStyle = style(16f, FontWeight.ExtraBold),
    val monogramLarge: TextStyle = style(18f, FontWeight.ExtraBold),
    val monogramXl: TextStyle = style(24f, FontWeight.ExtraBold),
    /** Ime u kompaktnom redu liste. */
    val rowTitle: TextStyle = style(13.5f, FontWeight.SemiBold),
    /** Iznos u redu liste ili kartici. */
    val amount: TextStyle = style(14f, FontWeight.Bold).tabularNums(),
    /** Iznos u kompaktnom redu liste. */
    val rowAmount: TextStyle = style(13.5f, FontWeight.Bold).tabularNums(),
)

val LocalObolTypography = staticCompositionLocalOf { ObolTypography() }

/** Material slotovi mapirani na iste stilove, da i M3 komponente koriste Manrope. */
internal fun materialTypography(t: ObolTypography): Typography {
    val base = Typography()
    return Typography(
        displayLarge = t.hero,
        displayMedium = base.displayMedium.copy(fontFamily = Manrope),
        displaySmall = base.displaySmall.copy(fontFamily = Manrope),
        headlineLarge = base.headlineLarge.copy(fontFamily = Manrope),
        headlineMedium = base.headlineMedium.copy(fontFamily = Manrope),
        headlineSmall = t.screenTitle,
        titleLarge = t.cardTitle,
        titleMedium = t.sectionTitle,
        titleSmall = t.body,
        bodyLarge = t.bodyLarge,
        bodyMedium = t.body,
        bodySmall = t.caption,
        labelLarge = t.bodyLarge,
        labelMedium = t.caption,
        labelSmall = t.overline,
    )
}
