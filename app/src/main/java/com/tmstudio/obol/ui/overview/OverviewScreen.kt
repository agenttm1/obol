package com.tmstudio.obol.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmstudio.obol.R
import com.tmstudio.obol.domain.Overview
import com.tmstudio.obol.domain.SubscriptionSummary
import com.tmstudio.obol.domain.UpcomingEvent
import com.tmstudio.obol.ui.components.ListDivider
import com.tmstudio.obol.ui.components.ServiceMark
import com.tmstudio.obol.ui.components.ObolLogo
import com.tmstudio.obol.ui.components.cycleSuffixText
import com.tmstudio.obol.ui.components.relativeDaysText
import com.tmstudio.obol.ui.components.serviceColor
import com.tmstudio.obol.ui.format.ObolFormat
import com.tmstudio.obol.ui.theme.ObolTheme
import java.time.LocalDate
import java.time.YearMonth

/** Ekran Pregled prema `docs/mockups/Main.html`. Kartica ušteda je izostavljena u v1. */
@Composable
fun OverviewScreen(
    onAddSubscription: () -> Unit,
    onOpenSubscription: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OverviewViewModel = viewModel(factory = OverviewViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OverviewContent(state, onAddSubscription, onOpenSubscription, onOpenCalendar, modifier)
}

@Composable
fun OverviewContent(
    state: OverviewUiState,
    onAddSubscription: () -> Unit,
    onOpenSubscription: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ObolTheme.spacing
    // Do prvog odgovora baze ne crtamo ništa, da prazno stanje ne bljesne.
    if (state is OverviewUiState.Loading) return
    val month = (state as? OverviewUiState.Content)?.today?.let(YearMonth::from) ?: YearMonth.now()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontal,
            end = spacing.screenHorizontal,
            top = spacing.screenTop,
            bottom = spacing.x24,
        ),
    ) {
        item(key = "header") {
            Header(month = month, onAddSubscription = onAddSubscription)
        }
        when (state) {
            OverviewUiState.Loading -> Unit
            OverviewUiState.Empty -> item(key = "empty") { EmptyState(onAddSubscription) }
            is OverviewUiState.Content -> content(state.overview, state.today, onOpenSubscription, onOpenCalendar)
        }
    }
}

private fun LazyListScope.content(
    overview: Overview,
    today: LocalDate,
    onOpenSubscription: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
) {
    item(key = "total") { MonthlyTotal(overview) }

    if (overview.upcoming.isNotEmpty()) {
        item(key = "upcoming-header") {
            SectionHeader(
                title = stringResource(R.string.overview_upcoming),
                modifier = Modifier.padding(top = ObolTheme.spacing.x16),
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = ObolTheme.spacing.minTouchTarget)
                        .clickable(role = Role.Button, onClick = onOpenCalendar)
                        .padding(start = ObolTheme.spacing.x12),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = stringResource(R.string.nav_calendar),
                        style = ObolTheme.typography.link,
                        color = ObolTheme.colors.accent,
                    )
                }
            }
        }
        items(overview.upcoming, key = { "upcoming-${it.subscription.id}" }) { event ->
            UpcomingCard(
                event = event,
                today = today,
                onClick = { onOpenSubscription(event.subscription.id) },
                modifier = Modifier.padding(top = ObolTheme.spacing.listRowGap),
            )
        }
    }

    item(key = "all-header") {
        SectionHeader(
            title = stringResource(R.string.overview_all_subscriptions),
            modifier = Modifier.padding(top = ObolTheme.spacing.sectionGap),
        ) {
            Text(
                text = overview.activeCount.toString(),
                style = ObolTheme.typography.caption,
                color = ObolTheme.colors.textSecondary,
            )
        }
    }
    items(overview.subscriptions, key = { "row-${it.subscription.id}" }) { summary ->
        SubscriptionRow(summary, onClick = { onOpenSubscription(summary.subscription.id) })
    }
}

@Composable
private fun Header(month: YearMonth, onAddSubscription: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.x8),
            ) {
                ObolLogo()
                Text(
                    text = stringResource(R.string.app_name),
                    style = ObolTheme.typography.brandTitle,
                    color = colors.textPrimary,
                )
            }
            Text(
                text = ObolFormat.monthYear(month).uppercase(ObolFormat.locale),
                style = ObolTheme.typography.overline,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.x6),
            )
        }
        Box(
            modifier = Modifier
                .size(spacing.minTouchTarget)
                .clip(ObolTheme.shapes.button)
                .background(colors.surface)
                .border(spacing.borderWidth, colors.borderStrong, ObolTheme.shapes.button)
                .clickable(role = Role.Button, onClick = onAddSubscription),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = stringResource(R.string.action_add_subscription),
                tint = colors.textPrimary,
                modifier = Modifier.size(spacing.iconSize),
            )
        }
    }
}

@Composable
private fun MonthlyTotal(overview: Overview) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    Column(Modifier.padding(top = spacing.x24)) {
        Text(
            text = stringResource(R.string.overview_monthly_total),
            style = typography.body,
            color = colors.textSecondary,
        )
        Row(
            modifier = Modifier.padding(top = spacing.x6),
            horizontalArrangement = Arrangement.spacedBy(spacing.x8),
        ) {
            Text(
                text = ObolFormat.amount(overview.monthlyTotalCents),
                style = typography.hero,
                color = colors.textPrimary,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                text = ObolFormat.currencySymbol(),
                style = typography.heroCurrency,
                color = colors.textSecondary,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Text(
            text = stringResource(
                R.string.overview_yearly_and_count,
                ObolFormat.money(overview.yearlyTotalCents),
                pluralStringResource(
                    R.plurals.overview_active_subscriptions,
                    overview.activeCount,
                    overview.activeCount,
                ),
            ),
            style = typography.bodyRegular,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.x8),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ObolTheme.spacing.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = ObolTheme.typography.sectionTitle,
            color = ObolTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun UpcomingCard(
    event: UpcomingEvent,
    today: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    val subscription = event.subscription
    when (event) {
        is UpcomingEvent.Billing -> CardRow(
            background = colors.surface,
            borderColor = colors.border,
            onClick = onClick,
            modifier = modifier,
            leading = { ServiceMark(subscription.serviceId, subscription.monogram, serviceColor(subscription.colorHex)) },
            title = subscription.name,
            subtitle = stringResource(
                R.string.overview_billing_subtitle,
                relativeDaysText(today, event.date),
                ObolFormat.dateLong(event.date),
            ),
            subtitleStyle = typography.captionSoft,
            subtitleColor = colors.textSecondary,
            trailing = {
                Text(
                    text = ObolFormat.money(event.priceCents, subscription.currency),
                    style = typography.amount,
                    color = colors.textPrimary,
                )
            },
        )

        is UpcomingEvent.TrialEnding -> CardRow(
            background = colors.trialSurface,
            borderColor = colors.trialBorder,
            onClick = onClick,
            modifier = modifier,
            leading = { TrialTile() },
            title = stringResource(R.string.overview_trial_title, subscription.name),
            subtitle = stringResource(
                R.string.overview_trial_subtitle,
                relativeDaysText(today, event.date),
                ObolFormat.dateLong(event.date),
            ),
            subtitleStyle = typography.caption,
            subtitleColor = colors.trialText,
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = ObolFormat.money(event.firstChargeCents, subscription.currency),
                        style = typography.amount,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.overview_first_charge),
                        style = typography.badge,
                        color = colors.trialText,
                        modifier = Modifier.padding(top = ObolTheme.spacing.x2),
                    )
                }
            },
        )
    }
}

@Composable
private fun CardRow(
    background: Color,
    borderColor: Color,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    subtitleStyle: TextStyle,
    subtitleColor: Color,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = ObolTheme.shapes.card,
) {
    val spacing = ObolTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(spacing.borderWidth, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(spacing.x12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x12),
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = ObolTheme.typography.sectionTitle,
                color = ObolTheme.colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = subtitleStyle,
                color = subtitleColor,
                modifier = Modifier.padding(top = spacing.x2),
            )
        }
        trailing()
    }
}

@Composable
private fun TrialTile() {
    val colors = ObolTheme.colors
    Box(
        modifier = Modifier
            .size(ObolTheme.spacing.monogramTile)
            .clip(ObolTheme.shapes.iconTile)
            .background(colors.tintedBackground(colors.trial)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = null,
            tint = colors.trial,
            modifier = Modifier.size(ObolTheme.spacing.iconSizeSmall),
        )
    }
}

@Composable
private fun SubscriptionRow(summary: SubscriptionSummary, onClick: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    val subscription = summary.subscription
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.minTouchTarget)
                .clickable(onClick = onClick)
                .padding(vertical = spacing.x10, horizontal = spacing.x2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.x10),
        ) {
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
            RowBadge(summary)
            Column(
                modifier = Modifier.widthIn(min = spacing.amountColumn),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = ObolFormat.money(summary.currentPriceCents, subscription.currency),
                    style = typography.rowAmount,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End,
                )
                cycleSuffixText(subscription)?.let {
                    Text(text = it, style = typography.badge, color = colors.textSecondary)
                }
            }
        }
        ListDivider()
    }
}

/** Probni period ima prednost; inače nedavna promjena cijene u boji svog značenja. */
@Composable
private fun RowBadge(summary: SubscriptionSummary) {
    val colors = ObolTheme.colors
    val change = summary.recentPriceChangeCents
    when {
        summary.inTrial -> Text(
            text = stringResource(R.string.overview_trial_badge),
            style = ObolTheme.typography.link,
            color = colors.trialText,
        )

        change != null -> Text(
            text = ObolFormat.signedMoney(change, summary.subscription.currency),
            style = ObolTheme.typography.link,
            color = if (change > 0) colors.warning else colors.accent,
        )
    }
}

@Composable
private fun EmptyState(onAddSubscription: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Column(Modifier.padding(top = spacing.x32 + spacing.x32)) {
        Text(
            text = stringResource(R.string.overview_empty_title),
            style = ObolTheme.typography.screenTitle,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.overview_empty_body),
            style = ObolTheme.typography.bodySoft,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.x8),
        )
        Button(
            onClick = onAddSubscription,
            shape = ObolTheme.shapes.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
            ),
            modifier = Modifier
                .padding(top = spacing.x24)
                .fillMaxWidth()
                .height(spacing.primaryButtonHeight),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                modifier = Modifier.size(spacing.iconSize),
            )
            Spacer(Modifier.size(spacing.x8))
            Text(
                text = stringResource(R.string.action_add_subscription),
                style = ObolTheme.typography.sectionTitle,
            )
        }
    }
}
