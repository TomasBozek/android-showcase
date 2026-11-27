package com.example.androidproject1.core.ui.component.choice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppCounter
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Quantity, one [step] at a time.
 *
 * The value sits between the two keys at a fixed width so the row does not shift as the number
 * grows from 9 to 10, and it will not go below [min]. A [step] other than one is for a figure
 * counted in units — 250 ms of latency — and the value shown is the figure, never the
 * number of steps.
 */
@Composable
fun AppStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = Int.MAX_VALUE,
    step: Int = 1,
) {
    val haptics = LocalHapticFeedback.current

    // A tick is felt as a tick, and the one that lands on the floor or the ceiling as a stop:
    // the wall is felt as it is reached, and the key beyond it is disabled as before.
    fun tick(next: Int) {
        haptics.performHapticFeedback(
            if (next == min || next == max) HapticFeedbackType.Reject else HapticFeedbackType.SegmentTick,
        )
        onValueChange(next)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm),
    ) {
        AppIconButton(
            icon = Icons.Filled.Clear,
            contentDescription = stringResource(R.string.app_stepper_decrement),
            onClick = { tick((value - step).coerceAtLeast(min)) },
            enabled = value > min,
        )
        // The value rolls between the keys, centred at a width that holds two digits.
        Box(modifier = Modifier.widthIn(min = 40.dp), contentAlignment = Alignment.Center) {
            AppCounter(text = value.toString(), role = TextRole.Numeric)
        }
        AppIconButton(
            icon = Icons.Filled.Add,
            contentDescription = stringResource(R.string.app_stepper_increment),
            onClick = { tick((value + step).coerceAtMost(max)) },
            enabled = value < max,
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppStepper(value = 1, onValueChange = {})
    AppStepper(value = 12, onValueChange = {})
    AppStepper(value = 0, onValueChange = {}, min = 0)
}
