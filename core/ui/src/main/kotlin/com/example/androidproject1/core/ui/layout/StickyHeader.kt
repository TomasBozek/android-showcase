package com.example.androidproject1.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A group's header that stays at the top while its group scrolls under it: `AppSectionHeader`
 * as a `stickyHeader`, on the base surface so the rows passing beneath it do not show through.
 *
 * A function on [LazyListScope] for the same reason `appAnimateItem` is one on `LazyItemScope`: a
 * sticky header is a kind of list item, not a modifier. [key] is the group's own, so the header
 * keeps its place when a group above it changes; [testTag] names it for a test and a flow.
 */
fun LazyListScope.appStickyHeader(
    title: String,
    key: Any? = title,
    testTag: String? = null,
) {
    stickyHeader(key = key) {
        AppSectionHeader(
            title = title,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.surfaceBase)
                .padding(horizontal = AppTheme.spacing.inset.lg, vertical = AppTheme.spacing.stack.xs)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        )
    }
}

/** Scrolled into the second group: its header is pinned while the first group's has gone. */
@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = PREVIEW_SCROLLED_TO),
        // A bounded height, or the list lays out every row, scrolls nowhere, and pins nothing.
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.density.listRowHeight * PREVIEW_VISIBLE_ROWS),
    ) {
        listOf("July 2026", "August 2026").forEach { month ->
            appStickyHeader(month)
            items(PREVIEW_ROWS) { row -> AppListItem(headline = "$month · trip ${row + 1}") }
        }
    }
}

private const val PREVIEW_ROWS = 5

private const val PREVIEW_VISIBLE_ROWS = 4

/** Two rows into August: July has scrolled away, and August's header is the pinned one. */
private const val PREVIEW_SCROLLED_TO = 8
