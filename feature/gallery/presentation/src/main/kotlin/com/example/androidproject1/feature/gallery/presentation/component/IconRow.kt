package com.example.androidproject1.feature.gallery.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * One icon size beside the text it sits with, so the size is compared against something.
 *
 * Three sizes and only three, and the job picks which: a marker in a row is `sm`, a control the
 * finger goes to is `md`, an icon that is the thing being looked at is `lg`. None of them is a
 * touch target — that is `density.minTouchTarget` and a different question.
 */
@Composable
fun IconRow(
    name: String,
    size: Dp,
    icon: ImageVector,
    usedFor: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(icon = icon, contentDescription = null, size = size)
        AppText(text = "$name · ${size.value.toInt()} dp · $usedFor", role = TextRole.Secondary)
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    IconRow(
        name = "sm",
        size = AppTheme.icons.sm,
        icon = Icons.Default.Star,
        usedFor = "beside text in a row",
    )
    IconRow(
        name = "lg",
        size = AppTheme.icons.lg,
        icon = Icons.Default.Star,
        usedFor = "the icon is the thing being looked at",
    )
}
