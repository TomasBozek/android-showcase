package com.example.androidproject1.feature.inventory.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.choice.AppRadio
import com.example.androidproject1.core.ui.component.choice.AppRadioGroup
import com.example.androidproject1.core.ui.component.field.AppDateField
import com.example.androidproject1.core.ui.component.field.AppFormField
import com.example.androidproject1.core.ui.component.field.AppSelect
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.inventory.domain.ItemCategory
import com.example.androidproject1.feature.inventory.domain.ItemCondition
import com.example.androidproject1.feature.inventory.presentation.R
import com.example.androidproject1.feature.inventory.presentation.inventoryeditor.InventoryEditorEvent
import com.example.androidproject1.feature.inventory.presentation.inventoryeditor.InventoryEditorState
import com.example.androidproject1.service.core.ui.text.resolve

/**
 * The editor's first step: what the item is called, what kind it is, what state it is in and
 * when it arrived. Its own file because `InventoryEditorScreen` composes four of these and a
 * screen file holds only the screen and its previews.
 */
@Composable
fun ItemBasicsStep(
    state: InventoryEditorState,
    onEvent: (InventoryEditorEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
    ) {
        AppTextField(
            value = state.name.value,
            onValueChange = { onEvent(InventoryEditorEvent.NameChanged(it)) },
            label = stringResource(R.string.inventory_editor_name_label),
            errorText = state.name.error?.resolve(),
            // Next, not Done: the category, the condition and the date are still below it.
            imeAction = ImeAction.Next,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventoryEditor_nameField"),
        )

        // The code: typed, or read with the camera into the same field. The field is always
        // there — a device with no Play services cannot scan, and a worn label will not.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
            verticalAlignment = Alignment.Bottom,
        ) {
            AppTextField(
                value = state.barcode,
                onValueChange = { onEvent(InventoryEditorEvent.BarcodeChanged(it)) },
                label = stringResource(R.string.inventory_editor_barcode_label),
                imeAction = ImeAction.Next,
                modifier = Modifier
                    .weight(1f)
                    .testTag("inventoryEditor_barcodeField"),
            )
            AppButton(
                label = stringResource(R.string.inventory_editor_scan),
                onClick = { onEvent(InventoryEditorEvent.ScanTapped) },
                kind = ButtonKind.Outline,
                modifier = Modifier.testTag("inventoryEditor_scanButton"),
            )
        }

        val categories = ItemCategory.entries
        AppFormField(
            label = stringResource(R.string.inventory_editor_category_label),
            required = true,
            modifier = Modifier.testTag("inventoryEditor_categoryField"),
        ) {
            AppSelect(
                options = categories.map { stringResource(it.labelRes()) },
                selectedIndex = categories.indexOf(state.category),
                onSelect = { onEvent(InventoryEditorEvent.CategorySelected(categories[it])) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        AppFormField(
            label = stringResource(R.string.inventory_editor_condition_label),
            helperText = stringResource(R.string.inventory_editor_condition_helper),
            modifier = Modifier.testTag("inventoryEditor_conditionField"),
        ) {
            AppRadioGroup {
                ItemCondition.entries.forEach { condition ->
                    AppRadio(
                        selected = state.condition == condition,
                        onSelect = { onEvent(InventoryEditorEvent.ConditionSelected(condition)) },
                        label = stringResource(condition.labelRes()),
                    )
                }
            }
        }

        AppDateField(
            value = state.acquiredOn,
            onValueChange = { onEvent(InventoryEditorEvent.AcquiredOnChanged(it)) },
            label = stringResource(R.string.inventory_editor_acquired_label),
            placeholder = stringResource(R.string.inventory_editor_acquired_placeholder),
            modifier = Modifier.testTag("inventoryEditor_acquiredField"),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    ItemBasicsStep(state = InventoryEditorState.PREVIEW, onEvent = {})
}
