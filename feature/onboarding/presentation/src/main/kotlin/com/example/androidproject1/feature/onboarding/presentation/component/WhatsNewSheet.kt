package com.example.androidproject1.feature.onboarding.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.overlay.AppSheet
import com.example.androidproject1.feature.onboarding.presentation.R

/**
 * What changed in this update: one page of text and one key, once per update, over
 * whichever flow is up. An [AppSheet], so a dialog on a tablet. The text is a string
 * resource, `onboarding_whats_new_body`, rewritten with each release that has something to say.
 *
 * Dismissing it by any means — the key, a swipe, a tap outside — is [onDone]: it has been seen.
 */
@Composable
fun WhatsNewSheet(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppSheet(
        onDismiss = onDone,
        title = stringResource(R.string.onboarding_whats_new_title),
        modifier = modifier.testTag("whatsNew_sheet"),
    ) {
        AppText(text = stringResource(R.string.onboarding_whats_new_body), role = TextRole.Body)
        AppButton(
            label = stringResource(R.string.onboarding_whats_new_done),
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("whatsNew_doneButton"),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A sheet draws in its own window; this shows the page it would carry.
    AppText(text = stringResource(R.string.onboarding_whats_new_body), role = TextRole.Body)
}
