package com.example.androidproject1.feature.gallery.presentation.gallery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.layout.appAnimateItem
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.gallery.presentation.R

/**
 * Every component in `:core:ui`, grouped the way the design system groups them.
 *
 * This is the running counterpart to the component document: what a screen is allowed to compose
 * from, and what each piece looks like in the theme that is actually loaded.
 */
@Composable
fun GalleryScreen(
    state: GalleryState,
    onEvent: (GalleryEvent) -> Unit,
) {
    AppScaffold(
        screenId = "GalleryScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.gallery_title),
                onNavigateUp = { onEvent(GalleryEvent.NavigateUpClicked) },
                navigateUpTestTag = "gallery_upButton",
            )
        },
        contentPadding = false,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("gallery_list"),
        ) {
            state.groups.forEach { (group, items) ->
                item(key = "header-${group.name}") {
                    AppSectionHeader(
                        title = group.label,
                        modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
                    )
                }
                items(items, key = { it.id }) { item ->
                    AppListItem(
                        headline = item.name,
                        supporting = item.summary,
                        onClick = { onEvent(GalleryEvent.ComponentClicked(item.id)) },
                        modifier = appAnimateItem().testTag("gallery_item_${item.id}"),
                    )
                    AppDivider(modifier = appAnimateItem())
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    GalleryScreen(
        state = GalleryState.PREVIEW,
    ) {}
}
