package com.tmstudio.obol.ui.add

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tmstudio.obol.R
import com.tmstudio.obol.data.db.entity.BillingCycle
import com.tmstudio.obol.data.db.entity.Service
import com.tmstudio.obol.domain.cheapestPlan
import com.tmstudio.obol.ui.components.MonogramTile
import com.tmstudio.obol.ui.components.ScreenHeader
import com.tmstudio.obol.ui.components.cycleSuffix
import com.tmstudio.obol.ui.components.dashedBorder
import com.tmstudio.obol.ui.components.serviceColor
import com.tmstudio.obol.ui.format.ObolFormat
import com.tmstudio.obol.ui.theme.ObolTheme

private const val GRID_COLUMNS = 3

/** Ekran „Dodaj pretplatu" prema `docs/mockups/Dodaj.html`. */
@Composable
fun AddScreen(
    onBack: () -> Unit,
    onServiceSelected: (String) -> Unit,
    onAddManually: () -> Unit,
    viewModel: AddViewModel = viewModel(factory = AddViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val spacing = ObolTheme.spacing

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(title = stringResource(R.string.add_title), onBack = onBack)
        SearchField(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            modifier = Modifier.padding(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.x20,
            ),
        )
        if (state.catalogSize > 0) {
            Text(
                text = pluralStringResource(R.plurals.add_catalog_hint, state.catalogSize, state.catalogSize),
                style = ObolTheme.typography.captionSoft,
                color = ObolTheme.colors.textSecondary,
                modifier = Modifier.padding(
                    start = spacing.screenHorizontal + spacing.x2,
                    end = spacing.screenHorizontal,
                    top = spacing.x10,
                ),
            )
        }
        val resultsTitle = stringResource(R.string.add_section_results)
        val popularTitle = stringResource(R.string.add_section_popular)
        val othersTitle = stringResource(R.string.add_section_other)
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.x20,
                bottom = spacing.x8,
            ),
            horizontalArrangement = Arrangement.spacedBy(spacing.listRowGap),
            verticalArrangement = Arrangement.spacedBy(spacing.listRowGap),
        ) {
            val results = state.results
            if (results != null) {
                if (results.isEmpty()) {
                    fullWidth("no-results") {
                        Text(
                            text = stringResource(R.string.add_no_results, query.trim()),
                            style = ObolTheme.typography.bodySoft,
                            color = ObolTheme.colors.textSecondary,
                        )
                    }
                } else {
                    serviceSection("results", resultsTitle, results, onServiceSelected)
                }
            } else {
                serviceSection("popular", popularTitle, state.popular, onServiceSelected)
                serviceSection("others", othersTitle, state.others, onServiceSelected)
            }
        }
        AddManuallyButton(
            onClick = onAddManually,
            modifier = Modifier.padding(
                start = spacing.screenHorizontal,
                end = spacing.screenHorizontal,
                top = spacing.x16,
                bottom = spacing.x24,
            ),
        )
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.serviceSection(
    key: String,
    title: String,
    services: List<Service>,
    onServiceSelected: (String) -> Unit,
) {
    if (services.isEmpty()) return
    fullWidth("$key-title") {
        Text(
            text = title.uppercase(ObolFormat.locale),
            style = ObolTheme.typography.sectionLabel,
            color = ObolTheme.colors.textSecondary,
            modifier = Modifier.padding(top = ObolTheme.spacing.x4),
        )
    }
    items(services, key = { "$key-${it.id}" }) { service ->
        ServiceTile(service, onClick = { onServiceSelected(service.id) })
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.card
    val label = stringResource(R.string.add_search_label)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.searchFieldHeight)
            .clip(shape)
            .background(colors.surface)
            .border(spacing.borderWidth, colors.borderStrong, shape)
            .padding(horizontal = spacing.x14),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x10),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(spacing.iconSizeSmall),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = ObolTheme.typography.input.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Search,
            ),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.add_search_placeholder),
                            style = ObolTheme.typography.input,
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
private fun ServiceTile(service: Service, onClick: () -> Unit) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val typography = ObolTheme.typography
    val shape = ObolTheme.shapes.tile
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(spacing.borderWidth, colors.border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = spacing.x14, horizontal = spacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.x8),
    ) {
        MonogramTile(
            monogram = service.monogram,
            color = serviceColor(service.colorHex),
            size = spacing.monogramTileSmall,
            shape = ObolTheme.shapes.iconTileSmall,
            style = typography.monogramSmall,
        )
        Text(
            text = service.name,
            style = typography.captionStrong,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            // Uvijek dva retka, da se cijene u istom redu mreže poravnaju.
            minLines = 2,
            maxLines = 2,
        )
        service.cheapestPlan()?.let { plan ->
            val price = ObolFormat.money(plan.priceCents)
            val text = if (plan.cycle == BillingCycle.MONTHLY) {
                price
            } else {
                stringResource(R.string.plan_amount_per_cycle, price, cycleSuffix(plan.cycle, null))
            }
            Text(
                text = stringResource(R.string.add_price_from, text),
                style = typography.badgeSoft,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AddManuallyButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = ObolTheme.spacing
    val colors = ObolTheme.colors
    val shape = ObolTheme.shapes.buttonLarge
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.primaryButtonHeight)
            .clip(shape)
            .background(colors.surface)
            .dashedBorder(spacing.borderWidth, colors.borderStrong, shape, spacing.dashLength, spacing.dashGap)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.x8, Alignment.CenterHorizontally),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(spacing.iconSizeSmall),
        )
        Text(
            text = stringResource(R.string.add_manual),
            style = ObolTheme.typography.sectionTitle,
            color = colors.textPrimary,
        )
    }
}
