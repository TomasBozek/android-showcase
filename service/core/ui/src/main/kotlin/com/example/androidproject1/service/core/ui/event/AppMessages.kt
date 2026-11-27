package com.example.androidproject1.service.core.ui.event

import android.content.Context
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * The outlet: one app-scoped queue any layer posts a message to — a view model, a repository, a
 * worker, the session. Inject it; a view model's `showSnackbar` already posts here.
 */
public interface AppMessages {

    public fun post(message: AppMessage)
}

/**
 * The queue behind [AppMessages], and what `Screen()` renders from.
 *
 * It lives for the process, not the screen, so a message posted as a screen closes is shown by the
 * next `Screen()` to resume; a message leaves the queue once it has been shown to the end,
 * dismissed, acted on, or read — on screen long enough that leaving the screen does not replay it.
 * Only the screen resumed last renders, so two screens composed at once
 * never show the same message twice.
 *
 * [of] the application is the instance `Screen()` reads by default and the one `:app` binds, so
 * posting needs no provider in the shell; one per `Application` rather than one per class loader,
 * so a Robolectric test — a fresh application each — never sees the last test's messages. A test
 * that drives the queue builds its own and hands it to `ProvideMessageOutlet`.
 */
public class MessageOutlet(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AppMessages {

    private val queue = MutableStateFlow<List<AppMessage>>(emptyList())

    /** What is waiting, the head first. */
    public val pending: StateFlow<List<AppMessage>> = queue.asStateFlow()

    /** The message to show next, or `null` with nothing waiting. */
    public val head: Flow<AppMessage?> = queue.map { it.firstOrNull() }.distinctUntilChanged()

    private val renderers = MutableStateFlow<List<Any>>(emptyList())

    /** The `Screen()` that renders — the one resumed last. */
    public val renderer: Flow<Any?> = renderers.map { it.lastOrNull() }.distinctUntilChanged()

    private val owners = ConcurrentHashMap<String, (String) -> Unit>()

    @Volatile
    private var handlers: (String) -> MessageActionHandler? = { null }

    @Volatile
    private var logger: Logger? = null

    override fun post(message: AppMessage) {
        queue.update { list ->
            val queued = list.indexOfFirst { it.key == message.key }
            when {
                queued >= 0 -> list.toMutableList().apply { set(queued, message) }
                message.priority == AppMessage.Priority.High -> {
                    val at = list.indexOfFirst { it.priority != AppMessage.Priority.High }
                    list.toMutableList().apply { add(if (at < 0) size else at, message) }
                }
                else -> list + message
            }
        }
    }

    /** [message] was shown to the end, dismissed or acted on. By identity: a replacement stays. */
    public fun done(message: AppMessage) {
        queue.update { list -> list.filterNot { it === message } }
    }

    public fun attach(renderer: Any): Unit = renderers.update { it - renderer + renderer }

    public fun detach(renderer: Any): Unit = renderers.update { it - renderer }

    /** Where a [MessageAction.Reply] posted under [owner] goes, until [unregister]. */
    public fun register(owner: String, onReply: (id: String) -> Unit) {
        owners[owner] = onReply
    }

    public fun unregister(owner: String) {
        owners.remove(owner)
    }

    /** The app's handlers, looked up by key when an action is pressed rather than when bound. */
    public fun bindHandlers(logger: Logger, handlers: (key: String) -> MessageActionHandler?) {
        this.logger = logger
        this.handlers = handlers
    }

    /** Runs a [MessageAction.Handler] or delivers a [MessageAction.Reply]; a `Command` is `Screen()`'s. */
    public fun perform(action: MessageAction) {
        when (action) {
            is MessageAction.Command -> Unit

            is MessageAction.Reply -> owners[action.owner]?.invoke(action.id)

            is MessageAction.Handler -> {
                val handler = handlers(action.handlerKey)
                if (handler == null) {
                    logger?.w { "No MessageActionHandler bound under ${action.handlerKey}" }
                    return
                }
                scope.launch {
                    try {
                        handler.handle(action.argument)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger?.e(e) { "MessageActionHandler ${action.handlerKey} failed" }
                    }
                }
            }
        }
    }

    public companion object {

        private val outlets = WeakHashMap<Context, MessageOutlet>()

        /** The app's outlet: what `Screen()` renders by default and `:app` binds as [AppMessages]. */
        public fun of(context: Context): MessageOutlet = synchronized(outlets) {
            outlets.getOrPut(context.applicationContext) { MessageOutlet() }
        }
    }
}
