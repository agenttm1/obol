package com.tmstudio.obol.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import com.tmstudio.obol.R
import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.Category
import com.tmstudio.obol.data.db.entity.Subscription
import com.tmstudio.obol.ui.theme.ObolTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Mint pločica s prstenom — logo iz `IkonaB.html`. */
@Composable
fun ObolLogo(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(ObolTheme.spacing.logoSize)
            .clip(ObolTheme.shapes.logo)
            .background(ObolTheme.colors.accent)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_obol_ring),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Pločica sa slovom servisa u njegovoj boji. */
@Composable
fun MonogramTile(
    monogram: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = ObolTheme.spacing.monogramTile,
    shape: Shape = ObolTheme.shapes.iconTile,
    style: TextStyle = ObolTheme.typography.monogram,
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(ObolTheme.colors.tintedBackground(color)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = monogram, style = style, color = color)
    }
}

/** Tanka crta između redova liste. */
@Composable
fun ListDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(ObolTheme.spacing.borderWidth)
            .background(ObolTheme.colors.divider)
    )
}

/** Boja servisa iz `colorHex`; neispravna vrijednost pada na sekundarnu boju teksta. */
@Composable
fun serviceColor(colorHex: String): Color {
    val fallback = ObolTheme.colors.textSecondary
    return remember(colorHex, fallback) {
        runCatching { Color(colorHex.toColorInt()) }.getOrDefault(fallback)
    }
}

/** „danas", „sutra", „za 3 dana". */
@Composable
fun relativeDaysText(today: LocalDate, date: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date).toInt()
    return when (days) {
        0 -> stringResource(R.string.relative_today)
        1 -> stringResource(R.string.relative_tomorrow)
        else -> pluralStringResource(R.plurals.relative_in_days, days, days)
    }
}

/** „/ god." i slično za cikluse koji nisu mjesečni; null za mjesečni. */
@Composable
fun cycleSuffixText(subscription: Subscription): String? =
    if (subscription.cycle == BillingCycle.MONTHLY) null
    else cycleSuffix(subscription.cycle, subscription.cycleDays)

/** „/ mj", „/ god." … — za iznos uz bilo koji ciklus. */
@Composable
fun cycleSuffix(cycle: BillingCycle, cycleDays: Int?): String = when (cycle) {
    BillingCycle.MONTHLY -> stringResource(R.string.cycle_suffix_monthly)
    BillingCycle.WEEKLY -> stringResource(R.string.cycle_suffix_weekly)
    BillingCycle.QUARTERLY -> stringResource(R.string.cycle_suffix_quarterly)
    BillingCycle.SEMIANNUAL -> stringResource(R.string.cycle_suffix_semiannual)
    BillingCycle.YEARLY -> stringResource(R.string.cycle_suffix_yearly)
    BillingCycle.CUSTOM_DAYS -> stringResource(R.string.cycle_suffix_custom_days, cycleDays ?: 0)
}

/** Ime ciklusa: „Mjesečno", „Svakih 28 dana" … */
@Composable
fun cycleLabel(cycle: BillingCycle, cycleDays: Int?): String = when (cycle) {
    BillingCycle.WEEKLY -> stringResource(R.string.cycle_weekly)
    BillingCycle.MONTHLY -> stringResource(R.string.cycle_monthly)
    BillingCycle.QUARTERLY -> stringResource(R.string.cycle_quarterly)
    BillingCycle.SEMIANNUAL -> stringResource(R.string.cycle_semiannual)
    BillingCycle.YEARLY -> stringResource(R.string.cycle_yearly)
    BillingCycle.CUSTOM_DAYS -> cycleDays
        ?.let { pluralStringResource(R.plurals.cycle_every_n_days, it, it) }
        ?: stringResource(R.string.cycle_custom_days)
}

@Composable
fun categoryLabel(category: Category): String = stringResource(
    when (category) {
        Category.ENTERTAINMENT -> R.string.category_entertainment
        Category.MUSIC -> R.string.category_music
        Category.GAMING -> R.string.category_gaming
        Category.STORAGE -> R.string.category_storage
        Category.TOOLS -> R.string.category_tools
        Category.EDUCATION -> R.string.category_education
        Category.FITNESS -> R.string.category_fitness
        Category.NEWS -> R.string.category_news
        Category.OTHER -> R.string.category_other
    }
)
