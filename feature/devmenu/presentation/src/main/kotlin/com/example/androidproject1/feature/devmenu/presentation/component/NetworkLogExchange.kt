package com.example.androidproject1.feature.devmenu.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.devmenu.presentation.R
import com.example.androidproject1.service.devtools.NetworkLogEntry

/**
 * One exchange of the network log, whole: the request line, what came back and how long it took,
 * and both bodies as they were — what the detail sheet shows when a row is tapped.
 */
@Composable
fun NetworkLogExchange(
    entry: NetworkLogEntry,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
        AppText(text = "${entry.method} ${entry.url}", role = TextRole.Title)
        AppText(text = entry.summary(), role = TextRole.Secondary)
        AppText(text = stringResource(R.string.network_log_request_body), role = TextRole.LabelSmall)
        AppText(text = entry.requestBody ?: stringResource(R.string.network_log_no_body), role = TextRole.Body)
        AppText(text = stringResource(R.string.network_log_response_body), role = TextRole.LabelSmall)
        AppText(text = entry.responseBody ?: stringResource(R.string.network_log_no_body), role = TextRole.Body)
    }
}

/** `200 · 41 ms`, or `no answer · 41 ms`: what a row and the sheet say after the path. */
@Composable
fun NetworkLogEntry.summary(): String = stringResource(
    R.string.network_log_summary,
    status?.toString() ?: stringResource(R.string.network_log_no_answer),
    durationMs,
)

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    NetworkLogExchange(
        entry = NetworkLogEntry(
            id = 1,
            method = "POST",
            url = "https://example.test/orders",
            path = "/orders",
            status = 201,
            durationMs = 84,
            requestBody = """{"lines":[{"productId":"coffee","quantity":2}]}""",
            responseBody = """{"id":"1042"}""",
        ),
    )
}
