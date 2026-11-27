package com.example.androidproject1.core.ui.component.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.layout.LocalMenuOpener
import com.example.androidproject1.core.ui.layout.appCoachTarget
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Where I am, and at most three things I can do about it.
 *
 * Flat and unshaded — `elevation.0` — so the bar belongs to the screen rather than floating over
 * it. Back is always on the left; a tab root passes no [onNavigateUp] at all, because the bottom
 * bar is what leaves it — and in its place the bar draws the menu icon, `menu_openButton`, when
 * the host provides [LocalMenuOpener]: the drawer icon on a top-level destination, the up
 * arrow anywhere else, which is the platform's own rule. A preview and a test see no opener and
 * draw no icon.
 *
 * @param navigateUpTestTag the arrow's id, `<screenStem>_upButton`. The caller's [modifier] goes
 * to the bar, so the arrow needs one of its own for a test to find it — and it is found by id
 * rather than by its label, which is translated.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    navigateUpTestTag: String? = null,
    /** Reaches the title's own text — for `Modifier.appSharedElement`, when a name arrives from a row. */
    titleModifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = AppTheme.spacing.inset.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm),
    ) {
        val openMenu = LocalMenuOpener.current
        when {
            onNavigateUp != null -> AppIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.app_navigate_up),
                onClick = onNavigateUp,
                modifier = navigateUpTestTag?.let { Modifier.testTag(it) } ?: Modifier,
            )

            openMenu != null -> AppIconButton(
                icon = Icons.Filled.Menu,
                contentDescription = stringResource(R.string.app_menu_open),
                onClick = openMenu,
                // The one element a tour points at that belongs to no screen: the bar
                // draws it, so the bar registers it.
                modifier = Modifier
                    .testTag("menu_openButton")
                    .appCoachTarget("menu_openButton"),
            )
        }
        AppText(
            text = title,
            role = TextRole.Display,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppTheme.spacing.inline.sm)
                .then(titleModifier),
        )
        actions()
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppTopBar(title = "Settings")
    AppTopBar(title = "Permissions", onNavigateUp = {})
}
