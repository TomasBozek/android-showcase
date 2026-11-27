package com.example.androidproject1.feature.movies.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.AppSpinner
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.movies.presentation.R

/**
 * The end of the list, as the next page is doing: a spinner while it loads, and — when it
 * failed — what happened and the one button that asks again. The rows above it stay exactly as they
 * were: a failed append costs the user nothing they had. A page that is not loading and did not fail
 * draws nothing.
 */
@Composable
fun MoviesLoadFooter(
    state: LoadState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is LoadState.Loading -> Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.stack.md)
                .testTag("movies_moreProgress"),
            contentAlignment = Alignment.Center,
        ) {
            AppSpinner()
        }

        is LoadState.Error -> Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(AppTheme.spacing.inset.lg)
                .testTag("movies_pageErrorGroup"),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppText(text = stringResource(R.string.movies_page_error), role = TextRole.Secondary)
            AppButton(
                label = stringResource(R.string.movies_retry),
                onClick = onRetry,
                kind = ButtonKind.Outline,
                modifier = Modifier.testTag("movies_retryButton"),
            )
        }

        is LoadState.NotLoading -> Unit
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    MoviesLoadFooter(state = LoadState.Error(java.io.IOException("offline")), onRetry = {})
}
