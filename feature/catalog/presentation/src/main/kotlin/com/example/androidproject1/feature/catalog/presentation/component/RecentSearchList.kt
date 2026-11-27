package com.example.androidproject1.feature.catalog.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.choice.AppChip
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.catalog.presentation.R

/**
 * What was searched for before, which is what a field nobody has typed in yet shows instead of an
 * empty result list — the two look identical otherwise, and only one of them deserves a message.
 */
@Composable
fun RecentSearchList(
    recents: List<String>,
    onRecentClick: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recents.isEmpty()) {
        AppEmptyState(
            title = stringResource(R.string.product_search_recents_empty_title),
            message = stringResource(R.string.product_search_recents_empty_message),
            modifier = modifier.testTag("productSearch_recentsEmpty"),
        )
        return
    }

    Column(modifier = modifier) {
        AppSectionHeader(
            title = stringResource(R.string.product_search_recents),
            actionLabel = stringResource(R.string.product_search_recents_clear),
            onAction = onClear,
            modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
        )
        // Chips, not rows: a recent search is a word to press, not a thing to open, and
        // ten of them fit under the field where ten rows would push the results away.
        FlowRow(
            modifier = Modifier
                .padding(horizontal = AppTheme.spacing.inset.lg)
                .testTag("productSearch_recentList"),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
        ) {
            recents.forEach { recent ->
                AppChip(
                    label = recent,
                    selected = false,
                    onClick = { onRecentClick(recent) },
                    leadingIcon = Icons.Filled.Search,
                    modifier = Modifier.testTag("productSearch_recentItem"),
                )
            }
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    RecentSearchList(
        recents = listOf("coffee", "a rather long previous search"),
        onRecentClick = {},
        onClear = {},
    )
    RecentSearchList(recents = emptyList(), onRecentClick = {}, onClear = {})
}
