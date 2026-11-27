package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.service.core.ui.form.Validator
import com.example.androidproject1.service.core.ui.text.resolve

/**
 * A question whose answer is a line of text — renaming a trip, naming a list, typing a reason.
 *
 * An [AppDialog] holding one [AppTextField]: [label] above the field, [placeholder] as its hint,
 * [initialValue] already in it. Confirm hands back what is typed, and is disabled while the value
 * fails [validator]; the validator's message is shown under the field once the value has been
 * edited, so an empty field that has not been touched yet is not scolded. The keyboard's done key
 * confirms whenever the button would.
 *
 * @param fieldTestTag the field's id, and [confirmTestTag] and [dismissTestTag] the buttons': the
 * caller's [modifier] goes to the dialog, so the parts need ids of their own for a flow to find
 * them — `AppConfirmDialog`'s precedent.
 */
@Composable
fun AppPromptDialog(
    title: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    label: String? = null,
    placeholder: String? = null,
    initialValue: String = "",
    validator: Validator? = null,
    dismissLabel: String = stringResource(R.string.app_dialog_cancel),
    fieldTestTag: String? = null,
    confirmTestTag: String? = null,
    dismissTestTag: String? = null,
    onDismissRequest: () -> Unit = onDismiss,
) {
    var value by rememberSaveable { mutableStateOf(initialValue) }
    var edited by rememberSaveable { mutableStateOf(false) }
    val error = validator?.invoke(value)
    val valid = error == null
    AppDialog(
        title = title,
        message = message,
        onDismiss = onDismissRequest,
        modifier = modifier,
        content = {
            AppTextField(
                value = value,
                onValueChange = {
                    value = it
                    edited = true
                },
                label = label,
                placeholder = placeholder,
                errorText = error?.takeIf { edited }?.resolve(),
                imeAction = ImeAction.Done,
                onImeAction = { if (valid) onConfirm(value) },
                modifier = fieldTestTag?.let { Modifier.testTag(it) } ?: Modifier,
            )
        },
    ) {
        AppButton(
            label = dismissLabel,
            onClick = onDismiss,
            kind = ButtonKind.Ghost,
            modifier = dismissTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        )
        AppButton(
            label = confirmLabel,
            onClick = { onConfirm(value) },
            kind = ButtonKind.Confirm,
            enabled = valid,
            modifier = confirmTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A dialog draws in its own window, so the preview shows what it would carry.
    AppText(text = "Rename this trip", role = TextRole.TitleLarge)
    AppTextField(value = "Lisbon", onValueChange = {}, label = "Name")
    AppButton(label = stringResource(R.string.app_dialog_cancel), onClick = {}, kind = ButtonKind.Ghost)
    AppButton(label = "Rename", onClick = {}, kind = ButtonKind.Confirm)
}
