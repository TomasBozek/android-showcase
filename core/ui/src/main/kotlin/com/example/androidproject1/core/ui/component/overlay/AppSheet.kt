package com.example.androidproject1.core.ui.component.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.choice.AppRadio
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.SizeClass

/** The card a sheet becomes on a wide window, so a test can tell the two shapes apart. */
const val SHEET_DIALOG_TAG = "sheet_dialog"

/**
 * The default way to open a detail or a choice on a phone — and a centred window on a tablet.
 *
 * On a `Compact` width the sheet comes from below, and the handle is always drawn, even when the
 * sheet cannot be dragged — it is what says "this came from below and goes back down". On
 * `Regular` and `Expanded` the same content is a window sized like [AppDialog] in the middle of
 * the screen: a sheet that spans a 1000 dp window is a wall, and `Density.kt` said an
 * overlay changes shape with the width long before anything did it. The caller never knows which
 * it got. Only one overlay at a time: a sheet over a sheet replaces the first rather than
 * stacking, and back closes both.
 *
 * The content scrolls: a sheet holding a form is taller than the narrowest phone, and without
 * this its last control was simply below the screen — which `OverlayScreenshotTest` records, and
 * no preview could have seen.
 */
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (AppTheme.density.sizeClass == SizeClass.Compact) {
        BottomSheet(onDismiss = onDismiss, modifier = modifier, title = title, content = content)
    } else {
        CentredSheet(onDismiss = onDismiss, modifier = modifier, title = title, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier,
    title: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // A window of its own, so the scaffold's `testTagsAsResourceId` does not reach it — see
        // AppMenu for the same line and why.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        sheetState = rememberModalBottomSheetState(),
        containerColor = AppTheme.colors.surfaceRaised,
        scrimColor = AppTheme.colors.scrim.copy(alpha = AppTheme.colors.scrimAlpha),
        shape = AppTheme.shapes.sheet,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.spacing.inset.md),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(AppTheme.shapes.pill)
                        .background(AppTheme.colors.borderStrong),
                )
            }
        },
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppTheme.spacing.inset.xl,
                    end = AppTheme.spacing.inset.xl,
                    bottom = AppTheme.spacing.inset.xl,
                ),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            if (title != null) AppText(text = title, role = TextRole.TitleLarge)
            content()
        }
    }
}

/**
 * The wide-window shape: [AppDialog]'s card — the same width cap, height cap, corner, surface and
 * inset — with the sheet's title and scrolling content and no actions row, since a sheet's
 * actions are its content. The window sizes itself for the same reason the dialog's does.
 */
@Composable
private fun CentredSheet(
    onDismiss: () -> Unit,
    modifier: Modifier,
    title: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = modifier
                // A window of its own, so the scaffold's `testTagsAsResourceId` does not reach it.
                .semantics { testTagsAsResourceId = true }
                .testTag(SHEET_DIALOG_TAG)
                .padding(AppTheme.spacing.inset.md)
                .widthIn(max = MAX_SHEET_DIALOG_WIDTH)
                .heightIn(max = MAX_SHEET_DIALOG_HEIGHT)
                .clip(AppTheme.shapes.xl)
                .background(AppTheme.colors.surfaceRaised)
                .padding(AppTheme.spacing.inset.xl),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
        ) {
            if (title != null) AppText(text = title, role = TextRole.TitleLarge)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
                content = content,
            )
        }
    }
}

/** [AppDialog]'s own caps: 440 wide, and 568 tall because a phone in landscape is about 360 dp tall. */
private val MAX_SHEET_DIALOG_WIDTH = 440.dp
private val MAX_SHEET_DIALOG_HEIGHT = 568.dp

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    // A sheet draws in its own window; this shows the content it would carry.
    AppText(text = "Appearance", role = TextRole.TitleLarge)
    AppRadio(selected = true, onSelect = {}, label = "Light")
    AppRadio(selected = false, onSelect = {}, label = "Dark")
}
