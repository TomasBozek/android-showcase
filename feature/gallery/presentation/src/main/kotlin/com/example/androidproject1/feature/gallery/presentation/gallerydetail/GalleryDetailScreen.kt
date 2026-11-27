package com.example.androidproject1.feature.gallery.presentation.gallerydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.choice.AppSegmented
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.gallery.presentation.R
import com.example.androidproject1.feature.gallery.presentation.component.VariantStage
import com.example.androidproject1.feature.gallery.presentation.galleryEntry

/**
 * One component, in every state worth seeing.
 *
 * Each variant is labelled with the state it shows, so this doubles as the check that a component
 * still has all of them: a missing state here is a missing state in the component.
 *
 * The strip above them draws the variants in a theme of their own. It is a second
 * [AppTheme] written here — the same mechanism the bench's stage uses, and written
 * again rather than imported, because a feature's `presentation` may not depend on another
 * feature's. A role that reads in light and not in dark is what it is for, and it costs a tap
 * instead of a rebuild.
 */
@Composable
fun GalleryDetailScreen(
    state: GalleryDetailState,
    onEvent: (GalleryDetailEvent) -> Unit,
) {
    val entry = galleryEntry(state.componentId)
    AppScaffold(
        screenId = "GalleryDetailScreen",
        topBar = {
            AppTopBar(
                title = state.name,
                onNavigateUp = { onEvent(GalleryDetailEvent.NavigateUpClicked) },
                navigateUpTestTag = "galleryDetail_upButton",
            )
        },
        contentPadding = false,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppTheme.spacing.inset.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            item(key = "summary") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
                ) {
                    AppText(text = state.group.label.uppercase(), role = TextRole.LabelSmall)
                    AppText(text = state.summary, role = TextRole.BodyLarge)
                }
            }
            item(key = "controls") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
                ) {
                    AppSegmented(
                        options = DetailTheme.entries.map { stringResource(it.label) },
                        selectedIndex = state.theme.ordinal,
                        onSelect = { onEvent(GalleryDetailEvent.ThemePicked(DetailTheme.entries[it])) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("galleryDetail_themeTab"),
                    )
                    AppButton(
                        label = stringResource(R.string.gallery_detail_playground),
                        onClick = { onEvent(GalleryDetailEvent.PlaygroundClicked) },
                        kind = ButtonKind.Outline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("galleryDetail_playgroundButton"),
                    )
                }
            }
            entry?.variants?.forEach { variant ->
                item(key = variant.label) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // One tag for every variant: a test reaches the control inside with
                            // `hasAnyAncestor`, the way every compound component is tested.
                            .testTag("galleryDetail_variantItem"),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
                    ) {
                        AppText(text = variant.label, role = TextRole.LabelSmall)
                        VariantStage(theme = state.theme) { variant.content() }
                    }
                }
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    GalleryDetailScreen(
        state = GalleryDetailState.PREVIEW,
    ) {}
}
