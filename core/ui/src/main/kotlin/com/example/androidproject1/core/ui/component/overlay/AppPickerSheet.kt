package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.choice.AppRadio
import com.example.androidproject1.core.ui.component.choice.AppRadioGroup
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppSelect

/**
 * One choice from a short list, from below.
 *
 * [AppSelect] is the same question as a field in a form, answered from an anchored menu, and it
 * stays that everywhere. This is the shape for a choice that is not a field — a sort order, a
 * view — where a phone shows a sheet of radios: every option is readable at once, the list can
 * scroll, and the current one is marked. A choice closes the sheet: the caller gets
 * [onSelect] with the index and then [onDismiss], in that order. On a wide window it is a
 * centred card, as every [AppSheet] is.
 *
 * @param optionTestTag the id every row carries — one id for the rows, as [AppMenu]'s items
 * share `<stem>_<name>Item`, since a row is addressed by position and the labels are translated.
 */
@Composable
fun AppPickerSheet(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    optionTestTag: String? = null,
) {
    AppSheet(onDismiss = onDismiss, modifier = modifier, title = title) {
        AppRadioGroup {
            options.forEachIndexed { index, option ->
                AppRadio(
                    selected = index == selectedIndex,
                    onSelect = {
                        onSelect(index)
                        onDismiss()
                    },
                    label = option,
                    modifier = if (optionTestTag != null) Modifier.testTag(optionTestTag) else Modifier,
                )
            }
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A sheet draws in its own window; this shows the rows it would carry.
    AppText(text = "Sort by", role = TextRole.TitleLarge)
    AppRadioGroup {
        AppRadio(selected = true, onSelect = {}, label = "Name")
        AppRadio(selected = false, onSelect = {}, label = "Price")
        AppRadio(selected = false, onSelect = {}, label = "Acquired")
    }
}
