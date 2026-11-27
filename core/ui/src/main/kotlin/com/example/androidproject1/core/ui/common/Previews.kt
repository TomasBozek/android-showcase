package com.example.androidproject1.core.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Renders a full screen in the three shapes that differ, in the IDE — a screen has no golden,
 * so this is what a screen is looked at in before the emulator.
 *
 * The large-font variant is not decoration: the system scale goes to 200 %, and a layout that only
 * works at 100 % is a layout that breaks for a large share of the people using it. Catching it in
 * the preview is cheaper than catching it in a report.
 *
 * **Three, not five.** A narrow phone (360 dp against the pixel_7's 411) was previewed too and
 * looked like the phone again. A tablet variant was dropped for the same reason and is back as
 * [TabletPreview], which a screen adds for itself: the scaffold caps its content column
 * at `AppTheme.density.contentMaxWidth`, so every screen does respond to width and the question is
 * which ones are worth looking at — a form, a settings list, anything whose controls were at the
 * bezel. Not every screen, which is what made the old blanket variant say nothing.
 */
@Preview(name = "Phone", showBackground = true, device = "id:pixel_7")
@Preview(name = "Dark", showBackground = true, uiMode = 0x21)
@Preview(name = "Large font", showBackground = true, fontScale = 1.5f)
annotation class ScreenPreview

/**
 * The same screen in an Expanded window, where the content column is capped and centred.
 *
 * Added beside [ScreenPreview] on a screen whose layout is worth checking at a width — a form, a
 * settings list, a sign-in — not on every screen: the cap applies to all of them and a preview per
 * screen saying "still centred" is a preview nobody reads. A screen that responds at a *particular*
 * width still writes its own `@Preview` naming that width, as `InventoryDetailScreen` does.
 *
 * The device is a real tablet rather than a `spec:`, so the preview is the window a person has.
 */
@Preview(name = "Tablet", showBackground = true, device = "id:pixel_tablet")
annotation class TabletPreview

/**
 * Renders a single component light and dark — and these are the goldens: `:core:ui`'s
 * `PreviewScreenshotTest` records one image per variant, and no other module records any.
 *
 * No large-font variant: the playground renders any component at 1.5× on demand, and the
 * touch-target floor is held by every screen test's `is accessible` case, so a third image per
 * component would say what those already say.
 */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = 0x21)
annotation class ComponentPreview

@Composable
fun ThemedScreenPreview(content: @Composable () -> Unit) {
    AppTheme {
        Surface { content() }
    }
}

@Composable
fun ThemedComponentPreview(content: @Composable ColumnScope.() -> Unit) {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}
