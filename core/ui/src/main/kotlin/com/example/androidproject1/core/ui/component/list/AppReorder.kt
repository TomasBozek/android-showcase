package com.example.androidproject1.core.ui.component.list

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.layout.ReorderState
import com.example.androidproject1.core.ui.layout.appReorderHandle
import com.example.androidproject1.core.ui.layout.appReorderItem
import com.example.androidproject1.core.ui.layout.rememberReorderState
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * The handle a row is dragged by: put it in [AppListItem]'s `dragHandle` slot and a long
 * press on it lifts the row, a drag moves it, a release puts it down.
 *
 * On the handle and never on the row, for two reasons. A row already drags sideways when it has
 * swipe keys, and a list that reorders on a press anywhere is a list nobody can scroll.
 * [key] is the key the row carries in its `items {}` call, so the gesture and the list agree on
 * which row is moving.
 *
 * It is not a button: a tap does nothing, which is why it draws an icon rather than an
 * [AppIconButton]. What reaches the same reordering without a drag is the caller's job — a menu
 * item, a pair of arrows — because a gesture nobody is told about cannot be the only way.
 */
@Composable
fun AppReorder(
    state: ReorderState,
    key: Any,
    modifier: Modifier = Modifier,
) {
    AppIcon(
        icon = DragHandleIcon,
        contentDescription = stringResource(R.string.app_drag_handle),
        size = AppTheme.icons.md,
        modifier = modifier.appReorderHandle(state, key),
    )
}

/**
 * Two bars — Material's own drag-handle glyph, drawn here rather than imported.
 * `material-icons-core` is the only icon dependency (`gradle/libs.versions.toml`) and has no
 * handle; `material-icons-extended` is several thousand vectors for this one, which is not a
 * dependency earning its place. Tint and size are [AppIcon]'s, so it
 * behaves like every other icon in the system.
 */
private val DragHandleIcon: ImageVector = ImageVector.Builder(
    name = "DragHandle",
    defaultWidth = ICON_VIEWPORT.dp,
    defaultHeight = ICON_VIEWPORT.dp,
    viewportWidth = ICON_VIEWPORT,
    viewportHeight = ICON_VIEWPORT,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(x = 4f, y = 9f)
        horizontalLineTo(x = 20f)
        verticalLineTo(y = 11f)
        horizontalLineTo(x = 4f)
        close()
        moveTo(x = 4f, y = 13f)
        horizontalLineTo(x = 20f)
        verticalLineTo(y = 15f)
        horizontalLineTo(x = 4f)
        close()
    }
}.build()

/** Material's own icon viewport, so the glyph lines up with every imported one. */
private const val ICON_VIEWPORT = 24f

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    var rows by remember { mutableStateOf(listOf("Praha", "Brno", "Ostrava")) }
    val state = rememberReorderState { from, to ->
        rows = rows.toMutableList().apply { add(to, removeAt(from)) }
    }
    LazyColumn(
        state = state.listState,
        modifier = Modifier.height(AppTheme.density.listRowHeight * PREVIEW_ROWS),
    ) {
        items(rows, key = { it }) { row ->
            AppListItem(
                headline = row,
                modifier = appReorderItem(state, row),
                dragHandle = { AppReorder(state = state, key = row) },
            )
        }
    }
}

/** The preview's list is exactly its three rows tall, so the golden has no empty strip under it. */
private const val PREVIEW_ROWS = 3
