package com.example.androidproject1.service.core.ui.test

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.androidproject1.service.core.ui.accessibility.MinTouchTargetKey
import org.junit.Assert.fail

/**
 * The two accessibility facts a screen reader and a thumb need, held over everything on screen:
 * **every clickable has a name** — a content description, text or an editable value in its
 * merged semantics — and **every clickable is laid out at least the theme's minimum touch target**
 * in both directions. A checkbox with no name and a 40 dp key were found by a reviewer or
 * by nobody for weeks; every `XScreenTest` now calls this once, in one test:
 *
 * ```
 * @Test
 * fun `is accessible`() {
 *     render(LoginState.PREVIEW)
 *     compose.assertAccessible()
 * }
 * ```
 *
 * The minimum is read from the theme the test composed — `MinTouchTargetKey`, which `AppTheme`
 * publishes from `AppTheme.density.minTouchTarget` — so a pointer density with a 40 dp target is
 * not a failure and a tablet's 56 dp is held. A composition that publishes none is held to
 * [FALLBACK_MIN_TOUCH_TARGET], the touch phone.
 *
 * Contrast is not repeated here: `ContrastTest` holds every role pair by machine already. Every
 * offender is listed at once, by test tag where it has one, so one run says everything a screen
 * is missing.
 */
fun ComposeTestRule.assertAccessible() {
    val minimum = onAllNodes(SemanticsMatcher.keyIsDefined(MinTouchTargetKey))
        .fetchSemanticsNodes()
        .firstOrNull()
        ?.config
        ?.get(MinTouchTargetKey)
        ?: FALLBACK_MIN_TOUCH_TARGET
    val minimumPx = with(density) { minimum.toPx() }

    val problems = onAllNodes(hasClickAction())
        .fetchSemanticsNodes()
        .filter { it.layoutInfo.isPlaced }
        .flatMap { node ->
            buildList {
                if (!node.hasName()) add("${node.describe()} is clickable and has no name")
                // The laid-out size, not `touchBoundsInRoot`: Compose pads every pointer target
                // to the platform's 48 dp for hit-testing, a fallback that stops working the
                // moment two small targets sit side by side — and it is clipped by every
                // scrolling ancestor, so a row below the fold reads as 0 × 0. The size a
                // component laid out is the size a thumb gets.
                val width = node.size.width.toFloat()
                val height = node.size.height.toFloat()
                if (width + TOLERANCE_PX < minimumPx || height + TOLERANCE_PX < minimumPx) {
                    val widthDp = with(density) { width.toDp() }
                    val heightDp = with(density) { height.toDp() }
                    add(
                        "${node.describe()} is ${widthDp.short()} × ${heightDp.short()}, " +
                            "under the ${minimum.short()} touch target",
                    )
                }
            }
        }

    if (problems.isNotEmpty()) {
        fail(
            "${problems.size} accessibility problem(s) on screen (minimum touch target ${minimum.short()}):\n" +
                problems.joinToString("\n") { "  - $it" },
        )
    }
}

/** A phone with no theme in the composition: the touch target Material and this system agree on. */
val FALLBACK_MIN_TOUCH_TARGET: Dp = 48.dp

// Layout rounds a 48 dp target to whole pixels on either side; half a pixel is not a smaller key.
private const val TOLERANCE_PX = 0.5f

private fun SemanticsNode.hasName(): Boolean {
    val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    val text = config.getOrNull(SemanticsProperties.Text).orEmpty()
    val editable = config.getOrNull(SemanticsProperties.EditableText)
    return description.any { it.isNotBlank() } ||
        text.any { it.text.isNotBlank() } ||
        editable?.text?.isNotBlank() == true
}

/** By its own tag, else by its text and the nearest tagged ancestor — enough to find it in the screen. */
private fun SemanticsNode.describe(): String {
    val tag = config.getOrNull(SemanticsProperties.TestTag)
    val role = config.getOrNull(SemanticsProperties.Role)?.let { " ($it)" }.orEmpty()
    val text = config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
        ?: config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
    val ancestor = generateSequence(parent) { it.parent }
        .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
        .firstOrNull()
        ?.let { " inside '$it'" }
        .orEmpty()
    return when {
        tag != null -> "'$tag'$role"
        text != null -> "\"$text\"$role$ancestor"
        else -> "an untagged ${role.ifEmpty { " clickable" }.trim()} at ${boundsInRoot.topLeft}$ancestor"
    }
}

private fun Dp.short(): String = "${value.toInt()} dp"
