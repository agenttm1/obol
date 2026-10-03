package com.tmstudio.obol.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmstudio.obol.R
import com.tmstudio.obol.ui.components.ListDivider
import com.tmstudio.obol.ui.components.ObolSwitch
import com.tmstudio.obol.ui.components.OptionSheet
import com.tmstudio.obol.ui.components.SheetOption
import com.tmstudio.obol.ui.format.ObolFormat
import com.tmstudio.obol.ui.theme.ObolTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Ekran Postavke prema `docs/mockups/Postavke.html`, bez Pro kartice i izvoza (izvan v1). */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val current = settings ?: return
    val spacing = ObolTheme.spacing
    val context = LocalContext.current

    // Korisnik može obavijesti uključiti u sustavu i vratiti se, pa se stanje čita pri svakom povratku.
    var notificationsEnabled by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    var showDaysSheet by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = spacing.screenHorizontal, end = spacing.screenHorizontal, top = spacing.screenTop, bottom = spacing.x24)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = ObolTheme.typography.screenTitle,
            color = ObolTheme.colors.textPrimary,
        )

        Group(stringResource(R.string.settings_group_reminders), Modifier.padding(top = spacing.x20)) {
            if (!notificationsEnabled) {
                NotificationsOffRow(onEnable = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                })
                ListDivider()
            }
            ValueRow(
                label = stringResource(R.string.settings_billing_days),
                value = billingDaysLabel(current.billingDaysBefore),
                onClick = { showDaysSheet = true },
            )
            ListDivider()
            ValueRow(
                label = stringResource(R.string.settings_reminder_time),
                value = current.time.format(TIME_FORMAT),
                onClick = { showTimePicker = true },
            )
            ListDivider()
            ToggleRow(
                label = stringResource(R.string.settings_usage_checks),
                subtitle = stringResource(R.string.settings_usage_checks_subtitle),
                checked = current.usageChecksEnabled,
                onCheckedChange = viewModel::setUsageChecks,
            )
            ListDivider()
            ToggleRow(
                label = stringResource(R.string.settings_price_alerts),
                subtitle = stringResource(R.string.settings_price_alerts_subtitle),
                checked = current.priceIncreaseAlertsEnabled,
                onCheckedChange = viewModel::setPriceIncreaseAlerts,
            )
        }

        Group(stringResource(R.string.settings_group_general), Modifier.padding(top = spacing.x20)) {
            // U v1 su valuta i tema fiksne (spec: valuta zasad uvijek EUR, samo tamna tema).
            ValueRow(stringResource(R.string.settings_currency), stringResource(R.string.settings_currency_value), onClick = null)
            ListDivider()
            ValueRow(stringResource(R.string.settings_theme), stringResource(R.string.settings_theme_value), onClick = null)
        }

        Text(
            text = stringResource(R.string.settings_privacy),
            style = ObolTheme.typography.footnote,
            color = ObolTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.x20),
        )
        Text(
            text = stringResource(R.string.settings_footer, appVersion()),
            style = ObolTheme.typography.footnote,
            color = ObolTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.x4),
        )
    }

    if (showDaysSheet) {
        OptionSheet(
            title = stringResource(R.string.settings_billing_days),
            options = SettingsViewModel.BILLING_DAY_OPTIONS.map { SheetOption(it, billingDaysLabel(it)) },
            selected = current.billingDaysBefore,
            onSelect = { viewModel.setBillingDaysBefore(it); showDaysSheet = false },
            onDismiss = { showDaysSheet = false },
        )
    }
    if (showTimePicker) {
        ReminderTimeDialog(
            initial = current.time,
            onConfirm = { viewModel.setReminderTime(it); showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun billingDaysLabel(days: Int): String =
    if (days == 0) {
        stringResource(R.string.settings_billing_days_off)
    } else {
        pluralStringResource(R.plurals.calendar_days, days, days)
    }

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
}

@Composable
private fun Group(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.cardLarge
    Column(modifier) {
        Text(
            text = title.uppercase(ObolFormat.locale),
            style = ObolTheme.typography.groupLabel,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = spacing.x8),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface)
                .border(spacing.borderWidth, colors.border, shape),
            content = content,
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: (() -> Unit)?) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.settingRowMinHeight)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = spacing.x16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x12),
    ) {
        Text(label, style = ObolTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = ObolTheme.typography.settingValue, color = colors.textSecondary)
        if (onClick != null) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(spacing.iconSizeXs),
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle != null) spacing.toggleRowMinHeight else spacing.settingRowMinHeight)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = spacing.x16, vertical = spacing.x8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x12),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = ObolTheme.typography.bodyLarge, color = colors.textPrimary)
            subtitle?.let {
                Text(
                    it,
                    style = ObolTheme.typography.captionSmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.x2),
                )
            }
        }
        // Klik obrađuje cijeli redak, pa prekidač sam ne prima dodir.
        ObolSwitch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NotificationsOffRow(onEnable: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.toggleRowMinHeight)
            .clickable(role = Role.Button, onClick = onEnable)
            .padding(horizontal = spacing.x16, vertical = spacing.x8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x12),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_notifications_off),
                style = ObolTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            Text(
                stringResource(R.string.settings_notifications_off_subtitle),
                style = ObolTheme.typography.captionSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.x2),
            )
        }
        Text(stringResource(R.string.settings_notifications_enable), style = ObolTheme.typography.link, color = colors.accent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: LocalTime, onConfirm: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val colors = ObolTheme.colors
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(stringResource(R.string.settings_reminder_time), style = ObolTheme.typography.cardTitle, color = colors.textPrimary)
        },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.surfaceAlt,
                    timeSelectorUnselectedContainerColor = colors.surfaceAlt,
                    timeSelectorSelectedContainerColor = colors.accentSurface,
                    timeSelectorSelectedContentColor = colors.accent,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.action_ok), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        },
    )
}

