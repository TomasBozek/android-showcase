package com.example.androidproject1.service.core.ui.event

import com.example.androidproject1.service.core.ui.text.UiText

/**
 * Something the app tells the user that belongs to what happened, not to the screen that was up.
 * Posted to [AppMessages] from any layer and shown by whichever `Screen()` is resumed.
 *
 * @param key identifies the message in the queue: posting one whose key is already queued
 * replaces it in place, so "Removed" twice is said once.
 * @param action is data — a [UiCommand], a named [MessageActionHandler], or a reply to the view
 * model that posted it — never a lambda, so it still means something after its screen is gone.
 * A message with an action is a **snackbar** — an offer, drawn plain and kept up longer; one
 * without is a **toast** in its [tone] — a confirmation, gone in a few seconds unless it is an
 * [ToastTone.Error], which waits to be read.
 */
public data class AppMessage(
    val key: String,
    val text: UiText,
    val action: MessageAction? = null,
    val priority: Priority = Priority.Normal,
    val duration: MessageDuration = MessageDuration.Short,
    val withDismissAction: Boolean = false,
    val tone: ToastTone = ToastTone.Neutral,
) {

    /** [High] goes ahead of every [Normal] message queued, and interrupts one being shown. */
    public enum class Priority { Normal, High }
}

/**
 * What a toast is reporting. Here rather than beside `AppToast` because a message is posted from
 * code that cannot name the design system; `:core:ui` aliases it. An error is the only one that
 * does not dismiss itself.
 */
public enum class ToastTone { Neutral, Success, Error, Info, Warning }

/** How long a message stays up unless the user acts: a few seconds, longer, or until acted on. */
public enum class MessageDuration { Short, Long, Indefinite }

/** The one button a message may carry, and what pressing it does. */
public sealed interface MessageAction {

    public val label: UiText

    /** Runs [command] as if a view model had sent it — `OpenAppSettings`, `Share`, a navigation. */
    public data class Command(override val label: UiText, val command: UiCommand) : MessageAction

    /**
     * Calls the [MessageActionHandler] bound in DI under [handlerKey] with [argument] — how an Undo
     * reaches a repository after the screen that offered it has gone.
     */
    public data class Handler(override val label: UiText, val handlerKey: String, val argument: String) : MessageAction

    /**
     * Comes back to the view model that posted it as `SystemEvent.SnackbarAction(id)` — while that
     * view model lives; a press after it has been cleared does nothing. `UiCommand.ShowSnackbar`
     * with a label becomes this, and [owner] is filled in by `Screen()`, never by hand.
     */
    public data class Reply(override val label: UiText, val id: String, val owner: String) : MessageAction
}

/**
 * What a [MessageAction.Handler] calls. Bound in a feature's Koin module —
 * `singleOf(::RestoreItemHandler) bind MessageActionHandler::class` — under a [key] no other
 * handler uses.
 */
public interface MessageActionHandler {

    public val key: String

    public suspend fun handle(argument: String)
}
