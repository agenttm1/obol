package com.tmstudio.obol.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmstudio.obol.R
import com.tmstudio.obol.domain.CalendarBilling
import com.tmstudio.obol.domain.CalendarMonth
import com.tmstudio.obol.ui.components.IconSquareButton
import com.tmstudio.obol.ui.components.ListDivider
import com.tmstudio.obol.ui.components.serviceColor
import com.tmstudio.obol.ui.format.ObolFormat
import com.tmstudio.obol.ui.theme.ObolTheme
import java.time.LocalDate
import java.time.YearMonth

private const val DAYS_IN_WEEK = 7

/** Najviše toliko točkica po danu; više naplata istog dana ne stane u ćeliju. */
private const val MAX_DOTS = 3

/** Ekran Kalendar prema `docs/mockups/Kalendar.html`. */
@Composable
fun CalendarScreen(
    onOpenSubscription: (Long) -> Unit,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val calendar = state.calendar ?: return
    val spacing = ObolTheme.spacing

    Column(Modifier.fillMaxSize()) {
        MonthHeader(
            month = calendar.month,
            onPrevious = viewModel::showPreviousMonth,
            onNext = viewModel::showNextMonth,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.x16,
                bottom = spacing.x24,
            ),
        ) {
            item(key = "summary") { MonthSummary(calendar) }
            item(key = "grid") {
                MonthGrid(calendar, state.today, Modifier.padding(top = spacing.x20))
            }
            item(key = "list-title") {
                val month = stringArrayResource(R.array.months_locative)[calendar.month.monthValue - 1]
                Text(
                    text = stringResource(R.string.calendar_billings_in, month).uppercase(ObolFormat.locale),
                    style = ObolTheme.typography.sectionLabel,
                    color = ObolTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.x20, bottom = spacing.x4),
                )
            }
            if (calendar.billings.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(if (state.isEmpty) R.string.calendar_empty else R.string.calendar_no_billings),
                        style = ObolTheme.typography.bodySoft,
                        color = ObolTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.x8),
                    )
                }
            }
            items(calendar.billings, key = { "${it.subscription.id}-${it.date}" }) { billing ->
                BillingRow(billing, onClick = { onOpenSubscription(billing.subscription.id) })
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    val spacing = ObolTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, top = spacing.screenTop),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconSquareButton(R.drawable.ic_chevron_left, stringResource(R.string.calendar_previous_month), onPrevious)
        Text(
            text = ObolFormat.monthYear(month).replaceFirstChar { it.titlecase(ObolFormat.locale) },
            style = ObolTheme.typography.monthTitle,
            color = ObolTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconSquareButton(R.drawable.ic_chevron_right, stringResource(R.string.calendar_next_month), onNext)
    }
}

@Composable
private fun MonthSummary(calendar: CalendarMonth) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    val shape = ObolTheme.shapes.cardLarge
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(spacing.borderWidth, colors.border, shape)
            .padding(vertical = spacing.cardPadding, horizontal = spacing.cardPaddingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.calendar_month_total), style = typography.caption, color = colors.textSecondary)
            Text(
                text = ObolFormat.money(calendar.totalCents),
                style = typography.amountSummary,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = spacing.x2),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(stringResource(R.string.calendar_billing_days), style = typography.caption, color = colors.textSecondary)
            Text(
                text = pluralStringResource(R.plurals.calendar_days, calendar.billingDays, calendar.billingDays),
                style = typography.amountMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = spacing.x2),
            )
        }
    }
}

@Composable
private fun MonthGrid(calendar: CalendarMonth, today: LocalDate, modifier: Modifier = Modifier) {
    val spacing = ObolTheme.spacing
    val month = calendar.month
    // Tjedan počinje ponedjeljkom; DayOfWeek.value je 1 za ponedjeljak.
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val weeks = cells.chunked(DAYS_IN_WEEK)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.x6)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.x6)) {
            stringArrayResource(R.array.weekdays_short).forEach { day ->
                Text(
                    text = day,
                    style = ObolTheme.typography.badge,
                    color = ObolTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(bottom = spacing.x2),
                )
            }
        }
        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.x6)) {
                (0 until DAYS_IN_WEEK).forEach { index ->
                    val date = week.getOrNull(index)
                    Box(Modifier.weight(1f)) {
                        if (date != null) DayCell(date, calendar.billingsOn(date), isToday = date == today)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, billings: List<CalendarBilling>, isToday: Boolean) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.iconTileSmall
    val hasBillings = billings.isNotEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.calendarCellHeight)
            .clip(shape)
            .background(if (hasBillings) colors.surfaceAlt else colors.surface)
            .then(if (isToday) Modifier.border(spacing.borderWidth, colors.textSecondary, shape) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = if (hasBillings) ObolTheme.typography.dayStrong else ObolTheme.typography.day,
            color = colors.textPrimary,
        )
        if (hasBillings) {
            Row(
                modifier = Modifier.padding(top = spacing.x2),
                horizontalArrangement = Arrangement.spacedBy(spacing.x2),
            ) {
                billings.take(MAX_DOTS).forEach { billing ->
                    Box(
                        Modifier
                            .size(spacing.calendarDot)
                            .clip(ObolTheme.shapes.dot)
                            .background(serviceColor(billing.subscription.colorHex))
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingRow(billing: CalendarBilling, onClick: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    val subscription = billing.subscription
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.minTouchTarget)
                .clickable(onClick = onClick)
                .padding(vertical = spacing.x8, horizontal = spacing.x2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.x10),
        ) {
            Text(
                text = stringResource(R.string.calendar_day_number, billing.date.dayOfMonth),
                style = typography.rowDay,
                color = colors.textSecondary,
                modifier = Modifier.width(spacing.dayColumn),
            )
            Box(
                Modifier
                    .size(spacing.dot)
                    .clip(ObolTheme.shapes.dot)
                    .background(serviceColor(subscription.colorHex))
            )
            Text(
                text = subscription.name,
                style = typography.rowTitle,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = ObolFormat.money(billing.priceCents, subscription.currency),
                style = typography.rowAmount,
                color = colors.textPrimary,
            )
        }
        ListDivider()
    }
}

