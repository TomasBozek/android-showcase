package com.example.androidproject1.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layer 2 — icon sizes. Three, and only three.
 *
 * An icon is a glyph doing a job, and the job is what picks the size: a marker inside a row is
 * [sm], a control the finger goes to is [md], a picture standing on its own is [lg]. Before this
 * there was no role at all, so every `Icon(...)` wrote whatever its call site felt like — 16, 18,
 * 22, 24, 32 — and a screen that wanted a large one had nothing to ask for, because a feature may
 * not write a `.dp`.
 *
 * The icon **set** is a separate question and is settled: Material's icons stay. The design
 * names Lucide, which has no first-party Compose artifact, and the sizes are what actually differ
 * between a tidy screen and an untidy one.
 *
 * These are not touch targets. A tappable icon is wrapped by `AppIconButton`, whose box is
 * `AppTheme.density.minTouchTarget` — the icon inside it stays [md] on a phone and on a tablet.
 */
@Immutable
data class AppIcons(
    /** 18 dp — beside text in a row, in a field, or inside a small button. */
    val sm: Dp = 18.dp,
    /** 24 dp — the default, and every icon that is itself the control. */
    val md: Dp = 24.dp,
    /** 32 dp — a key, an empty state, anywhere the icon is the thing being looked at. */
    val lg: Dp = 32.dp,
)

internal val LocalAppIcons = staticCompositionLocalOf { AppIcons() }

/**
 * The glyphs `material-icons-core` does not have and a screen needs, each drawn once here so a
 * feature never writes a path. `material-icons-extended` is several
 * thousand vectors for these few, which is not a dependency earning its place; each is Material's
 * own path on its 24-unit viewport, so it sits beside every imported icon without a seam.
 */
object AppGlyphs {

    /** Material's `content_copy`: two sheets, the front one outlined. */
    val Copy: ImageVector = glyph(
        name = "Copy",
        path = "M16,1H4C2.9,1 2,1.9 2,3v14h2V3h12V1zM19,5H8C6.9,5 6,5.9 6,7v14c0,1.1 0.9,2 2,2h11c1.1,0 2,-0.9 " +
            "2,-2V7C21,5.9 20.1,5 19,5zM19,21H8V7h11V21z",
    )
}

private fun glyph(name: String, path: String): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black)).build()
