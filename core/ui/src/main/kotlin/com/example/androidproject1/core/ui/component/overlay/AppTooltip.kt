package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A label for an icon that has none: shown on a long press, and on hover where there is a
 * pointer, above the thing it names — or below it when there is no room above.
 *
 * It never carries anything you cannot continue without: a tooltip is the explanation of an icon,
 * and every [AppIconButton] wraps itself in one with its `contentDescription`, so the string that
 * makes an icon testable is the one that explains it. Reach for this directly only for something
 * that is not an icon button.
 *
 * @param content the anchor — what is long-pressed or hovered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTooltip(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = {
            // A popup of its own, like a menu: the scaffold's `testTagsAsResourceId` does not
            // reach it, so the bubble carries the line itself.
            TooltipBubble(text = text, modifier = Modifier.semantics { testTagsAsResourceId = true })
        },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}

/** The bubble: a label on the inverse surface, the same box the static tooltip was. */
@Composable
private fun TooltipBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(AppTheme.shapes.sm)
            .background(AppTheme.colors.surfaceInverse)
            .padding(
                horizontal = AppTheme.spacing.inset.md,
                vertical = AppTheme.spacing.inset.sm,
            ),
    ) {
        AppText(text = text, role = TextRole.Label, color = AppTheme.colors.textOnInverse)
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // The bubble itself, since a preview cannot long-press: what the tooltip looks like open.
    TooltipBubble(text = "Split the bill")
    AppTooltip(text = "Favourite") {
        AppIconButton(icon = Icons.Filled.Favorite, contentDescription = "Favourite", onClick = {}, tooltip = false)
    }
}
