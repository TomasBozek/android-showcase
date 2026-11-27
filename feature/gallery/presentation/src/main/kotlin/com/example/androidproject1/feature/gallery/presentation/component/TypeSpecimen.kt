package com.example.androidproject1.feature.gallery.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.content.textStyle
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * One type role, set in itself: the sample at the role's own size, then the role's name and the
 * size and line height it resolved to at this density.
 *
 * The metrics are read from the loaded theme rather than written here, so the page says what the
 * device is actually rendering — which is the whole reason to look at a specimen rather than at
 * `Type.kt`.
 */
@Composable
fun TypeSpecimen(
    role: TextRole,
    sample: String,
    modifier: Modifier = Modifier,
) {
    val style = role.textStyle()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
    ) {
        AppText(text = sample, role = role)
        AppText(
            text = "${role.name} · ${style.fontSize.value.toInt()} sp / ${style.lineHeight.value.toInt()} sp",
            role = TextRole.Secondary,
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    TypeSpecimen(role = TextRole.Display, sample = "Order 12 — On its way")
    TypeSpecimen(role = TextRole.Body, sample = "The default size for list rows and dialog copy.")
}
