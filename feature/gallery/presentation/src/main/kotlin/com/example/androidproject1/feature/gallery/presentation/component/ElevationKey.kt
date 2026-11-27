package com.example.androidproject1.feature.gallery.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.PressTravel
import com.example.androidproject1.core.ui.theme.keySurface

/**
 * One elevation level, drawn as the thing it is for: a key whose edge is that level, that really
 * presses.
 *
 * `Modifier.shadow` is not what elevation is here — the edge is a hard bottom border that shortens
 * under a finger, so the only honest page for a level is one that can be held down. The key does
 * nothing when tapped on purpose: it is the press that is being shown, not an action.
 */
@Composable
fun ElevationKey(
    name: String,
    level: Dp,
    usedFor: String,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(AppTheme.density.minTouchTarget * KEY_WIDTHS)
                .height(AppTheme.density.minTouchTarget)
                .clickable(interactionSource = interaction, indication = null, onClick = {})
                .keySurface(
                    color = AppTheme.colors.neutral.bg,
                    edge = AppTheme.colors.neutral.edge,
                    shape = AppTheme.shapes.md,
                    edgeHeight = level,
                    pressed = pressed,
                ),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text = name, role = TextRole.LabelSmall)
        }
        Column {
            AppText(text = "${level.value.toInt()} dp · $usedFor", role = TextRole.Secondary)
            AppText(text = "press travels ${PressTravel.value.toInt()} dp", role = TextRole.Tertiary)
        }
    }
}

/** A key and a half wide — the proportion a medium button actually has. */
private const val KEY_WIDTHS = 1.5f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    ElevationKey(name = "level2", level = AppTheme.elevation.level2, usedFor = "key, button")
    ElevationKey(name = "level4", level = AppTheme.elevation.level4, usedFor = "dialog, sheet")
}
