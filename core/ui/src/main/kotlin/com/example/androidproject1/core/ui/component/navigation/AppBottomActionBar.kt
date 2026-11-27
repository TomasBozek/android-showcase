package com.example.androidproject1.core.ui.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.content.AppCounter
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.AppVerticalDivider
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * The anchored bar carrying a screen's one main action.
 *
 * The total is on the left and the action on the right, and the action is the wider of the two —
 * on a phone this is what the thumb reaches without looking.
 *
 * @param actionTestTag what a flow and a test find the action by — `<stem>_<name>Button`. The
 * caller's [modifier] goes to the bar, whose centre is the total rather than the key, so an id put
 * there is one a flow taps and nothing answers.
 */
@Composable
fun AppBottomActionBar(
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    actionTestTag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surfaceRaised)
            .padding(AppTheme.spacing.inset.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.lg),
    ) {
        if (value != null) {
            Column(modifier = Modifier.weight(1f)) {
                if (label != null) AppText(text = label, role = TextRole.LabelSmall)
                // The total rolls as the cart changes under it.
                AppCounter(text = value, role = TextRole.DisplayLarge)
            }
            // A hairline between the figure and the key: two things side by side, one bar.
            AppVerticalDivider(modifier = Modifier.height(AppTheme.density.minTouchTarget))
        }
        AppButton(
            label = actionLabel,
            onClick = onAction,
            size = ControlSize.Large,
            enabled = enabled,
            modifier = Modifier
                .weight(if (value != null) 1.4f else 1f)
                .then(if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppBottomActionBar(
        label = "TOTAL",
        value = "1 248,00",
        actionLabel = "Pay",
        onAction = {},
    )
    AppBottomActionBar(actionLabel = "Continue", onAction = {})
}
