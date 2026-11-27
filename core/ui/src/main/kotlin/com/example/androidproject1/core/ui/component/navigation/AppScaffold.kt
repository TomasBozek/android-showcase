package com.example.androidproject1.core.ui.component.navigation

import android.app.Activity
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.service.core.ui.component.LocalContentMessage
import com.example.androidproject1.service.telemetry.ScreenViewEffect
import java.util.WeakHashMap

/**
 * The shell every screen sits in: the base surface, the system insets, and an optional top bar.
 *
 * The activity draws edge to edge and `Screen()` applies no insets of its own, so this is the one
 * place that pads for them — which is why a screen should not reach for `safeDrawingPadding()`
 * itself and end up padded twice.
 *
 * Content is a `Column`; a screen that needs something else puts a `Box` or a `LazyColumn` inside
 * it. Pass [contentPadding] `false` when the content runs to the edge, as a list does.
 *
 * On a window wider than a phone the content column is capped at `AppTheme.density.contentMaxWidth`
 * and centred, so a form's fields stop short of the bezel and its button stays under them.
 * The cap is the column's *width* and [contentPadding] is its *padding*, so the two are
 * independent: a settings list still runs its rows to the column's edge, and the column is 840 dp
 * rather than 1280. A screen that wants the whole window says so inside, with `fillMaxWidth()` on
 * content the cap does not fight.
 *
 * When `Screen()` has set [LocalContentMessage] — the ViewModel showed a `ContentState` over a
 * screen that has data — the chrome's message is drawn in the content's place and the content is
 * not composed, so the top bar, the up arrow and [screenId] stay. A screen never reads the
 * local itself.
 *
 * @param screenId the screen's own test identifier, which is its name — `"SettingsScreen"`. It is
 *   published as a resource id, so `assertVisible: id: "SettingsScreen"` is the universal check
 *   that a flow is where it meant to be, and it costs a screen one argument. Elements inside use
 *   `Modifier.testTag("settings_permissionsButton")`: the screen's name in camelCase without
 *   `Screen`, an underscore, then the element.
 * @param secure keeps the window out of screenshots, screen recording and the recents thumbnail
 *   (`FLAG_SECURE`) while this screen is composed — for a screen that shows a card number or an
 *   account. Cleared when it leaves, so the next screen can be captured again.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    screenId: String? = null,
    topBar: @Composable (() -> Unit)? = null,
    contentPadding: Boolean = true,
    secure: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    ScreenViewEffect(screenId)
    if (secure) SecureWindowEffect()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.surfaceBase)
            // Publishes every testTag below as a resource id, so one Maestro flow addresses the
            // same ids on Android that Playwright addresses as data-testid on the web.
            .semantics { testTagsAsResourceId = true }
            .then(if (screenId != null) Modifier.testTag(screenId) else Modifier)
            .safeDrawingPadding(),
    ) {
        topBar?.invoke()
        val message = LocalContentMessage.current
        if (message != null) {
            ContentMessageBody(
                state = message.state,
                onAction = message.onAction,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val maxWidth = AppTheme.density.contentMaxWidth
            Column(
                modifier = Modifier
                    // The cap goes first: it narrows the constraints `fillMaxSize` then fills, and
                    // the other order fills the window and caps nothing.
                    .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally)
                    .then(if (contentPadding) Modifier.padding(AppTheme.spacing.inset.xl) else Modifier),
                content = content,
            )
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppScaffold(screenId = "SettingsScreen", topBar = { AppTopBar(title = "Settings") }) {
        AppText(text = "Content sits here, padded and inside the insets.")
    }
}

/**
 * `FLAG_SECURE` on the activity's window for as long as one secure screen is composed.
 *
 * Counted per window, because a transition composes the outgoing screen and the incoming one at
 * once: two secure screens in a row would otherwise see the first one's exit clear the flag the
 * second one set.
 */
@Composable
private fun SecureWindowEffect() {
    val window = generateSequence(LocalContext.current) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>()
        .firstOrNull()
        ?.window
        ?: return
    DisposableEffect(window) {
        val holders = secureHolders.getOrDefault(window, 0)
        if (holders == 0) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        secureHolders[window] = holders + 1
        onDispose {
            val left = secureHolders.getOrDefault(window, 1) - 1
            if (left == 0) {
                secureHolders.remove(window)
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                secureHolders[window] = left
            }
        }
    }
}

/** How many secure screens each window has composed. Main thread only, like composition. */
private val secureHolders = WeakHashMap<Window, Int>()
