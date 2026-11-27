package com.example.androidproject1.core.ui.component.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.R
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * Search: the glass on the left, clearing on the right.
 *
 * Clearing is its own control rather than a gesture, because a half-typed query is the most common
 * thing anyone needs to undo.
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
            .clip(AppTheme.shapes.md)
            .background(colors.surfaceSunken)
            .border(1.dp, colors.borderStrong, AppTheme.shapes.md)
            .padding(horizontal = AppTheme.spacing.inset.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm),
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(AppTheme.icons.sm),
        )
        // The input fills the row's height and carries the placeholder as its name, so it is a
        // full-height target a screen reader can call something — an empty single-line field is
        // otherwise eleven dp tall and nameless. The default name is the field's job.
        val name = placeholder.ifEmpty { stringResource(R.string.app_search) }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AppTheme.typography.bodyMd.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.focusRing),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
                .semantics { text = AnnotatedString(name) },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        AppText(text = placeholder, role = TextRole.Secondary)
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            AppIconButton(
                icon = Icons.Filled.Close,
                contentDescription = stringResource(R.string.app_search_clear),
                onClick = { onValueChange("") },
            )
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppSearchField(value = "", onValueChange = {}, placeholder = "Search products")
    AppSearchField(value = "Pilsner", onValueChange = {})
}
