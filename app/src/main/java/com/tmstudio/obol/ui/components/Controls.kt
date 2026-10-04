package com.tmstudio.obol.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.tmstudio.obol.R
import com.tmstudio.obol.ui.theme.ObolTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Zaglavlje ekrana s povratnom strelicom (Dodaj, Postavi plan, Detalji). */
@Composable
fun ScreenHeader(title: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, top = spacing.screenTop),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x12),
    ) {
        IconSquareButton(R.drawable.ic_back, stringResource(R.string.action_back), onBack)
        title?.let { Text(text = it, style = ObolTheme.typography.subScreenTitle, color = colors.textPrimary) }
    }
}

/** Kvadratni gumb s ikonom: povratak, prethodni i sljedeći mjesec. */
@Composable
fun IconSquareButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.iconTile
    Box(
        modifier = modifier
            .size(spacing.minTouchTarget)
            .clip(shape)
            .background(colors.surface)
            .border(spacing.borderWidth, colors.borderStrong, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(spacing.iconSizeSmall),
        )
    }
}

/** Prekidač u bojama teme: mint kad je uključen. */
@Composable
fun ObolSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val colors = ObolTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.switchThumb,
            checkedTrackColor = colors.accent,
            checkedBorderColor = colors.accent,
            uncheckedThumbColor = colors.switchThumbOff,
            uncheckedTrackColor = colors.surfaceAlt,
            uncheckedBorderColor = colors.switchTrackOffBorder,
        ),
    )
}

/** Isprekidani obrub, npr. za „Dodaj ručno". */
fun Modifier.dashedBorder(width: Dp, color: Color, shape: Shape, dash: Dp, gap: Dp): Modifier =
    drawWithContent {
        drawContent()
        val stroke = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
        )
        drawOutline(shape.createOutline(size, layoutDirection, this), color, style = stroke)
    }

/** Jedna opcija u donjem izborniku. */
data class SheetOption<T>(
    val value: T,
    val title: String,
    val subtitle: String? = null,
    val trailing: String? = null,
    val leadingColor: Color? = null,
)

/** Donji izbornik s popisom opcija; odabrana je označena mint bojom. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<SheetOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        contentColor = colors.textPrimary,
    ) {
        Text(
            text = title,
            style = typography.cardTitle,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.x8),
        )
        LazyColumn(Modifier.navigationBarsPadding().padding(bottom = spacing.x16)) {
            items(options) { option ->
                val isSelected = option.value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = spacing.settingRowMinHeight)
                        .clickable { onSelect(option.value) }
                        .padding(horizontal = spacing.screenHorizontal, vertical = spacing.x10),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.x12),
                ) {
                    option.leadingColor?.let {
                        Box(Modifier.size(spacing.dot).clip(ObolTheme.shapes.dot).background(it))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            style = typography.sectionTitle,
                            color = if (isSelected) colors.accent else colors.textPrimary,
                        )
                        option.subtitle?.let {
                            Text(text = it, style = typography.captionSoft, color = colors.textSecondary)
                        }
                    }
                    option.trailing?.let {
                        Text(text = it, style = typography.amount, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}

/** Birač datuma. DatePicker radi s UTC milisekundama, pa se LocalDate pretvara preko UTC ponoći. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObolDatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ObolTheme.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    ?.let(onConfirm)
                onDismiss()
            }) {
                Text(stringResource(R.string.action_ok), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = colors.surface),
    ) {
        DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = colors.surface))
    }
}
