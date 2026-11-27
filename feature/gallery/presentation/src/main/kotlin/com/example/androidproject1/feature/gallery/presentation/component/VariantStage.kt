package com.example.androidproject1.feature.gallery.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.gallery.presentation.gallerydetail.DetailTheme

/**
 * The ground a gallery variant is drawn on, in the theme the detail screen's strip picked.
 *
 * The demo ground is the sunken surface, which is what the system says a key or a list sits on. It
 * also has to be: a neutral button is the raised surface itself, so on a card it would be white on
 * white and invisible.
 *
 * [AppTheme] is entered again rather than only recoloured, so the variant sees the whole of layer 2
 * — colours, typography, elevation, motion — as it would on a device in that theme. That is the
 * mechanism the bench's stage uses, written here rather than imported from the bench: a feature's `presentation` may not
 * depend on another feature's. On [DetailTheme.System] the nested theme resolves to the one the
 * screen is already in, which is why a page opened from the list looks like the rest of the app.
 */
@Composable
fun VariantStage(
    theme: DetailTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        DetailTheme.System -> isSystemInDarkTheme()
        DetailTheme.Light -> false
        DetailTheme.Dark -> true
    }
    AppTheme(darkTheme = dark) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(AppTheme.shapes.lg)
                .background(AppTheme.colors.surfaceSunken)
                .padding(AppTheme.spacing.inset.lg),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
        ) {
            content()
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    VariantStage(theme = DetailTheme.Light) { AppText(text = "A variant, in light") }
    VariantStage(theme = DetailTheme.Dark) { AppText(text = "The same one, in dark") }
}
