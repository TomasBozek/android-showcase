package com.example.androidproject1.core.ui.component.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.overlay.AppChoiceDialog
import com.example.androidproject1.core.ui.component.overlay.AppConfirmDialog
import com.example.androidproject1.core.ui.component.overlay.AppDialog
import com.example.androidproject1.core.ui.component.overlay.AppPromptDialog
import com.example.androidproject1.core.ui.component.overlay.AppToast
import com.example.androidproject1.core.ui.component.overlay.ChoiceOption
import com.example.androidproject1.core.ui.component.overlay.ToastTone
import com.example.androidproject1.core.ui.component.status.AppSpinner
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.service.core.ui.component.ALERT_ANSWER_TAG
import com.example.androidproject1.service.core.ui.component.ALERT_CONFIRM_TAG
import com.example.androidproject1.service.core.ui.component.ALERT_DECLINE_TAG
import com.example.androidproject1.service.core.ui.component.ALERT_DIALOG_TAG
import com.example.androidproject1.service.core.ui.component.MessageKind
import com.example.androidproject1.service.core.ui.component.MessageVisuals
import com.example.androidproject1.service.core.ui.component.ScreenChrome
import com.example.androidproject1.service.core.ui.component.alertItemTag
import com.example.androidproject1.service.core.ui.state.AlertAnswer
import com.example.androidproject1.service.core.ui.state.AlertInput
import com.example.androidproject1.service.core.ui.state.AlertState
import com.example.androidproject1.service.core.ui.state.ContentState
import com.example.androidproject1.service.core.ui.state.LoadingState
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.core.ui.text.toUiText
import androidx.compose.material3.SnackbarHost as MaterialSnackbarHost

/**
 * This app's answer to [ScreenChrome]: the four things `Screen()` draws, drawn out of the
 * design system instead of out of Material.
 *
 * Installed by `AppTheme`, so a screen gets it by being a screen. Nothing here is new UI — the
 * empty state is [AppEmptyState], the alert is [AppDialog], the spinner is [AppSpinner] — which is
 * the point: the chrome and the content now go out of date together, and a re-brand reaches the
 * loading overlay the same way it reaches everything else.
 */
object AppScreenChrome : ScreenChrome {

    @Composable
    override fun Surface(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppTheme.colors.surfaceBase),
            content = content,
        )
    }

    @Composable
    override fun ContentMessage(state: ContentState, onAction: () -> Unit, modifier: Modifier) {
        ContentMessageBody(
            state = state,
            onAction = onAction,
            modifier = modifier
                .fillMaxSize()
                // Shown instead of the whole screen, scaffold included, so it pads itself. Inside
                // a scaffold the body below is composed instead, and the scaffold has padded.
                .safeDrawingPadding(),
        )
    }

    @Composable
    override fun LoadingOverlay(state: LoadingState?, modifier: Modifier) {
        // The scrim fades on `fadeMillis`, and keeps the last message for the way out —
        // a scrim whose words vanished a frame before it did would read as two things leaving.
        var shown by remember { mutableStateOf(state) }
        if (state != null) shown = state
        val motion = AppTheme.motion
        AnimatedVisibility(
            visible = state != null,
            enter = fadeIn(tween(motion.fadeMillis, easing = motion.toggleEasing)),
            exit = fadeOut(tween(motion.fadeMillis, easing = motion.toggleEasing)),
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.scrim.copy(alpha = AppTheme.colors.scrimAlpha))
                    // Swallow touches so the screen underneath stays uninteractive while loading.
                    .pointerInput(Unit) {},
                verticalArrangement = Arrangement.spacedBy(
                    AppTheme.spacing.stack.md,
                    Alignment.CenterVertically,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppSpinner(color = AppTheme.colors.textOnScrim)
                // On the scrim, not on the surface. `textOnScrim` is the one role that does not
                // flip between the themes, because the scrim does not either.
                AppText(
                    text = (shown ?: LoadingState()).message.resolve(),
                    role = TextRole.Body,
                    color = AppTheme.colors.textOnScrim,
                )
            }
        }
    }

    @Composable
    override fun SnackbarHost(hostState: SnackbarHostState, modifier: Modifier) {
        MaterialSnackbarHost(hostState = hostState, modifier = modifier) { data ->
            val visuals = data.visuals
            // A toast wears its tone; a snackbar is an offer and is drawn plain. Visuals
            // that are not a message's — nothing posts those today — are drawn as a toast would be.
            val snackbar = (visuals as? MessageVisuals)?.kind == MessageKind.Snackbar || visuals.actionLabel != null
            AppToast(
                message = visuals.message,
                tone = if (snackbar) ToastTone.Neutral else (visuals as? MessageVisuals)?.tone ?: ToastTone.Neutral,
                actionLabel = visuals.actionLabel,
                onAction = data::performAction,
                onDismiss = if (visuals.withDismissAction) data::dismiss else null,
                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.md),
            )
        }
    }

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Composable
    override fun AlertDialog(
        state: AlertState,
        onConfirm: (AlertAnswer?) -> Unit,
        onDecline: () -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier,
    ) {
        // A dialog is its own window, so it does not inherit the scaffold's setting — without
        // this the tags below exist in Compose's tree and are invisible to anything driving the
        // device.
        val dialogModifier = modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(ALERT_DIALOG_TAG)
        val declineLabel = state.declineLabel
        // A word or a choice: the two question dialogs, keyed by the alert so a second
        // question does not open on the first one's typing.
        when (val input = state.input) {
            is AlertInput.Text -> key(state.id) {
                AppPromptDialog(
                    title = state.title?.resolve().orEmpty(),
                    message = state.message?.resolve(),
                    confirmLabel = state.confirmLabel.resolve(),
                    onConfirm = { onConfirm(AlertAnswer.Text(it)) },
                    onDismiss = onDecline,
                    onDismissRequest = { if (state.dismissible) onDismiss() },
                    label = input.label?.resolve(),
                    placeholder = input.placeholder?.resolve(),
                    initialValue = input.initial,
                    validator = input.validator,
                    dismissLabel = declineLabel?.resolve() ?: stringResource(R.string.app_dialog_cancel),
                    fieldTestTag = ALERT_ANSWER_TAG,
                    confirmTestTag = ALERT_CONFIRM_TAG,
                    dismissTestTag = ALERT_DECLINE_TAG,
                    modifier = dialogModifier,
                )
                return
            }

            is AlertInput.Choice -> key(state.id) {
                AppChoiceDialog(
                    title = state.title?.resolve().orEmpty(),
                    message = state.message?.resolve(),
                    options = input.options.map { ChoiceOption(it.id, it.label.resolve()) },
                    confirmLabel = state.confirmLabel.resolve(),
                    onConfirm = { onConfirm(AlertAnswer.Choices(it)) },
                    onDismiss = onDecline,
                    onDismissRequest = { if (state.dismissible) onDismiss() },
                    multiple = input.multiple,
                    initialSelection = input.initial,
                    dismissLabel = declineLabel?.resolve() ?: stringResource(R.string.app_dialog_cancel),
                    optionTestTag = ::alertItemTag,
                    confirmTestTag = ALERT_CONFIRM_TAG,
                    dismissTestTag = ALERT_DECLINE_TAG,
                    modifier = dialogModifier,
                )
                return
            }

            null -> Unit
        }
        if (declineLabel != null) {
            // Two buttons is a confirmation, and a confirmation is AppConfirmDialog's: the
            // destructive gap between the two, and the confirm in the destructive colour when the
            // state says it cannot be undone — a delete, a log-out — rather than an OK's.
            AppConfirmDialog(
                title = state.title?.resolve().orEmpty(),
                message = state.message?.resolve(),
                confirmLabel = state.confirmLabel.resolve(),
                dismissLabel = declineLabel.resolve(),
                onConfirm = { onConfirm(null) },
                onDismiss = onDecline,
                onDismissRequest = { if (state.dismissible) onDismiss() },
                destructive = state.destructive,
                confirmTestTag = ALERT_CONFIRM_TAG,
                dismissTestTag = ALERT_DECLINE_TAG,
                modifier = dialogModifier,
            )
        } else {
            AppDialog(
                modifier = dialogModifier,
                title = state.title?.resolve().orEmpty(),
                message = state.message?.resolve(),
                onDismiss = { if (state.dismissible) onDismiss() },
                actions = {
                    AppButton(
                        label = state.confirmLabel.resolve(),
                        onClick = { onConfirm(null) },
                        modifier = Modifier.testTag(ALERT_CONFIRM_TAG),
                    )
                },
            )
        }
    }
}

/**
 * The two chrome elements a preview can see. The alert dialog and the snackbar host draw in windows
 * of their own, so they are covered by `OverlayScreenshotTest` in this module instead.
 */
/**
 * The chrome's message, drawn wherever the caller puts it and padded by nothing.
 *
 * [AppScreenChrome.ContentMessage] wraps it full-screen for a screen with no data; `AppScaffold`
 * composes it in its content column, under the top bar, when `Screen()` has set
 * `LocalContentMessage` over a screen that has data — so the message keeps the screen's shell.
 */
@Composable
internal fun ContentMessageBody(state: ContentState, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AppEmptyState(
            // ContentState.Empty carries no title by default and AppEmptyState wants one; the
            // message is the sentence that matters either way, so it moves up rather than
            // inventing a heading nobody wrote.
            title = state.title?.resolve() ?: state.message.resolve(),
            message = if (state.title != null) state.message.resolve() else "",
            actionLabel = state.actionLabel?.resolve(),
            onAction = onAction.takeIf { state.actionLabel != null },
            modifier = Modifier
                .wrapContentHeight()
                .testTag(CONTENT_MESSAGE_TAG),
        )
    }
}

/** The chrome's message wherever it draws, so a flow can assert one is up without naming its copy. */
const val CONTENT_MESSAGE_TAG = "content_empty"

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.lg)) {
        Box(modifier = Modifier.height(PREVIEW_ELEMENT_HEIGHT)) {
            AppScreenChrome.ContentMessage(
                state = ContentState.Error(message = "Nothing came back. Try again.".toUiText()),
                onAction = {},
                modifier = Modifier,
            )
        }
        Box(modifier = Modifier.height(PREVIEW_ELEMENT_HEIGHT)) {
            AppScreenChrome.LoadingOverlay(
                state = LoadingState(message = "Saving the photo".toUiText()),
                modifier = Modifier,
            )
        }
    }
}

private val PREVIEW_ELEMENT_HEIGHT = 200.dp
