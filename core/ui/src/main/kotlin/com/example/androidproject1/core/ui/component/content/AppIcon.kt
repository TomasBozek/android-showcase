package com.example.androidproject1.core.ui.component.content

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * An icon that is not a control: the glyph on a door, the mark beside a row, the thing a card is
 * about. For an icon you press, [AppIconButton].
 *
 * The size is one of the three the theme names — `sm` in a row, `md` for a control, `lg` where
 * the icon is the thing being looked at — and never a number, so a screen asks for a role.
 *
 * @param contentDescription what a screen reader says, or `null` for an icon whose meaning the
 * text beside it already carries.
 */
@Composable
fun AppIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = AppTheme.icons.md,
    tint: Color = AppTheme.colors.textSecondary,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Row {
        AppIcon(icon = Icons.Filled.Favorite, contentDescription = null, size = AppTheme.icons.sm)
        AppIcon(icon = Icons.Filled.Favorite, contentDescription = null)
        AppIcon(icon = Icons.Filled.Favorite, contentDescription = null, size = AppTheme.icons.lg)
    }
}
