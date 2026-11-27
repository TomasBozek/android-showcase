package com.example.androidproject1.feature.gallery.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * One shape role as a tile of the role's own corner, with the name and what it is for.
 *
 * A radius reads at one size and not another, which is why the tile is the width of a key rather
 * than of a swatch: `md` and `lg` are two dp apart and tell apart on a key, not on a chip.
 */
@Composable
fun ShapeTile(
    name: String,
    shape: CornerBasedShape,
    usedFor: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                // A key's own footprint, asked for in roles: a radius that reads on a key does not
                // read on a chip, and a feature may not write a dp.
                .width(AppTheme.density.minTouchTarget * KEY_WIDTHS)
                .height(AppTheme.density.minTouchTarget)
                .clip(shape)
                .background(AppTheme.colors.neutral.container),
        )
        AppText(text = "$name · $usedFor", role = TextRole.Secondary)
    }
}

/** A key and a half wide — the proportion a medium button actually has. */
private const val KEY_WIDTHS = 1.5f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    ShapeTile(name = "md", shape = AppTheme.shapes.md, usedFor = "key, field, medium button")
    ShapeTile(name = "pill", shape = AppTheme.shapes.pill, usedFor = "tag, switch, round button")
}
