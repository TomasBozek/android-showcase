package com.example.androidproject1.core.ui.component.content

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Nothing here — and what to do about it.
 *
 * [message] says why it is empty and [actionLabel] offers the first step. "Nothing to show" on its
 * own is the one thing an empty state must never be: it leaves the person exactly where they were.
 *
 * It arrives rather than appearing: on first composition the icon grows from [ENTRANCE_SCALE] and
 * the whole state fades and lifts, over `toggleMillis`. Once per screen entry — the effect keys on
 * `Unit`, so a recomposition or a tab return does not replay it, and a list that empties while you
 * are looking at it is not a thing that jumps. At zero (reduced motion) and in inspection
 * mode it is simply drawn, so a preview and a golden are the settled frame the way they are for
 * `appStaggeredEntrance`.
 */
@Composable
fun AppEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    /** The action's own id — the caller's [modifier] tags the whole state, and a flow taps the button. */
    actionTestTag: String? = null,
) {
    val toggleMillis = AppTheme.motion.toggleMillis
    val easing = AppTheme.motion.toggleEasing
    val still = toggleMillis == 0 || LocalInspectionMode.current
    val lift = with(LocalDensity.current) { AppTheme.spacing.stack.sm.toPx() }
    val progress = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!still) progress.animateTo(1f, tween(toggleMillis, easing = easing))
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress.value
                translationY = lift * (1f - progress.value)
            }
            .padding(AppTheme.spacing.inset.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.textTertiary,
                modifier = Modifier
                    .size(AppTheme.icons.lg)
                    .graphicsLayer {
                        // The icon grows a little further than the text lifts, so the eye lands on
                        // it first — it is the one part of an empty state that is not a sentence.
                        val scale = ENTRANCE_SCALE + (1f - ENTRANCE_SCALE) * progress.value
                        scaleX = scale
                        scaleY = scale
                    },
            )
        }
        AppText(text = title, role = TextRole.Title)
        AppText(text = message, role = TextRole.Secondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            AppButton(
                label = actionLabel,
                onClick = onAction,
                modifier = if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier,
            )
        }
    }
}

/** Where the icon starts: felt as arriving, not seen as a resize. */
private const val ENTRANCE_SCALE = 0.8f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppEmptyState(
        title = "No open orders",
        message = "Orders you start will show up here until they are paid.",
        actionLabel = "New order",
        onAction = {},
    )
}
