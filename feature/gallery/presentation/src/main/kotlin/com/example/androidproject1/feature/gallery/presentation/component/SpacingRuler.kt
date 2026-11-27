package com.example.androidproject1.feature.gallery.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * One spacing step, drawn at its own width with the name a screen asks for it by.
 *
 * A step is invisible by definition — it is the gap between two other things — so the bar is the
 * only way to compare `stack.md` with `inline.md` without measuring a screenshot.
 */
@Composable
fun SpacingRuler(
    name: String,
    step: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(step)
                .height(AppTheme.icons.sm)
                .clip(AppTheme.shapes.xs)
                .background(AppTheme.colors.selection.bg),
        )
        AppText(text = "$name · ${step.value.toInt()} dp", role = TextRole.Secondary)
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    SpacingRuler(name = "inset.sm", step = AppTheme.spacing.inset.sm)
    SpacingRuler(name = "stack.lg", step = AppTheme.spacing.stack.lg)
}
