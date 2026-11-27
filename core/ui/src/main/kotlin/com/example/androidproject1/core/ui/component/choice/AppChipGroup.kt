package com.example.androidproject1.core.ui.component.choice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.overlay.AppPickerSheet
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A row of [AppChip]s that wraps: the tags of a filter, the views of a list.
 *
 * The chosen ones are [selected], by index, and the group holds nothing: a tap hands back the
 * whole next set through [onSelectionChange], so the caller's state is the only state. With
 * [single] a tap chooses that one alone — a sort order, a view — and a tap on the chosen one
 * clears it, so "none" stays reachable; without it a tap toggles membership.
 *
 * @param chipTestTag one id for every chip, the way [AppPickerSheet]'s rows share one: a chip is
 * addressed by position and its label is translated.
 */
@Composable
fun AppChipGroup(
    options: List<String>,
    selected: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    single: Boolean = false,
    chipTestTag: String? = null,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
    ) {
        options.forEachIndexed { index, option ->
            val chosen = index in selected
            AppChip(
                label = option,
                selected = chosen,
                onClick = {
                    onSelectionChange(
                        when {
                            single -> if (chosen) emptySet() else setOf(index)
                            chosen -> selected - index
                            else -> selected + index
                        },
                    )
                },
                modifier = if (chipTestTag != null) Modifier.testTag(chipTestTag) else Modifier,
            )
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppChipGroup(
        options = listOf("Fragile", "Lent out", "Insured", "Needs repair", "Boxed"),
        selected = setOf(0, 2),
        onSelectionChange = {},
    )
}
