package com.example.androidproject1.service.core.ui.form

import com.example.androidproject1.service.core.ui.R
import com.example.androidproject1.service.core.ui.state.AlertState
import com.example.androidproject1.service.core.ui.text.toUiText

/** The id every discard confirmation is raised and answered under. */
public const val ALERT_ID_DISCARD: String = "discard_changes"

/**
 * The confirmation a form raises when the back gesture would throw away what has been typed.
 *
 * One wording and one id for every form in the app, in `:service:core:ui` because the copy ships
 * with the module: three screens asking the same question three different ways is how a product
 * stops sounding like one thing. `DiscardBackHandler` in `:service:navigation` is the gesture that asks for it.
 */
public fun discardAlert(): AlertState = AlertState(
    id = ALERT_ID_DISCARD,
    title = R.string.core_discard_title.toUiText(),
    message = R.string.core_discard_message.toUiText(),
    confirmLabel = R.string.core_action_discard.toUiText(),
    declineLabel = R.string.core_action_keep_editing.toUiText(),
)
