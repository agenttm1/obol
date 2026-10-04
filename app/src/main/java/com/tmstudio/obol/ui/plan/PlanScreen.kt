package com.tmstudio.obol.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmstudio.obol.R
import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.Category
import com.tmstudio.obol.domain.parseMoneyInput
import com.tmstudio.obol.ui.components.ServiceMark
import com.tmstudio.obol.ui.components.ObolDatePickerDialog
import com.tmstudio.obol.ui.components.ObolSwitch
import com.tmstudio.obol.ui.components.OptionSheet
import com.tmstudio.obol.ui.components.ScreenHeader
import com.tmstudio.obol.ui.components.SheetOption
import com.tmstudio.obol.ui.components.categoryLabel
import com.tmstudio.obol.ui.components.cycleLabel
import com.tmstudio.obol.ui.components.cycleSuffix
import com.tmstudio.obol.ui.components.serviceColor
import com.tmstudio.obol.domain.defaultColorHex
import com.tmstudio.obol.ui.format.ObolFormat
import com.tmstudio.obol.ui.theme.ObolTheme

private enum class PlanSheet { PLAN, CYCLE, CATEGORY }
private enum class PlanDate { TRIAL_END, BILLING, PRICE_CHANGE }

/** Ekran „Postavi plan" prema `docs/mockups/Plan.html`. */
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    /** Argument je true kad je upravo spremljena prva pretplata. */
    onSaved: (Boolean) -> Unit,
    viewModel: PlanViewModel = viewModel(factory = PlanViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onSaved(state.savedFirst) }

    val form = state.form
    Column(Modifier.fillMaxSize().imePadding()) {
        val title = if (form?.isEditing == true) R.string.plan_title_edit else R.string.plan_title
        ScreenHeader(title = stringResource(title), onBack = onBack)
        if (form != null) {
            PlanContent(
                form = form,
                errors = if (state.showErrors) form.errors(state.today) else emptySet(),
                saving = state.saving,
                onChange = viewModel::update,
                onSave = viewModel::save,
            )
        }
    }
}

@Composable
private fun PlanContent(
    form: PlanForm,
    errors: Set<PlanFormError>,
    saving: Boolean,
    onChange: ((PlanForm) -> PlanForm) -> Unit,
    onSave: () -> Unit,
) {
    val spacing = ObolTheme.spacing
    var sheet by rememberSaveable { mutableStateOf<PlanSheet?>(null) }
    var datePicker by rememberSaveable { mutableStateOf<PlanDate?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenHorizontal)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = spacing.x16, bottom = spacing.x8),
            verticalArrangement = Arrangement.spacedBy(spacing.x12),
        ) {
            if (form.isManual) {
                ManualServiceCard(form, onNameChange = { name -> onChange { it.copy(name = name) } })
                if (PlanFormError.NAME_MISSING in errors) ErrorText(stringResource(R.string.plan_error_name))
                SettingRow(
                    label = stringResource(R.string.plan_category),
                    value = categoryLabel(form.category),
                    onClick = { sheet = PlanSheet.CATEGORY },
                )
            } else {
                CatalogServiceCard(form, onChangePlan = { sheet = PlanSheet.PLAN })
            }

            PriceAndCycle(form, onChange, onCycleClick = { sheet = PlanSheet.CYCLE })
            if (PlanFormError.PRICE_INVALID in errors) ErrorText(stringResource(R.string.plan_error_price))
            if (form.priceChanged) {
                SettingRow(
                    label = stringResource(R.string.plan_price_change_from),
                    value = ObolFormat.dateShort(form.priceChangeFrom),
                    onClick = { datePicker = PlanDate.PRICE_CHANGE },
                    caption = stringResource(R.string.plan_price_change_caption),
                )
            }
            if (form.cycle == BillingCycle.CUSTOM_DAYS) {
                CycleDaysField(form, onChange)
                if (PlanFormError.CYCLE_DAYS_INVALID in errors) ErrorText(stringResource(R.string.plan_error_cycle_days))
            }

            TrialSection(form, onChange, onPickDate = { datePicker = PlanDate.TRIAL_END })
            if (PlanFormError.TRIAL_ENDED in errors) ErrorText(stringResource(R.string.plan_error_trial_ended))

            PromoSection(form, onChange)
            if (PlanFormError.PROMO_PRICE_INVALID in errors) ErrorText(stringResource(R.string.plan_error_promo_price))
            if (PlanFormError.PROMO_NOT_LOWER in errors) ErrorText(stringResource(R.string.plan_error_promo_not_lower))

            BillingDayRow(form, onClick = { datePicker = PlanDate.BILLING })
        }

        SaveArea(isEditing = form.isEditing, saving = saving, onSave = onSave)
    }

    when (sheet) {
        PlanSheet.PLAN -> OptionSheet(
            title = stringResource(R.string.plan_choose_plan),
            options = form.plans.map { plan ->
                SheetOption(
                    value = plan,
                    title = plan.name,
                    subtitle = plan.note,
                    trailing = stringResource(
                        R.string.plan_amount_per_cycle,
                        ObolFormat.money(plan.priceCents),
                        cycleSuffix(plan.cycle, null),
                    ),
                )
            },
            selected = form.plans.firstOrNull {
                it.name == form.planLabel && it.cycle == form.cycle && parseMoneyInput(form.priceText) == it.priceCents
            },
            onSelect = { plan -> onChange { it.withPlan(plan) }; sheet = null },
            onDismiss = { sheet = null },
        )

        PlanSheet.CYCLE -> OptionSheet(
            title = stringResource(R.string.plan_cycle),
            options = BillingCycle.entries.map { SheetOption(it, cycleLabel(it, null)) },
            selected = form.cycle,
            onSelect = { cycle -> onChange { it.copy(cycle = cycle) }; sheet = null },
            onDismiss = { sheet = null },
        )

        PlanSheet.CATEGORY -> OptionSheet(
            title = stringResource(R.string.plan_category),
            options = Category.entries.map {
                SheetOption(it, categoryLabel(it), leadingColor = serviceColor(it.defaultColorHex()))
            },
            selected = form.category,
            onSelect = { category -> onChange { it.copy(category = category) }; sheet = null },
            onDismiss = { sheet = null },
        )

        null -> Unit
    }

    datePicker?.let { target ->
        ObolDatePickerDialog(
            initial = when (target) {
                PlanDate.TRIAL_END -> form.trialEndsOn
                PlanDate.BILLING -> form.billingDate
                PlanDate.PRICE_CHANGE -> form.priceChangeFrom
            },
            onConfirm = { date ->
                onChange {
                    when (target) {
                        PlanDate.TRIAL_END -> it.copy(trialEndsOn = date)
                        PlanDate.BILLING -> it.copy(billingDate = date)
                        PlanDate.PRICE_CHANGE -> it.copy(priceChangeFrom = date)
                    }
                }
            },
            onDismiss = { datePicker = null },
        )
    }
}

@Composable
private fun CatalogServiceCard(form: PlanForm, onChangePlan: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    ServiceCard(form) {
        Column(Modifier.weight(1f)) {
            Text(text = form.name, style = ObolTheme.typography.cardTitle, color = colors.textPrimary)
            form.planLabel?.let {
                Text(
                    text = it,
                    style = ObolTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.x2),
                )
            }
        }
        if (form.plans.size > 1) {
            val shape = ObolTheme.shapes.chip
            Box(
                modifier = Modifier
                    .heightIn(min = spacing.minTouchTarget)
                    .clip(shape)
                    .border(spacing.borderWidth, colors.borderStrong, shape)
                    .clickable(role = Role.Button, onClick = onChangePlan)
                    .padding(horizontal = spacing.x12),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.plan_change), style = ObolTheme.typography.chip, color = colors.textPrimary)
            }
        }
    }
}

@Composable
private fun ManualServiceCard(form: PlanForm, onNameChange: (String) -> Unit) {
    val colors = ObolTheme.colors
    ServiceCard(form) {
        BasicTextField(
            value = form.name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = ObolTheme.typography.cardTitle.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.weight(1f).heightIn(min = ObolTheme.spacing.minTouchTarget),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (form.name.isEmpty()) {
                        Text(
                            text = stringResource(R.string.plan_name_placeholder),
                            style = ObolTheme.typography.cardTitle,
                            color = colors.textTertiary,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun ServiceCard(form: PlanForm, content: @Composable RowScope.() -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.tile
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(spacing.borderWidth, colors.border, shape)
            .padding(vertical = spacing.x12, horizontal = spacing.x14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x12),
    ) {
        ServiceMark(
            serviceId = form.serviceId,
            monogram = form.monogram,
            color = serviceColor(form.colorHex),
            size = spacing.monogramTileLarge,
            style = ObolTheme.typography.monogramLarge,
        )
        content()
    }
}

@Composable
private fun PriceAndCycle(form: PlanForm, onChange: ((PlanForm) -> PlanForm) -> Unit, onCycleClick: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(spacing.listRowGap),
    ) {
        FieldBox(label = stringResource(R.string.plan_price), modifier = Modifier.weight(1f)) {
            AmountField(
                value = form.priceText,
                onValueChange = { text -> onChange { it.copy(priceText = text) } },
                style = ObolTheme.typography.inputAmount,
                suffix = ObolFormat.currencySymbol(),
            )
        }
        FieldBox(
            label = stringResource(R.string.plan_cycle),
            modifier = Modifier.weight(1f),
            onClick = onCycleClick,
        ) {
            // Ista visina kao polje za cijenu, da vrijednosti stoje u istoj liniji.
            Row(
                modifier = Modifier.heightIn(min = spacing.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.x6),
            ) {
                Text(
                    text = cycleLabel(form.cycle, null),
                    style = ObolTheme.typography.inputSuffix,
                    color = colors.textPrimary,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(spacing.iconSizeXs),
                )
            }
        }
    }
}

@Composable
private fun CycleDaysField(form: PlanForm, onChange: ((PlanForm) -> PlanForm) -> Unit) {
    FieldBox(label = stringResource(R.string.plan_cycle_days), modifier = Modifier.fillMaxWidth()) {
        val colors = ObolTheme.colors
        BasicTextField(
            value = form.cycleDaysText,
            onValueChange = { text -> onChange { it.copy(cycleDaysText = text.filter(Char::isDigit).take(4)) } },
            singleLine = true,
            textStyle = ObolTheme.typography.inputAmount.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Kutija s velikom oznakom iznad sadržaja; cijela je klikabilna kad postoji [onClick]. */
@Composable
private fun FieldBox(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.card
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(colors.surface)
            .border(spacing.borderWidth, colors.border, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = spacing.x12, horizontal = spacing.x14),
        verticalArrangement = Arrangement.spacedBy(spacing.x6),
    ) {
        Text(
            text = label.uppercase(ObolFormat.locale),
            style = ObolTheme.typography.fieldLabel,
            color = colors.textSecondary,
        )
        content()
    }
}

/** Polje za iznos s oznakom valute odmah iza broja. */
@Composable
private fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    style: TextStyle,
    suffix: String,
    suffixStyle: TextStyle = ObolTheme.typography.inputSuffix,
    color: Color = ObolTheme.colors.textPrimary,
    textAlign: TextAlign = TextAlign.Start,
) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val focusRequester = remember { FocusRequester() }
    Row(
        modifier = Modifier
            .heightIn(min = spacing.minTouchTarget)
            .clickable(indication = null, interactionSource = null) { focusRequester.requestFocus() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x4),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == ',' || it == '.' }.take(9)) },
            singleLine = true,
            textStyle = style.copy(color = color, textAlign = textAlign),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .widthIn(min = spacing.x32)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) Text(text = ObolFormat.amount(0), style = style, color = colors.textTertiary)
                    inner()
                }
            },
        )
        Text(text = suffix, style = suffixStyle, color = colors.textSecondary)
    }
}

/** Kartica s prekidačem u zaglavlju i detaljima ispod kad je uključena. */
@Composable
private fun ToggleSection(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeBackground: Color,
    activeBorder: Color,
    activeTitleColor: Color,
    activeSubtitleColor: Color,
    content: @Composable () -> Unit,
) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.cardLarge
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (checked) activeBackground else colors.surface)
            .border(spacing.borderWidth, if (checked) activeBorder else colors.border, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.toggleRowMinHeight)
                .clickable(role = Role.Switch) { onCheckedChange(!checked) }
                .padding(vertical = spacing.x10, horizontal = spacing.x14),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.x12),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ObolTheme.typography.sectionTitle,
                    color = if (checked) activeTitleColor else colors.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = ObolTheme.typography.captionSmall,
                    color = if (checked) activeSubtitleColor else colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.x2),
                )
            }
            // Klik obrađuje cijeli redak, pa prekidač sam ne prima dodir.
            ObolSwitch(checked = checked, onCheckedChange = null)
        }
        if (checked) {
            Column(Modifier.padding(start = spacing.x14, end = spacing.x14, bottom = spacing.x12)) {
                content()
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    dividerColor: Color,
    onClick: (() -> Unit)? = null,
    value: @Composable () -> Unit,
) {
    val spacing = ObolTheme.spacing
    Column {
        Box(Modifier.fillMaxWidth().height(spacing.borderWidth).background(dividerColor))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.minTouchTarget)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.x8),
        ) {
            Text(
                text = label,
                style = ObolTheme.typography.body,
                color = ObolTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            value()
        }
    }
}

@Composable
private fun TrialSection(form: PlanForm, onChange: ((PlanForm) -> PlanForm) -> Unit, onPickDate: () -> Unit) {
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    ToggleSection(
        title = stringResource(R.string.plan_trial_title),
        subtitle = stringResource(R.string.plan_trial_subtitle),
        checked = form.trialEnabled,
        onCheckedChange = { checked -> onChange { it.copy(trialEnabled = checked) } },
        activeBackground = colors.trialSurface,
        activeBorder = colors.trialBorder,
        activeTitleColor = colors.trial,
        activeSubtitleColor = colors.trialText,
    ) {
        DetailRow(stringResource(R.string.plan_trial_until), colors.trialBorder, onClick = onPickDate) {
            Text(ObolFormat.dateShort(form.trialEndsOn), style = typography.valueStrong, color = colors.trial)
        }
        DetailRow(stringResource(R.string.plan_trial_then), colors.trialBorder) {
            Text(amountPerCycle(form, form.priceCents), style = typography.valueStrong, color = colors.trial)
        }
    }
}

@Composable
private fun PromoSection(form: PlanForm, onChange: ((PlanForm) -> PlanForm) -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    ToggleSection(
        title = stringResource(R.string.plan_promo_title),
        subtitle = stringResource(R.string.plan_promo_subtitle),
        checked = form.promoEnabled,
        onCheckedChange = { checked -> onChange { it.copy(promoEnabled = checked) } },
        activeBackground = colors.surface,
        activeBorder = colors.border,
        activeTitleColor = colors.textPrimary,
        activeSubtitleColor = colors.textSecondary,
    ) {
        DetailRow(stringResource(R.string.plan_promo_price), colors.divider) {
            AmountField(
                value = form.promoPriceText,
                onValueChange = { text -> onChange { it.copy(promoPriceText = text) } },
                style = typography.valueStrong,
                suffix = stringResource(
                    R.string.plan_amount_per_cycle,
                    ObolFormat.currencySymbol(),
                    cycleSuffix(form.cycle, form.cycleDays),
                ),
                suffixStyle = typography.valueStrong,
                textAlign = TextAlign.End,
            )
        }
        DetailRow(stringResource(R.string.plan_promo_duration), colors.divider) {
            Stepper(
                text = promoCyclesLabel(form.cycle, form.promoCycles),
                canDecrease = form.promoCycles > PlanForm.MIN_PROMO_CYCLES,
                canIncrease = form.promoCycles < PlanForm.MAX_PROMO_CYCLES,
                onDecrease = { onChange { it.copy(promoCycles = it.promoCycles - 1) } },
                onIncrease = { onChange { it.copy(promoCycles = it.promoCycles + 1) } },
            )
        }
        form.fullPriceFrom?.let { from ->
            DetailRow(
                stringResource(R.string.plan_full_price_from, ObolFormat.dateShort(from)),
                colors.divider,
            ) {
                // Koral: od tog datuma cijena raste.
                Text(
                    text = form.priceCents?.let { ObolFormat.money(it) }.orEmpty(),
                    style = typography.valueStrong,
                    color = colors.warning,
                    modifier = Modifier.padding(start = spacing.x8),
                )
            }
        }
    }
}

@Composable
private fun Stepper(
    text: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(R.drawable.ic_minus, stringResource(R.string.plan_promo_shorter), canDecrease, onDecrease)
        Text(
            text = text,
            style = ObolTheme.typography.valueStrong,
            color = ObolTheme.colors.textPrimary,
        )
        StepperButton(R.drawable.ic_plus, stringResource(R.string.plan_promo_longer), canIncrease, onIncrease)
    }
}

@Composable
private fun StepperButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = ObolTheme.colors
    Box(
        modifier = Modifier
            .size(ObolTheme.spacing.minTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (enabled) colors.accent else colors.textTertiary,
            modifier = Modifier.size(ObolTheme.spacing.iconSizeXs),
        )
    }
}

@Composable
private fun BillingDayRow(form: PlanForm, onClick: () -> Unit) {
    val date = form.firstBillingDate
    val value = if (form.cycle == BillingCycle.MONTHLY) {
        stringResource(R.string.plan_billing_day_monthly, date.dayOfMonth)
    } else {
        ObolFormat.dateShort(date)
    }
    // S probnim periodom prva naplata slijedi iz njegova kraja i ne bira se zasebno.
    SettingRow(
        label = stringResource(R.string.plan_billing_day),
        value = value,
        onClick = if (form.trialEnabled) null else onClick,
        caption = if (form.trialEnabled) {
            stringResource(R.string.plan_billing_day_after_trial, ObolFormat.dateShort(date))
        } else {
            null
        },
    )
}

@Composable
private fun SettingRow(label: String, value: String, onClick: (() -> Unit)?, caption: String? = null) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.buttonLarge
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(spacing.borderWidth, colors.border, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = spacing.settingRowMinHeight)
            .padding(horizontal = spacing.x14, vertical = spacing.x10),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = ObolTheme.typography.bodyLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(text = value, style = ObolTheme.typography.link, color = colors.textSecondary)
            if (onClick != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.padding(start = spacing.x8).size(spacing.iconSizeXs),
                )
            }
        }
        caption?.let {
            Text(
                text = it,
                style = ObolTheme.typography.captionSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.x2),
            )
        }
    }
}

@Composable
private fun SaveArea(isEditing: Boolean, saving: Boolean, onSave: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    Column(Modifier.padding(top = spacing.x16, bottom = spacing.x24)) {
        Button(
            onClick = onSave,
            enabled = !saving,
            shape = ObolTheme.shapes.buttonLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
                disabledContainerColor = colors.accentSurface,
                disabledContentColor = colors.accentText,
            ),
            modifier = Modifier.fillMaxWidth().height(spacing.primaryButtonHeight),
        ) {
            Text(
                text = stringResource(if (isEditing) R.string.plan_save_edit else R.string.plan_save),
                style = ObolTheme.typography.button,
            )
        }
        Text(
            text = stringResource(R.string.plan_footer),
            style = ObolTheme.typography.captionSmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.x10),
        )
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = ObolTheme.typography.caption,
        color = ObolTheme.colors.warning,
        modifier = Modifier.padding(horizontal = ObolTheme.spacing.x4),
    )
}

@Composable
private fun amountPerCycle(form: PlanForm, cents: Int?): String =
    stringResource(
        R.string.plan_amount_per_cycle,
        cents?.let { ObolFormat.money(it) } ?: ObolFormat.currencySymbol(),
        cycleSuffix(form.cycle, form.cycleDays),
    )

@Composable
private fun promoCyclesLabel(cycle: BillingCycle, count: Int): String {
    val plural = when (cycle) {
        BillingCycle.WEEKLY -> R.plurals.promo_cycles_weekly
        BillingCycle.MONTHLY -> R.plurals.promo_cycles_monthly
        BillingCycle.QUARTERLY -> R.plurals.promo_cycles_quarterly
        BillingCycle.SEMIANNUAL -> R.plurals.promo_cycles_semiannual
        BillingCycle.YEARLY -> R.plurals.promo_cycles_yearly
        BillingCycle.CUSTOM_DAYS -> R.plurals.promo_cycles_custom
    }
    return pluralStringResource(plural, count, count)
}

