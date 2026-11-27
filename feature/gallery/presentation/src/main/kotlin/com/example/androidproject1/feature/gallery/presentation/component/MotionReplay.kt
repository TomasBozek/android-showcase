package com.example.androidproject1.feature.gallery.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppReveal
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * One motion token, shown through a component that actually reads it, replayed by a key.
 *
 * A duration cannot be read off a still page, and every golden this system has is a settled frame
 * — so the only way to know whether `toggleMillis` is the right 140 ms is to watch it. The demo is
 * the real reader rather than a box this file animates: a feature composes motion the way it
 * composes widgets, and a synthetic box would be a second implementation of the token that
 * reduced motion could not reach.
 *
 * [content] is given `playing`, which goes false → true one frame after a replay, so an
 * `AppReveal` or an `AppSwitch` has a state to move to. A reader that loops on its own, or that
 * moves under a finger, ignores it.
 */
@Composable
fun MotionReplay(
    name: String,
    millis: Int,
    usedBy: String,
    modifier: Modifier = Modifier,
    content: @Composable (playing: Boolean) -> Unit,
) {
    var run by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
    ) {
        AppText(text = "$name · $millis ms", role = TextRole.Label)
        AppText(text = usedBy, role = TextRole.Secondary)
        key(run) { Playing(content) }
        ReplayKey { run++ }
    }
}

/**
 * The same replay around a component that moves on first composition — the Motion group's second
 * look at the nine that do.
 *
 * The key throws the content away and composes it again, so whatever it does when it appears
 * happens again, and the gallery never learns what any of them actually does.
 */
@Composable
fun MotionReplay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var run by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
    ) {
        key(run) { content() }
        ReplayKey { run++ }
    }
}

/** False on the frame the content is composed, true on the next — an entrance to animate into. */
@Composable
private fun Playing(content: @Composable (playing: Boolean) -> Unit) {
    var playing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { playing = true }
    content(playing)
}

@Composable
private fun ReplayKey(onClick: () -> Unit) {
    AppButton(
        label = "Replay",
        onClick = onClick,
        kind = ButtonKind.Ghost,
        size = ControlSize.Small,
        modifier = Modifier.testTag("galleryDetail_replayButton"),
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    MotionReplay(name = "fadeMillis", millis = 160, usedBy = "AppScrollShadow, a banner, a badge") {
        AppReveal(visible = it) { AppText(text = "It appears on the token.") }
    }
    MotionReplay { AppText(text = "anything that moves on first composition") }
}
