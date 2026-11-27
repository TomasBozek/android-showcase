package com.example.androidproject1.feature.gallery.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.ActionColors
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.FeedbackColors

/**
 * One colour role: the colour itself, the name a screen asks for it by, and the hex it resolves to.
 *
 * The hex is the point. A role's value is the one thing the layer-2 files say and the screen never
 * sees, so a page that shows the name alone shows nothing a grep could not — and a re-brand is
 * checked by looking at this page in both themes.
 */
@Composable
fun ColourSwatch(
    name: String,
    colour: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The hairline is not decoration: the stage a variant is drawn on is `surfaceSunken`, so
        // that role's own swatch is a hole in the page without it, and `surfaceRaised` reads as
        // one on any light ground. Seen on the emulator before the border was there.
        Box(
            modifier = Modifier
                .size(AppTheme.icons.lg)
                .clip(AppTheme.shapes.sm)
                .background(colour)
                .border(hairline, AppTheme.colors.borderStrong, AppTheme.shapes.sm),
        )
        Column {
            AppText(text = name, role = TextRole.Body)
            AppText(text = colour.hex(), role = TextRole.Secondary)
        }
    }
}

/**
 * An action family as what it actually is: a key with its own fixed bottom edge, its label on it,
 * and the soft container pair beside it. Six roles share this shape; the feedback roles are kept out of it.
 */
@Composable
fun ActionSwatch(
    name: String,
    colours: ActionColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
    ) {
        AppText(text = name, role = TextRole.Label)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm)) {
            SwatchChip(text = "bg", background = colours.bg, foreground = colours.label)
            SwatchChip(text = "edge", background = colours.edge, foreground = colours.label)
            SwatchChip(text = "container", background = colours.container, foreground = colours.onContainer)
        }
        AppText(text = colours.bg.hex() + " · " + colours.edge.hex(), role = TextRole.Secondary)
    }
}

/** A feedback family: the container, the text on it, and the one accent a dot or an icon takes. */
@Composable
fun FeedbackSwatch(
    name: String,
    colours: FeedbackColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
    ) {
        AppText(text = name, role = TextRole.Label)
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm)) {
            SwatchChip(text = "container", background = colours.container, foreground = colours.onContainer)
            SwatchChip(text = "accent", background = colours.accent, foreground = colours.container)
        }
        AppText(text = colours.container.hex() + " · " + colours.accent.hex(), role = TextRole.Secondary)
    }
}

@Composable
private fun SwatchChip(text: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .clip(AppTheme.shapes.sm)
            .background(background)
            .border(hairline, AppTheme.colors.borderStrong, AppTheme.shapes.sm)
            .padding(horizontal = AppTheme.spacing.inset.md, vertical = AppTheme.spacing.inset.sm),
    ) {
        AppText(text = text, role = TextRole.LabelSmall, color = foreground)
    }
}

/**
 * One pixel, in whatever the device's density is — asked for rather than written, because a
 * feature may not name a dp.
 */
private val hairline: Dp
    @Composable get() = with(LocalDensity.current) { 1.toDp() }

/** `#RRGGBB`, upper case — the form the theme files and the design project both write. */
private fun Color.hex(): String = "#%06X".format(toArgb() and HEX_MASK)

private const val HEX_MASK = 0xFFFFFF

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    ColourSwatch(name = "textPrimary", colour = AppTheme.colors.textPrimary)
    ActionSwatch(name = "confirm", colours = AppTheme.colors.confirm)
    FeedbackSwatch(name = "feedback.error", colours = AppTheme.colors.feedback.error)
}
