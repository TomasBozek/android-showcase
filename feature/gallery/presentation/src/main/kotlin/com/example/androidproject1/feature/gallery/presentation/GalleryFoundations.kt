package com.example.androidproject1.feature.gallery.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.choice.AppSwitch
import com.example.androidproject1.core.ui.component.content.AppReveal
import com.example.androidproject1.core.ui.component.content.AppSwitcher
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.AppSkeleton
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.gallery.presentation.component.ActionSwatch
import com.example.androidproject1.feature.gallery.presentation.component.ColourSwatch
import com.example.androidproject1.feature.gallery.presentation.component.ElevationKey
import com.example.androidproject1.feature.gallery.presentation.component.FeedbackSwatch
import com.example.androidproject1.feature.gallery.presentation.component.IconRow
import com.example.androidproject1.feature.gallery.presentation.component.MotionReplay
import com.example.androidproject1.feature.gallery.presentation.component.ShapeTile
import com.example.androidproject1.feature.gallery.presentation.component.SpacingRuler
import com.example.androidproject1.feature.gallery.presentation.component.TypeSpecimen

/**
 * What the seven foundation pages draw.
 *
 * A gallery of components alone shows the leaves: it says what an `AppButton` looks like and never
 * what `confirm` is, what `md` measures or how long `toggleMillis` lasts. Every page here reads the
 * loaded theme rather than restating `Ramp.kt` — so it says what the device is rendering, in the
 * theme it is rendering it in, and the light/dark strip on the detail screen is what makes it a
 * check rather than a picture.
 *
 * Kept out of `GalleryCatalog.kt` because that file is the component catalogue and is long enough.
 */
@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
    ) {
        content()
    }
}

@Composable
internal fun SurfaceRoles() = Page {
    val colors = AppTheme.colors
    ColourSwatch("surfaceSunken", colors.surfaceSunken)
    ColourSwatch("surfaceBase", colors.surfaceBase)
    ColourSwatch("surfaceRaised", colors.surfaceRaised)
    ColourSwatch("surfaceInverse", colors.surfaceInverse)
    ColourSwatch("textOnInverse", colors.textOnInverse)
}

@Composable
internal fun TextRoles() = Page {
    val colors = AppTheme.colors
    ColourSwatch("textPrimary", colors.textPrimary)
    ColourSwatch("textSecondary", colors.textSecondary)
    ColourSwatch("textTertiary", colors.textTertiary)
    ColourSwatch("textDisabled", colors.textDisabled)
}

@Composable
internal fun ActionRoles() = Page {
    val colors = AppTheme.colors
    ActionSwatch("confirm", colors.confirm)
    ActionSwatch("destructive", colors.destructive)
    ActionSwatch("info", colors.info)
    ActionSwatch("selection", colors.selection)
    ActionSwatch("warning", colors.warning)
    ActionSwatch("neutral", colors.neutral)
}

@Composable
internal fun FeedbackRolesPage() = Page {
    val feedback = AppTheme.colors.feedback
    FeedbackSwatch("feedback.success", feedback.success)
    FeedbackSwatch("feedback.warning", feedback.warning)
    FeedbackSwatch("feedback.error", feedback.error)
    FeedbackSwatch("feedback.info", feedback.info)
}

@Composable
internal fun LineRoles() = Page {
    val colors = AppTheme.colors
    ColourSwatch("border", colors.border)
    ColourSwatch("borderStrong", colors.borderStrong)
    ColourSwatch("focusRing", colors.focusRing)
    ColourSwatch("scrim", colors.scrim)
    ColourSwatch("textOnScrim", colors.textOnScrim)
}

@Composable
internal fun TypeRoles() = Page {
    TypeSpecimen(TextRole.DisplayLarge, "1 248,00")
    TypeSpecimen(TextRole.Display, "Order 12 — On its way")
    TypeSpecimen(TextRole.TitleLarge, "Discard these changes?")
    TypeSpecimen(TextRole.Title, "Drinks · Hot drinks")
    TypeSpecimen(TextRole.BodyLarge, "Running text and descriptions.")
    TypeSpecimen(TextRole.Body, "The default size for list rows and dialog copy.")
    TypeSpecimen(TextRole.Label, "Appearance")
    TypeSpecimen(TextRole.LabelSmall, "OPEN ORDERS")
    TypeSpecimen(TextRole.Numeric, "1 234 567,00")
    TypeSpecimen(TextRole.Secondary, "Opened 19:24 · Jana N.")
    TypeSpecimen(TextRole.Tertiary, "—")
}

@Composable
internal fun InsetSteps() = Page {
    val inset = AppTheme.spacing.inset
    SpacingRuler("inset.xs", inset.xs)
    SpacingRuler("inset.sm", inset.sm)
    SpacingRuler("inset.md", inset.md)
    SpacingRuler("inset.lg", inset.lg)
    SpacingRuler("inset.xl", inset.xl)
}

@Composable
internal fun StackSteps() = Page {
    val stack = AppTheme.spacing.stack
    SpacingRuler("stack.xs", stack.xs)
    SpacingRuler("stack.sm", stack.sm)
    SpacingRuler("stack.md", stack.md)
    SpacingRuler("stack.lg", stack.lg)
    SpacingRuler("stack.xl", stack.xl)
    SpacingRuler("destructiveGap", AppTheme.spacing.destructiveGap)
}

@Composable
internal fun InlineSteps() = Page {
    val inline = AppTheme.spacing.inline
    SpacingRuler("inline.xs", inline.xs)
    SpacingRuler("inline.sm", inline.sm)
    SpacingRuler("inline.md", inline.md)
    SpacingRuler("inline.lg", inline.lg)
    SpacingRuler("inline.xl", inline.xl)
}

@Composable
internal fun ShapeRoles() = Page {
    val shapes = AppTheme.shapes
    ShapeTile("none", shapes.none, "table, divider")
    ShapeTile("xs", shapes.xs, "badge, indicator")
    ShapeTile("sm", shapes.sm, "small button, checkbox")
    ShapeTile("md", shapes.md, "key, field, medium button")
    ShapeTile("lg", shapes.lg, "tile, large button")
    ShapeTile("xl", shapes.xl, "card, panel, dialog")
    ShapeTile("sheet", shapes.sheet, "bottom sheet, rounded at the top")
    ShapeTile("pill", shapes.pill, "tag, switch, round button")
}

@Composable
internal fun ElevationLevels() = Page {
    val elevation = AppTheme.elevation
    ElevationKey("level0", elevation.level0, "surface, list row")
    ElevationKey("level1", elevation.level1, "card, tile")
    ElevationKey("level2", elevation.level2, "key, button")
    ElevationKey("level3", elevation.level3, "menu, floating action")
    ElevationKey("level4", elevation.level4, "dialog, sheet")
}

@Composable
internal fun IconSizes() = Page {
    val icons = AppTheme.icons
    IconRow("sm", icons.sm, Icons.Default.Star, "beside text, in a field, in a small button")
    IconRow("md", icons.md, Icons.Default.Star, "the default, and every icon that is the control")
    IconRow("lg", icons.lg, Icons.Default.Delete, "a key, an empty state, the thing being looked at")
}

/**
 * Each token through a component that reads it, rather than through a box this feature animates.
 * The reader is the demo: what moves here is what moves in the app.
 */
@Composable
internal fun MotionTokens() = Page {
    val motion = AppTheme.motion
    MotionReplay("pressMillis", motion.pressMillis, "keySurface, pressFeedback, AppFabMenu's key") {
        // A press is not replayed — it is held. The key travels under the finger on the token.
        AppButton(label = "Hold this key", onClick = {})
    }
    MotionReplay("toggleMillis", motion.toggleMillis, "AppSwitch, AppCheckbox, AppAccordion, AppPager") {
        AppSwitch(checked = it, onCheckedChange = {}, label = "The thumb crosses on the token")
    }
    MotionReplay("sheetEnterMillis", motion.sheetEnterMillis, "a sheet and a dialog coming in") {
        AppReveal(visible = it, slide = true) { AppText("It slides in on the token.") }
    }
    MotionReplay("sheetExitMillis", motion.sheetExitMillis, "a sheet and a dialog going out") {
        AppReveal(visible = !it, slide = true) { AppText("And out on this one.") }
    }
    MotionReplay("fadeMillis", motion.fadeMillis, "AppScrollShadow, a banner, a badge, the scrim") {
        AppReveal(visible = it) { AppText("It appears rather than moves.") }
    }
    MotionReplay("ambientMillis", motion.ambientMillis, "AppSkeleton's sweep, indeterminate progress") {
        // The one loop that never ends: it ignores `playing` and is at rest under reduced motion.
        AppSkeleton(modifier = Modifier.fillMaxWidth())
    }
    MotionReplay("screenMillis", motion.screenMillis, "AppNavHost's push and pop, a shared element") {
        AppSwitcher(target = if (it) 1 else 0) { step ->
            AppText(if (step == 0) "The screen being left" else "The screen arriving")
        }
    }
}
