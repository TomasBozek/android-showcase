package com.example.androidproject1.service.core.ui.component

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidproject1.service.core.ui.R
import com.example.androidproject1.service.core.ui.event.AppMessage
import com.example.androidproject1.service.core.ui.event.MessageAction
import com.example.androidproject1.service.core.ui.event.MessageDuration
import com.example.androidproject1.service.core.ui.event.MessageOutlet
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.event.UiEvent
import com.example.androidproject1.service.core.ui.state.ContentState
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.util.CollectEffect
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.navigation.LocalAppNavigator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The chrome's message, handed to the screen's shell rather than drawn over it.
 *
 * A screen that loaded nothing is still that screen: its top bar, its up arrow and its id stay,
 * and the message takes the place of the content alone. [Screen] provides this whenever the
 * ViewModel has set a [ContentState] over a non-null `data`; the design system's scaffold reads it
 * and draws the message in its content column. `null` means there is nothing to say.
 */
@Immutable
public data class ContentMessage(val state: ContentState, val onAction: () -> Unit)

/** Set by [Screen] for the shell underneath it; read by `AppScaffold`. */
public val LocalContentMessage: ProvidableCompositionLocal<ContentMessage?> = compositionLocalOf { null }

/**
 * The [MessageOutlet] every [Screen] renders from and posts a view model's snackbars to.
 * `null` means the application's, [MessageOutlet.of] — the one `:app` binds as `AppMessages` — so
 * the shell provides nothing; a test hands in its own through [ProvideMessageOutlet].
 */
public val LocalMessageOutlet: ProvidableCompositionLocal<MessageOutlet?> =
    staticCompositionLocalOf<MessageOutlet?> { null }

@Composable
internal fun ProvideMessageOutlet(outlet: MessageOutlet, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMessageOutlet provides outlet, content = content)
}

/**
 * Wraps a feature's stateless screen composable.
 *
 * The single place that collects state, interprets [UiCommand]s, renders the loading overlay, the
 * alert dialog and empty/error content, and delivers navigation intents. The [content] lambda
 * therefore only ever receives a non-null state, and a destination never writes a collector.
 *
 * @param onNavigation turns this screen's navigation intents into back-stack calls. Collected
 * only while the UI is at least STARTED; an intent emitted below that is buffered and delivered on
 * resume rather than dropped.
 */
@Composable
public fun <State, Event : UiEvent, Navigation : Any> Screen(
    viewModel: BaseViewModel<State, Event, Navigation>,
    onNavigation: (Navigation) -> Unit = {},
    content: @Composable (State, (Event) -> Unit) -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val outlet = LocalMessageOutlet.current ?: remember(context) { MessageOutlet.of(context) }
    // What draws the surface, the overlay, the alert and the empty/error content. `AppTheme`
    // installs this app's; on its own the module falls back to Material.
    val chrome = LocalScreenChrome.current

    chrome.Surface(modifier = Modifier.fillMaxSize()) {
        // A failure or empty state stands in for the content rather than covering it: there is
        // nothing behind it worth showing. Over a screen that has data, it stands in for the
        // content *inside* the screen's shell: the screen is composed, and its scaffold
        // reads the message from the local and draws it where the content would be.
        val contentState = uiState.content
        val data = uiState.data
        when {
            data != null -> {
                val message = remember(contentState, viewModel) {
                    contentState?.let { state ->
                        ContentMessage(state) { viewModel.onSystemEvent(SystemEvent.ContentAction(state.id)) }
                    }
                }
                CompositionLocalProvider(LocalContentMessage provides message) {
                    content(data, viewModel::onUiEvent)
                }
            }

            contentState != null -> chrome.ContentMessage(
                state = contentState,
                onAction = { viewModel.onSystemEvent(SystemEvent.ContentAction(contentState.id)) },
                modifier = Modifier,
            )
        }

        uiState.alert?.let { alert ->
            chrome.AlertDialog(
                state = alert,
                onConfirm = { answer ->
                    viewModel.onSystemEvent(SystemEvent.AlertResult.Confirmed(alert.id, alert.payload, answer))
                },
                onDecline = {
                    viewModel.onSystemEvent(SystemEvent.AlertResult.Declined(alert.id, alert.payload))
                },
                onDismiss = {
                    viewModel.onSystemEvent(SystemEvent.AlertResult.Dismissed(alert.id, alert.payload))
                },
                modifier = Modifier,
            )
        }

        // Composed always, shown while a call is waiting: the chrome owns the fade.
        chrome.LoadingOverlay(state = uiState.loading, modifier = Modifier)

        // The activity is edge to edge and `Screen()` applies no insets to its content, so the
        // host pads itself — without this the snackbar sits under the gesture-navigation bar.
        chrome.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding(),
        )
    }

    // `AppTheme` maps its base surface onto Material's, so a Custom Tab wears the screen's colour
    // without this module naming the design system.
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()
    val activity = LocalActivity.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val appNavigator = LocalAppNavigator.current

    CollectEffect(flow = viewModel.navigation, action = onNavigation)

    val noApp = R.string.core_command_no_app.toUiText()

    // A message with no action, which is this system's toast.
    fun say(message: UiText) = outlet.post(AppMessage(key = "core.command.${message.hashCode()}", text = message))

    fun run(command: UiCommand) {
        when (command) {
            // Posted rather than shown, so it outlives this screen; its action comes back to
            // this view model for as long as the view model lives.
            is UiCommand.ShowSnackbar -> outlet.post(
                AppMessage(
                    key = "${viewModel.messageOwner}:${command.id}",
                    text = command.message,
                    action = command.actionLabel?.let { MessageAction.Reply(it, command.id, viewModel.messageOwner) },
                    duration = if (command.actionLabel == null) MessageDuration.Short else MessageDuration.Long,
                    withDismissAction = command.withDismissAction,
                    tone = command.tone,
                ),
            )

            is UiCommand.NavigateBack -> backDispatcher?.onBackPressed()

            is UiCommand.CloseApp -> activity?.finishAndRemoveTask()

            is UiCommand.OpenAppSettings -> context.openAppSettings()

            // What leaves the app: when nothing on the device can take it, the user hears so.
            // Through the shell's app navigator; a host that provides none drops it.
            is UiCommand.Navigate -> appNavigator?.navigate(command.keys)

            is UiCommand.OpenBrowser -> if (!context.openBrowser(command.url, toolbarColor)) say(noApp)

            is UiCommand.Share -> if (!context.share(command)) say(noApp)

            is UiCommand.OpenExternal -> if (!context.openExternal(command.target)) say(noApp)

            is UiCommand.Copy -> if (!context.copy(command)) say(R.string.core_command_copied.toUiText())
        }
    }

    // A reply outlives this composition — the view model may still be in the back stack when its
    // message is acted on elsewhere — so it is registered for the view model's life, not the screen's.
    // An effect, so a composition that is thrown away registers nothing.
    LaunchedEffect(viewModel, outlet) {
        val owner = viewModel.messageOwner
        outlet.register(owner) { id -> viewModel.onSystemEvent(SystemEvent.SnackbarAction(id)) }
        viewModel.addCloseable("$MESSAGE_OWNER_KEY:${System.identityHashCode(outlet)}") { outlet.unregister(owner) }
    }

    // Only the screen resumed last renders. A message leaves the queue once it has been shown to
    // the end, acted on, or read — on screen for READ_AFTER_MILLIS — so one cut short by
    // navigation before anyone could read it is shown again by the next screen, and one already
    // read is not replayed on every screen after it. One that waits to be dismissed is carried
    // until it is.
    val renderer = remember { Any() }
    LifecycleResumeEffect(outlet) {
        outlet.attach(renderer)
        onPauseOrDispose { outlet.detach(renderer) }
    }
    LaunchedEffect(outlet) {
        outlet.renderer.map { it === renderer }.distinctUntilChanged().collectLatest { rendering ->
            if (!rendering) return@collectLatest
            outlet.head.collectLatest { message ->
                if (message == null) return@collectLatest
                val visuals = MessageVisuals.of(
                    message = message,
                    text = message.text.resolve(context),
                    actionLabel = message.action?.label?.resolve(context),
                )
                coroutineScope {
                    var read = false
                    val reading = launch {
                        delay(READ_AFTER_MILLIS)
                        read = visuals.duration != SnackbarDuration.Indefinite
                    }
                    try {
                        val result = snackbarHostState.showSnackbar(visuals)
                        outlet.done(message)
                        val action = message.action
                        if (result == SnackbarResult.ActionPerformed && action != null) {
                            if (action is MessageAction.Command) run(action.command) else outlet.perform(action)
                        }
                    } finally {
                        reading.cancel()
                        if (read) outlet.done(message)
                    }
                }
            }
        }
    }

    CollectEffect(flow = viewModel.command, action = ::run)
}

private const val MESSAGE_OWNER_KEY = "core.messageOwner"

/** How long a message is on screen before it counts as read and is not carried to the next one. */
private const val READ_AFTER_MILLIS = 1_500L
