package com.example.androidproject1.service.core.ui.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import com.example.androidproject1.service.core.ui.event.AppMessage
import com.example.androidproject1.service.core.ui.event.MessageDuration
import com.example.androidproject1.service.core.ui.event.ToastTone

/** The two kinds of message a user reads differently. */
public enum class MessageKind {

    /** Confirms what happened and asks nothing: a tone, no action. */
    Toast,

    /** Offers one action about it. */
    Snackbar,
}

/**
 * An [AppMessage] as the snackbar host sees it: Material's visuals plus the [tone] and [kind] a
 * chrome draws from. `Screen()` builds it with [of], which is where the kinds' rules live — a
 * snackbar stays up at least `Long`, an error toast until it is dismissed.
 */
public data class MessageVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val withDismissAction: Boolean,
    override val duration: SnackbarDuration,
    val tone: ToastTone,
    val kind: MessageKind,
) : SnackbarVisuals {

    public companion object {

        public fun of(message: AppMessage, text: String, actionLabel: String?): MessageVisuals {
            val kind = if (message.action == null) MessageKind.Toast else MessageKind.Snackbar
            val duration = when {
                kind == MessageKind.Toast && message.tone == ToastTone.Error -> SnackbarDuration.Indefinite
                message.duration == MessageDuration.Indefinite -> SnackbarDuration.Indefinite
                kind == MessageKind.Snackbar || message.duration == MessageDuration.Long -> SnackbarDuration.Long
                else -> SnackbarDuration.Short
            }
            return MessageVisuals(
                message = text,
                actionLabel = actionLabel,
                // Something that waits has to be closable without acting on it.
                withDismissAction = message.withDismissAction || duration == SnackbarDuration.Indefinite,
                duration = duration,
                tone = message.tone,
                kind = kind,
            )
        }
    }
}
