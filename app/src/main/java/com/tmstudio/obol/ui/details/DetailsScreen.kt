package com.tmstudio.obol.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmstudio.obol.R
import com.tmstudio.obol.ui.components.ListDivider
import com.tmstudio.obol.ui.components.ServiceMark
import com.tmstudio.obol.ui.components.ScreenHeader
import com.tmstudio.obol.ui.components.categoryLabel
import com.tmstudio.obol.ui.components.cycleSuffixText
import com.tmstudio.obol.ui.components.relativeDaysText
import com.tmstudio.obol.ui.components.serviceColor
import com.tmstudio.obol.ui.format.ObolFormat
import com.tmstudio.obol.ui.theme.ObolTheme
import java.time.YearMonth

/** Ekran „Detalji pretplate" prema `docs/mockups/Detalji.html`. Kartica preporuke je izostavljena u v1. */
@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DetailsViewModel = viewModel(factory = DetailsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state is DetailsUiState.Missing) onBack() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = null, onBack = onBack)
        val content = state as? DetailsUiState.Content ?: return@Column
        DetailsContent(
            state = content,
            onAnswerUsage = viewModel::answerUsage,
            onEdit = { onEdit(content.details.subscription.id) },
            onDelete = viewModel::delete,
        )
    }
}

@Composable
private fun DetailsContent(
    state: DetailsUiState.Content,
    onAnswerUsage: (YearMonth, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = ObolTheme.spacing
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = spacing.screenHorizontal)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = spacing.x16, bottom = spacing.x8),
            verticalArrangement = Arrangement.spacedBy(spacing.x14),
        ) {
            Identity(state)
            PriceCard(state)
            state.usageQuestion?.let { period ->
                UsageCard(state.details.subscription.name, period, onAnswer = { used -> onAnswerUsage(period, used) })
            }
            if (state.history.isNotEmpty()) History(state)
        }
        Actions(onEdit = onEdit, onDelete = { confirmDelete = true })
    }

    if (confirmDelete) {
        DeleteDialog(
            name = state.details.subscription.name,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Identity(state: DetailsUiState.Content) {
    val sub = state.details.subscription
    val spacing = ObolTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x14),
    ) {
        ServiceMark(
            serviceId = sub.serviceId,
            monogram = sub.monogram,
            color = serviceColor(sub.colorHex),
            size = spacing.monogramTileXl,
            shape = ObolTheme.shapes.cardLarge,
            style = ObolTheme.typography.monogramXl,
        )
        Column {
            Text(text = sub.name, style = ObolTheme.typography.detailTitle, color = ObolTheme.colors.textPrimary)
            Text(
                text = sub.planLabel ?: categoryLabel(sub.category),
                style = ObolTheme.typography.body,
                color = ObolTheme.colors.textSecondary,
                modifier = Modifier.padding(top = spacing.x2),
            )
        }
    }
}

@Composable
private fun DetailsCard(content: @Composable () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.cardXl
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(spacing.borderWidth, colors.border, shape)
            .padding(spacing.cardPaddingLarge)
    ) {
        content()
    }
}

@Composable
private fun PriceCard(state: DetailsUiState.Content) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    val sub = state.details.subscription
    DetailsCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.details_price), style = typography.caption, color = colors.textSecondary)
                Row(
                    modifier = Modifier.padding(top = spacing.x4),
                    horizontalArrangement = Arrangement.spacedBy(spacing.x6),
                ) {
                    Text(
                        text = ObolFormat.money(state.currentPriceCents, sub.currency),
                        style = typography.amountLarge,
                        color = colors.textPrimary,
                        modifier = Modifier.alignByBaseline(),
                    )
                    cycleSuffixText(sub)?.let {
                        Text(it, style = typography.caption, color = colors.textSecondary, modifier = Modifier.alignByBaseline())
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.details_yearly), style = typography.caption, color = colors.textSecondary)
                Text(
                    text = ObolFormat.money(state.yearlyFullPriceCents, sub.currency),
                    style = typography.amountMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = spacing.x4),
                )
            }
        }

        state.trialEndsOn?.let { endsOn ->
            Text(
                text = stringResource(
                    R.string.details_trial_then,
                    ObolFormat.dateLong(endsOn),
                    ObolFormat.money(state.nextCharge.priceCents, sub.currency),
                ),
                style = typography.caption,
                color = colors.trialText,
                modifier = Modifier.padding(top = spacing.x8),
            )
        }
        if (state.promoEndsOn != null && state.priceAfterPromoCents != null) {
            // Koral: nakon promocije cijena raste.
            Text(
                text = stringResource(
                    R.string.details_promo_then,
                    ObolFormat.dateLong(state.promoEndsOn),
                    ObolFormat.money(state.priceAfterPromoCents, sub.currency),
                ),
                style = typography.caption,
                color = colors.warning,
                modifier = Modifier.padding(top = spacing.x8),
            )
        }

        ListDivider(Modifier.padding(top = spacing.x14))
        Row(
            modifier = Modifier.padding(top = spacing.x14),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.x8),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_calendar),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(spacing.iconSizeSmall),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.details_next_charge, ObolFormat.dateLong(state.nextCharge.date)),
                    style = typography.body,
                    color = colors.textPrimary,
                )
                // Iznos se ističe samo kad se razlikuje od današnjeg, npr. nakon najavljenog
                // poskupljenja. Probni period ima vlastiti redak iznad.
                val nextPrice = state.nextCharge.priceCents
                if (state.trialEndsOn == null && nextPrice != state.currentPriceCents) {
                    Text(
                        text = ObolFormat.money(nextPrice, sub.currency),
                        style = typography.captionStrong,
                        color = if (nextPrice > state.currentPriceCents) colors.warning else colors.accent,
                        modifier = Modifier.padding(top = spacing.x2),
                    )
                }
            }
            Pill(relativeDaysText(state.today, state.nextCharge.date))
        }
    }
}

@Composable
private fun Pill(text: String) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.pill
    Text(
        text = text,
        style = ObolTheme.typography.captionStrong,
        color = colors.accent,
        modifier = Modifier
            .clip(shape)
            .background(colors.accentSurface)
            .border(spacing.borderWidth, colors.accentBorder, shape)
            .padding(horizontal = spacing.x8, vertical = spacing.x4),
    )
}

@Composable
private fun UsageCard(name: String, period: YearMonth, onAnswer: (Boolean) -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    val month = stringArrayResource(R.array.months_locative)[period.monthValue - 1]
    DetailsCard {
        Text(
            text = stringResource(R.string.details_usage_question, name, month),
            style = typography.sectionTitle,
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.details_usage_body),
            style = typography.captionSoft,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = spacing.x6),
        )
        Row(
            modifier = Modifier.padding(top = spacing.x14),
            horizontalArrangement = Arrangement.spacedBy(spacing.listRowGap),
        ) {
            ChoiceButton(
                text = stringResource(R.string.details_usage_yes),
                background = colors.accent,
                border = colors.accent,
                textColor = colors.onAccent,
                onClick = { onAnswer(true) },
                modifier = Modifier.weight(1f),
            )
            ChoiceButton(
                text = stringResource(R.string.details_usage_no),
                background = Color.Transparent,
                border = colors.borderStrong,
                textColor = colors.textPrimary,
                onClick = { onAnswer(false) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChoiceButton(
    text: String,
    background: Color,
    border: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = ObolTheme.spacing.minTouchTarget,
) {
    val spacing = ObolTheme.spacing
    val shape = ObolTheme.shapes.iconTile
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(shape)
            .background(background)
            .border(spacing.borderWidth, border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.x16),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = ObolTheme.typography.sectionTitle, color = textColor)
    }
}

@Composable
private fun History(state: DetailsUiState.Content) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    val currency = state.details.subscription.currency
    Column(Modifier.padding(top = spacing.x4)) {
        Text(
            text = stringResource(R.string.details_history).uppercase(ObolFormat.locale),
            style = typography.sectionLabel,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = spacing.x8),
        )
        state.history.forEachIndexed { index, billing ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = spacing.x8, horizontal = spacing.x2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ObolFormat.dateLongWithYear(billing.date),
                    style = typography.bodySoft,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(text = ObolFormat.money(billing.priceCents, currency), style = typography.amountSmall, color = colors.textPrimary)
            }
            if (index < state.history.lastIndex) ListDivider()
        }
    }
}

@Composable
private fun Actions(onEdit: () -> Unit, onDelete: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Row(
        modifier = Modifier.padding(top = spacing.x16, bottom = spacing.x24),
        horizontalArrangement = Arrangement.spacedBy(spacing.listRowGap),
    ) {
        ChoiceButton(
            text = stringResource(R.string.details_edit),
            background = colors.surface,
            border = colors.borderStrong,
            textColor = colors.textPrimary,
            onClick = onEdit,
            modifier = Modifier.weight(1f),
            minHeight = spacing.secondaryButtonHeight,
        )
        // Koral: brisanje je opasna radnja.
        ChoiceButton(
            text = stringResource(R.string.details_delete),
            background = Color.Transparent,
            border = colors.warningBorder,
            textColor = colors.warning,
            onClick = onDelete,
            minHeight = spacing.secondaryButtonHeight,
        )
    }
}

@Composable
private fun DeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = ObolTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(stringResource(R.string.details_delete_title, name), style = ObolTheme.typography.cardTitle, color = colors.textPrimary)
        },
        text = {
            Text(stringResource(R.string.details_delete_body), style = ObolTheme.typography.bodySoft, color = colors.textSecondary)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.details_delete), color = colors.warning)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        },
    )
}

