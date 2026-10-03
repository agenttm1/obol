package com.tmstudio.obol.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.tmstudio.obol.ui.theme.ObolTheme

/** Donja navigacija prema `docs/mockups/Main.html`. */
@Composable
fun ObolBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ObolTheme.colors
    val spacing = ObolTheme.spacing
    Column(modifier.fillMaxWidth().background(colors.navBg)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(spacing.borderWidth)
                .background(colors.divider)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = spacing.navPaddingHorizontal,
                    end = spacing.navPaddingHorizontal,
                    top = spacing.navPaddingTop,
                    bottom = spacing.navPaddingBottom,
                )
                .selectableGroup(),
        ) {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == selected
                val tint = if (isSelected) colors.accent else colors.textSecondary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = spacing.minTouchTarget)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(destination) },
                            role = Role.Tab,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        spacing.navIconLabelGap,
                        Alignment.CenterVertically,
                    ),
                ) {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(spacing.navIconSize),
                    )
                    Text(
                        text = stringResource(destination.labelRes),
                        style = ObolTheme.typography.navLabel,
                        color = tint,
                    )
                }
            }
        }
    }
}
