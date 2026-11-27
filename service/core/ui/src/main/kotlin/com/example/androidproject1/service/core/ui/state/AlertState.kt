package com.example.androidproject1.service.core.ui.state

import androidx.compose.runtime.Immutable
import com.example.androidproject1.service.core.ui.R
import com.example.androidproject1.service.core.ui.form.Validator
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Context an alert carries back to the ViewModel with its result — "which item was I confirming?".
 *
 * A feature implements it with its own data class, so the confirm handler reads a typed value
 * instead of casting out of a map:
 *
 * ```
 * private data class DeleteItem(val id: String) : AlertPayload
 * ```
 *
 * `@Immutable` because it is carried inside [AlertState], which every screen's `UiState` holds:
 * an unannotated interface there makes the whole envelope unstable. A payload is context about
 * what is being confirmed, so a data class of values is the only sensible implementation.
 */
@Immutable
public interface AlertPayload

/**
 * A question an alert asks beyond yes or no: a line of text, or a choice among a few
 * options. The answer comes back on `SystemEvent.AlertResult.Confirmed.answer`.
 */
@Immutable
public sealed interface AlertInput {

    /**
     * A line of text: [label] above the field, [placeholder] as its hint, [initial] already in it.
     * Confirm is disabled while [validator] fails, and its message shows under the field.
     */
    public data class Text(
        val label: UiText? = null,
        val initial: String = "",
        val validator: Validator? = null,
        val placeholder: UiText? = null,
    ) : AlertInput

    /** One of [options], or with [multiple] any of them, starting from [initial]. */
    public data class Choice(
        val options: List<AlertOption>,
        val multiple: Boolean = false,
        val initial: Set<String> = emptySet(),
    ) : AlertInput
}

/** One answer an [AlertInput.Choice] offers: [id] comes back, [label] is read. */
@Immutable
public data class AlertOption(val id: String, val label: UiText)

/** What an [AlertInput] was answered with. */
@Immutable
public sealed interface AlertAnswer {

    public data class Text(val value: String) : AlertAnswer

    /** The chosen ids, in the options' order; one in single mode. */
    public data class Choices(val ids: List<String>) : AlertAnswer
}

/**
 * One alert, raised with `setAlert` and drawn centrally by `Screen()`. It asks one of three kinds
 * of question:
 *
 * ```
 * // yes or no — the answer is which button: Confirmed or Declined
 * setAlert(id = "delete", message = …, confirmLabel = …, declineLabel = …, destructive = true)
 * // a word — Confirmed(answer = AlertAnswer.Text(value))
 * setAlert(id = "rename", message = …, input = AlertInput.Text(label = …, initial = name, validator = required()))
 * // a choice — Confirmed(answer = AlertAnswer.Choices(ids))
 * setAlert(id = "sort", message = …, input = AlertInput.Choice(options, multiple = false))
 * ```
 *
 * @property id identifies which alert this is, so a ViewModel showing several dialogs can tell the
 * resulting [com.example.androidproject1.service.core.ui.event.SystemEvent.AlertResult]s apart.
 * @property payload optional typed context echoed back with the result.
 * @property destructive the confirm cannot be undone — a delete, a log-out — so the chrome draws
 * it in the destructive colour and apart from the decline, never in the colour of an OK.
 * @property input a word or a choice to ask for; `null` is a yes or a no. An alert with an
 * input always offers a decline — [declineLabel], or Cancel.
 */
public data class AlertState(
    val id: String,
    val title: UiText? = null,
    val message: UiText?,
    val confirmLabel: UiText = R.string.core_action_ok.toUiText(),
    val declineLabel: UiText? = null,
    val dismissible: Boolean = true,
    val payload: AlertPayload? = null,
    val destructive: Boolean = false,
    val input: AlertInput? = null,
)

public fun <Data> MutableStateFlow<UiState<Data>>.clearAlert() {
    update { it.copy(alert = null) }
}

/** Raises an alert already built — the shared ones, such as `discardAlert()`. */
public fun <Data> MutableStateFlow<UiState<Data>>.setAlert(alert: AlertState) {
    update { it.copy(alert = alert) }
}

public fun <Data> MutableStateFlow<UiState<Data>>.setAlert(
    id: String,
    message: UiText,
    title: UiText? = null,
    confirmLabel: UiText = R.string.core_action_ok.toUiText(),
    declineLabel: UiText? = null,
    dismissible: Boolean = true,
    payload: AlertPayload? = null,
    destructive: Boolean = false,
    input: AlertInput? = null,
) {
    update {
        it.copy(
            alert = AlertState(
                id = id,
                title = title,
                message = message,
                confirmLabel = confirmLabel,
                declineLabel = declineLabel,
                dismissible = dismissible,
                payload = payload,
                destructive = destructive,
                input = input,
            ),
        )
    }
}
