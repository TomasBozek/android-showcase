package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.choice.AppCheckbox
import com.example.androidproject1.core.ui.component.choice.AppRadio
import com.example.androidproject1.core.ui.component.choice.AppRadioGroup
import com.example.androidproject1.core.ui.component.choice.CheckState
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole

/** One answer an [AppChoiceDialog] offers: [id] is what comes back, [label] what is read. */
@Immutable
data class ChoiceOption(val id: String, val label: String)

/**
 * A choice among a few options, asked as a dialog — sort by, remind me, which list. For a long
 * list, or one worth searching, `AppPickerSheet` is the tool; this is for the three to seven
 * options that fit on the card.
 *
 * Single choice draws [AppRadio] rows, in one [AppRadioGroup] so a screen reader says "one of N",
 * and confirm is disabled until one is picked. [multiple] draws [AppCheckbox] rows and may confirm
 * with none, because "none" is an answer there. [onConfirm] hands back the chosen ids in the
 * options' order, one of them in single mode.
 *
 * @param optionTestTag the id each row carries, from the option's own id — `"sort_item_$it"` — so
 * a flow taps the option it means; [confirmTestTag] and [dismissTestTag] are the buttons', as on
 * `AppConfirmDialog`.
 */
@Composable
fun AppChoiceDialog(
    title: String,
    options: List<ChoiceOption>,
    confirmLabel: String,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    multiple: Boolean = false,
    initialSelection: Set<String> = emptySet(),
    dismissLabel: String = stringResource(R.string.app_dialog_cancel),
    optionTestTag: ((id: String) -> String)? = null,
    confirmTestTag: String? = null,
    dismissTestTag: String? = null,
    onDismissRequest: () -> Unit = onDismiss,
) {
    var chosen by rememberSaveable { mutableStateOf(initialSelection.toList()) }
    fun tagged(id: String) = optionTestTag?.let { Modifier.testTag(it(id)) } ?: Modifier
    AppDialog(
        title = title,
        message = message,
        onDismiss = onDismissRequest,
        modifier = modifier,
        content = {
            if (multiple) {
                options.forEach { option ->
                    AppCheckbox(
                        checked = if (option.id in chosen) CheckState.On else CheckState.Off,
                        onCheckedChange = { on -> chosen = if (on) chosen + option.id else chosen - option.id },
                        label = option.label,
                        modifier = tagged(option.id),
                    )
                }
            } else {
                AppRadioGroup {
                    options.forEach { option ->
                        AppRadio(
                            selected = option.id in chosen,
                            onSelect = { chosen = listOf(option.id) },
                            label = option.label,
                            modifier = tagged(option.id),
                        )
                    }
                }
            }
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
            onClick = { onConfirm(options.map { it.id }.filter { it in chosen }) },
            kind = ButtonKind.Confirm,
            enabled = multiple || chosen.isNotEmpty(),
            modifier = confirmTestTag?.let { Modifier.testTag(it) } ?: Modifier,
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A dialog draws in its own window, so the preview shows what it would carry.
    AppText(text = "Sort by", role = TextRole.TitleLarge)
    AppRadioGroup {
        AppRadio(selected = true, onSelect = {}, label = "Name")
        AppRadio(selected = false, onSelect = {}, label = "Price")
    }
    AppCheckbox(checked = CheckState.On, onCheckedChange = {}, label = "Show sold out")
    AppButton(label = "Apply", onClick = {}, kind = ButtonKind.Confirm)
}
