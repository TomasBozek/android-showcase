package com.example.androidproject1.service.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.androidproject1.service.core.ui.R
import com.example.androidproject1.service.core.ui.state.AlertAnswer
import com.example.androidproject1.service.core.ui.state.AlertInput
import com.example.androidproject1.service.core.ui.state.AlertState
import com.example.androidproject1.service.core.ui.state.ContentState
import com.example.androidproject1.service.core.ui.state.LoadingState
import com.example.androidproject1.service.core.ui.text.resolve
import com.example.androidproject1.service.core.ui.text.toUiText
import androidx.compose.material3.SnackbarHost as MaterialSnackbarHost
import androidx.compose.material3.Surface as MaterialSurface

/** The dialog itself, so a flow can assert one is open without naming its copy. */
public const val ALERT_DIALOG_TAG: String = "alert_dialog"

public const val ALERT_CONFIRM_TAG: String = "alert_confirmButton"

public const val ALERT_DECLINE_TAG: String = "alert_declineButton"

/** The field an `AlertInput.Text` is typed into. */
public const val ALERT_ANSWER_TAG: String = "alert_answerField"

/** The row of an `AlertInput.Choice` option, by its id — `alert_item_groceries`. */
public fun alertItemTag(id: String): String = "alert_item_$id"

/**
 * The four things [Screen] draws that are not the feature's content — and the seam that lets an app
 * draw them in its own design system.
 *
 * `Screen()` renders a base surface, a loading overlay, an alert dialog and empty/error content
 * centrally, so no feature reimplements a spinner or a "couldn't load" layout. That is the right
 * design and it left one thing wrong: those four were raw Material with `dp` literals in them,
 * which is the exact thing the design system forbids every feature. `:service:core:ui` may
 * not name `:core:*` — it is copied out and reused — so it could not read `AppTheme` to fix it.
 *
 * This is the way out that costs nothing at either end. `:core:ui` already depends on this module
 * and every screen already composes inside `AppTheme`, so the theme installs an implementation
 * built from `App*` components and `Screen()` reads it. [DefaultScreenChrome] below is what the
 * module does on its own, so `service/` still stands up when it is copied into a project that has
 * no design system yet.
 *
 * Implementations are objects, not lambdas, and the local is `static`: the chrome does not change
 * for the life of a composition, and reading it should not subscribe anything to a snapshot.
 */
@Stable
public interface ScreenChrome {

    /** The base surface every screen sits on. */
    @Composable
    public fun Surface(modifier: Modifier, content: @Composable BoxScope.() -> Unit)

    /**
     * A failure or an empty state, shown **instead of** the content — the whole window, for a
     * screen that has no data to draw a shell with. Over a screen that has data, `Screen()` hands
     * the same message to the shell through [LocalContentMessage] instead.
     */
    @Composable
    public fun ContentMessage(state: ContentState, onAction: () -> Unit, modifier: Modifier)

    /**
     * A blocking overlay over the content, composed always and shown while [state] is non-null —
     * so an implementation can fade it in and out rather than have it appear. With `null`
     * and nothing in flight it draws nothing.
     */
    @Composable
    public fun LoadingOverlay(state: LoadingState?, modifier: Modifier)

    /**
     * The host the app's messages are drawn in — the outlet's, a view model's
     * `UiCommand.ShowSnackbar` among them.
     *
     * The [hostState] is Material's, and stays Material's: it is the queue `Screen()` drives with a
     * suspending `showSnackbar`, not something drawn. Only the row it renders is the design
     * system's business, which is what this hands over.
     */
    @Composable
    public fun SnackbarHost(hostState: SnackbarHostState, modifier: Modifier)

    /**
     * A dialog over the content. Whatever draws it must carry [ALERT_DIALOG_TAG],
     * [ALERT_CONFIRM_TAG] and [ALERT_DECLINE_TAG] and must set `testTagsAsResourceId` on its own
     * window — a dialog does not inherit the scaffold's, so without it the tags exist in Compose's
     * tree and are invisible to anything driving the device.
     *
     * With an [AlertState.input] it asks for a word or a choice: the field carries
     * [ALERT_ANSWER_TAG], each option [alertItemTag], a decline is always offered, and [onConfirm]
     * is handed the answer — `null` for a yes-or-no alert.
     */
    @Composable
    public fun AlertDialog(
        state: AlertState,
        onConfirm: (AlertAnswer?) -> Unit,
        onDecline: () -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier,
    )
}

public val LocalScreenChrome: ProvidableCompositionLocal<ScreenChrome> =
    staticCompositionLocalOf<ScreenChrome> { DefaultScreenChrome }

/**
 * What this module draws with nothing else present: stock Material, and the only place in the
 * repository allowed to have `dp` literals in a screen's chrome.
 *
 * It is not what this app uses — `:core:ui` installs its own — and it is not dead code either. It
 * is what makes `export_service.py` produce a directory that compiles and runs on the first day of
 * a project that has no theme yet.
 */
internal object DefaultScreenChrome : ScreenChrome {

    @Composable
    override fun Surface(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
        // Material's, by its alias: inside this object a bare `Surface(...)` resolves to the member
        // above it and recurses until the stack is gone — which nothing saw until `ScreenTest`
        // composed `Screen()` with this chrome.
        MaterialSurface(modifier = modifier) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }

    @Composable
    override fun ContentMessage(state: ContentState, onAction: () -> Unit, modifier: Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                // Shown instead of the content, which may or may not have had a Scaffold of its own.
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.title?.let {
                Text(
                    text = it.resolve(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = state.message.resolve(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            state.actionLabel?.let {
                Button(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = it.resolve())
                }
            }
        }
    }

    @Composable
    override fun LoadingOverlay(state: LoadingState?, modifier: Modifier) {
        if (state == null) return
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                // Swallow touches so the screen underneath stays uninteractive while loading.
                .pointerInput(Unit) {},
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(text = state.message.resolve())
        }
    }

    @Composable
    override fun SnackbarHost(hostState: SnackbarHostState, modifier: Modifier) {
        MaterialSnackbarHost(hostState = hostState, modifier = modifier)
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
        if (state.input != null) {
            key(state.id) { InputAlertDialog(state, state.input, onConfirm, onDecline, onDismiss, modifier) }
            return
        }
        AlertDialog(
            // Tagged here rather than per screen: every confirm-then-act dialog in the app is this
            // one, and a flow that taps "Log out" by text hits the button behind the dialog when
            // the screen offers the same word — which is exactly what qa.2 found.
            modifier = modifier
                .semantics { testTagsAsResourceId = true }
                .testTag(ALERT_DIALOG_TAG),
            onDismissRequest = { if (state.dismissible) onDismiss() },
            title = state.title?.let { { Text(text = it.resolve()) } },
            text = state.message?.let { { Text(text = it.resolve()) } },
            confirmButton = {
                TextButton(onClick = { onConfirm(null) }, modifier = Modifier.testTag(ALERT_CONFIRM_TAG)) {
                    Text(text = state.confirmLabel.resolve())
                }
            },
            dismissButton = state.declineLabel?.let { label ->
                {
                    TextButton(onClick = onDecline, modifier = Modifier.testTag(ALERT_DECLINE_TAG)) {
                        Text(text = label.resolve())
                    }
                }
            },
        )
    }

    /** A word or a choice, in stock Material: a text field, or radio or checkbox rows. */
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    @Composable
    private fun InputAlertDialog(
        state: AlertState,
        input: AlertInput,
        onConfirm: (AlertAnswer?) -> Unit,
        onDecline: () -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier,
    ) {
        var text by rememberSaveable { mutableStateOf((input as? AlertInput.Text)?.initial.orEmpty()) }
        var chosen by rememberSaveable { mutableStateOf((input as? AlertInput.Choice)?.initial.orEmpty().toList()) }
        val error = (input as? AlertInput.Text)?.validator?.invoke(text)
        val answer: AlertAnswer
        val valid: Boolean
        when (input) {
            is AlertInput.Text -> {
                answer = AlertAnswer.Text(text)
                valid = error == null
            }

            is AlertInput.Choice -> {
                answer = AlertAnswer.Choices(input.options.map { it.id }.filter { it in chosen })
                valid = input.multiple || chosen.isNotEmpty()
            }
        }
        AlertDialog(
            modifier = modifier
                .semantics { testTagsAsResourceId = true }
                .testTag(ALERT_DIALOG_TAG),
            onDismissRequest = { if (state.dismissible) onDismiss() },
            title = state.title?.let { { Text(text = it.resolve()) } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.message?.let { Text(text = it.resolve()) }
                    when (input) {
                        is AlertInput.Text -> OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = input.label?.let { { Text(text = it.resolve()) } },
                            placeholder = input.placeholder?.let { { Text(text = it.resolve()) } },
                            isError = error != null,
                            supportingText = error?.let { { Text(text = it.resolve()) } },
                            singleLine = true,
                            modifier = Modifier.testTag(ALERT_ANSWER_TAG),
                        )

                        is AlertInput.Choice -> input.options.forEach { option ->
                            val on = option.id in chosen
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(alertItemTag(option.id))
                                    .toggleable(value = on) { checked ->
                                        chosen = when {
                                            !input.multiple -> listOf(option.id)
                                            checked -> chosen + option.id
                                            else -> chosen - option.id
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (input.multiple) {
                                    Checkbox(checked = on, onCheckedChange = null)
                                } else {
                                    RadioButton(selected = on, onClick = null)
                                }
                                Text(text = option.label.resolve(), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(answer) },
                    enabled = valid,
                    modifier = Modifier.testTag(ALERT_CONFIRM_TAG),
                ) {
                    Text(text = state.confirmLabel.resolve())
                }
            },
            dismissButton = {
                TextButton(onClick = onDecline, modifier = Modifier.testTag(ALERT_DECLINE_TAG)) {
                    Text(text = (state.declineLabel ?: R.string.core_action_cancel.toUiText()).resolve())
                }
            },
        )
    }

    private const val SCRIM_ALPHA = 0.4f
}
