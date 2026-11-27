package com.example.androidproject1.service.core.ui.event

import com.example.androidproject1.service.core.ui.state.AlertAnswer
import com.example.androidproject1.service.core.ui.state.AlertPayload

/**
 * Raised by the framework-level UI rather than by a feature's own screen. Each carries the [id] of
 * the alert that produced it, so a ViewModel showing several dialogs can tell them apart, and the
 * [payload] that alert was raised with.
 */
public sealed interface SystemEvent : UiEvent {

    public sealed interface AlertResult : SystemEvent {

        public val id: String
        public val payload: AlertPayload?

        /**
         * The primary button was pressed. [answer] is what an alert with an `AlertInput` was
         * answered with, `null` for a yes-or-no alert.
         */
        public data class Confirmed(
            override val id: String,
            override val payload: AlertPayload? = null,
            val answer: AlertAnswer? = null,
        ) : AlertResult

        /** The secondary button was pressed. */
        public data class Declined(
            override val id: String,
            override val payload: AlertPayload? = null,
        ) : AlertResult

        /** The dialog was dismissed without a choice. */
        public data class Dismissed(
            override val id: String,
            override val payload: AlertPayload? = null,
        ) : AlertResult
    }

    /**
     * The button on a [com.example.androidproject1.service.core.ui.state.ContentState] was pressed —
     * "Try again" on a failure, or whatever action an empty state offers.
     *
     * `BaseViewModel` handles this by default: it clears the content state and re-runs the call
     * that failed. Override `onSystemEvent` only when an empty state's action needs to do
     * something else.
     */
    public data class ContentAction(val id: String) : SystemEvent

    /**
     * The action button on a [UiCommand.ShowSnackbar] was pressed, carrying that snackbar's id.
     *
     * `BaseViewModel` ignores it: a snackbar with an action is asking the screen a question, so
     * there is no sensible default. Handle the ids you raise in `onSystemEvent` and delegate the
     * rest to `super`.
     */
    public data class SnackbarAction(val id: String) : SystemEvent
}
