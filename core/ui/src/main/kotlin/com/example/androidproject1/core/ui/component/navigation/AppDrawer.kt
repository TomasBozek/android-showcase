package com.example.androidproject1.core.ui.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppLabelledDivider
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.AppBadge
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * One door in [AppDrawer].
 *
 * @param id what [AppDrawer]'s `onSelect` reports and what `selectedId` is compared with.
 * @param testTag what a flow and a test find the row by — `<stem>_<name>Item`. A row is a plain
 * value, so there is no modifier to hang one on; the same treatment as `MenuItem`'s.
 */
data class DrawerItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0,
    val testTag: String? = null,
)

/** A titled run of doors, divided from the next by an [AppLabelledDivider] that says the title. */
data class DrawerSection(
    val title: String,
    val items: List<DrawerItem>,
)

/**
 * The main menu: every place in the app, in sections, over the screen.
 *
 * The five-tab bar stays the five most-used places; this is all of them. Modal on every size
 * class, opened from `AppTopBar`'s menu icon or by the platform's swipe from the start edge, and
 * closed by a row, the scrim, the swipe back or the back gesture — each of which reaches
 * [onOpenChange], so the caller's `open` is the one truth and a test can read it.
 *
 * The selected door draws in the same pill as `AppBottomNav`'s, so the two chromes read as one.
 *
 * @param header what sits above the list — an account, a logo; nothing by default.
 * @param content the screen the drawer opens over.
 */
@Composable
fun AppDrawer(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    sections: List<DrawerSection>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(if (open) DrawerValue.Open else DrawerValue.Closed)
    val isOpen by rememberUpdatedState(open)

    // The caller drives the state, and a gesture the drawer handles itself — the scrim, a swipe —
    // drives the caller: only a settled value that disagrees with `open` is reported, so opening
    // from the caller never bounces back as a close.
    LaunchedEffect(open) { if (open) drawerState.open() else drawerState.close() }
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }.collect { value ->
            val settledOpen = value == DrawerValue.Open
            if (settledOpen != isOpen) onOpenChange(settledOpen)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        scrimColor = AppTheme.colors.scrim.copy(alpha = AppTheme.colors.scrimAlpha),
        drawerContent = {
            ModalDrawerSheet(
                // The sheet is a sibling of the screen, not a descendant of its scaffold, so the
                // scaffold's `testTagsAsResourceId` does not reach it — the same line AppSheet and
                // AppMenu carry, for the same reason.
                modifier = Modifier.semantics { testTagsAsResourceId = true },
                drawerContainerColor = AppTheme.colors.surfaceRaised,
                drawerContentColor = AppTheme.colors.textPrimary,
                drawerShape = AppTheme.shapes.sheet,
            ) {
                header()
                // A plain column that scrolls, not a lazy one: a menu is a dozen rows, and every
                // row existing at once is what lets a test count the doors and a flow find one
                // below the fold.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .selectableGroup()
                        .testTag("menu_list")
                        .padding(AppTheme.spacing.inset.md),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
                ) {
                    sections.forEach { section ->
                        AppLabelledDivider(
                            label = section.title,
                            modifier = Modifier.padding(vertical = AppTheme.spacing.stack.sm),
                        )
                        section.items.forEach { item ->
                            DrawerRow(
                                item = item,
                                selected = item.id == selectedId,
                                onClick = {
                                    onSelect(item.id)
                                    onOpenChange(false)
                                },
                            )
                        }
                    }
                }
            }
        },
        content = content,
    )
}

@Composable
private fun DrawerRow(
    item: DrawerItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
            .clip(AppTheme.shapes.pill)
            .background(if (selected) colors.destructive.container else colors.surfaceRaised)
            .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.inset.lg, vertical = AppTheme.spacing.inset.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (selected) colors.destructive.onContainer else colors.textSecondary,
            modifier = Modifier.size(AppTheme.icons.md),
        )
        AppText(
            text = item.label,
            role = TextRole.Body,
            color = if (selected) colors.destructive.onContainer else colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        AppBadge(count = item.badge)
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        AppDrawer(
            open = true,
            onOpenChange = {},
            sections = listOf(
                DrawerSection(
                    "Places",
                    listOf(
                        DrawerItem("home", "Home", Icons.Filled.Home),
                        DrawerItem("catalog", "Catalog", Icons.AutoMirrored.Filled.List),
                        DrawerItem("cart", "Cart", Icons.Filled.ShoppingCart, badge = 3),
                    ),
                ),
                DrawerSection("You", listOf(DrawerItem("settings", "Settings", Icons.Filled.Settings))),
            ),
            selectedId = "home",
            onSelect = {},
        ) {
            AppText(text = "The screen underneath", role = TextRole.Body)
        }
    }
}
