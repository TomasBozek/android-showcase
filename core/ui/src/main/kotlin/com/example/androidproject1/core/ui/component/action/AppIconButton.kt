package com.example.androidproject1.core.ui.component.action

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.height
import com.example.androidproject1.core.ui.component.navigation.AppToolbar
import com.example.androidproject1.core.ui.component.overlay.AppTooltip
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.keySurface
import com.example.androidproject1.core.ui.theme.pressFeedback

/**
 * What an icon button does, in the same words [AppButton] uses.
 *
 * [Plain] is the default and carries no fill — it is the one that belongs in a top bar, where a
 * filled circle would compete with the title. The other two are for an icon that is the action
 * itself, with no label beside it to say which.
 */
/** [Inverse] is [Plain] on a dark wash — the actions on an [AppToolbar]. */
enum class IconButtonKind { Plain, Confirm, Destructive, Inverse }

/**
 * An icon that does something.
 *
 * [contentDescription] is not optional: an icon with no label has to say what it is, to a screen
 * reader and to a test, and it is the same string for both. [size] moves the target, which is what
 * a finger has to find; the glyph inside it stays 24 dp.
 *
 * [square] is for an icon button in a row of them — a toolbar, a keypad — where a run of circles
 * reads as a row of holes. On its own an icon button is a pill.
 *
 * [tooltip] wraps the button in an [AppTooltip] that says [contentDescription] on a long press
 * and on hover — every icon-only action explains itself, and no screen has to ask. Off
 * only where the caller draws its own.
 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: IconButtonKind = IconButtonKind.Plain,
    size: ControlSize = ControlSize.Medium,
    square: Boolean = false,
    tooltip: Boolean = true,
) {
    if (tooltip) {
        AppTooltip(text = contentDescription, modifier = modifier) {
            IconButtonKey(icon, contentDescription, onClick, Modifier, enabled, kind, size, square)
        }
    } else {
        IconButtonKey(icon, contentDescription, onClick, modifier, enabled, kind, size, square)
    }
}

@Composable
private fun IconButtonKey(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    kind: IconButtonKind,
    size: ControlSize,
    square: Boolean,
) {
    val colors = AppTheme.colors
    val family = when (kind) {
        IconButtonKind.Plain, IconButtonKind.Inverse -> null
        IconButtonKind.Confirm -> colors.confirm
        IconButtonKind.Destructive -> colors.destructive
    }
    val shape = if (square) AppTheme.shapes.md else AppTheme.shapes.pill
    val tint = when {
        !enabled -> colors.textDisabled
        family != null -> family.label
        kind == IconButtonKind.Inverse -> colors.textOnInverse
        else -> colors.textPrimary
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            // A filled kind is a key and travels on its edge; a plain one has no edge and scales
            // instead. A disabled one does neither.
            .then(if (family == null && enabled) Modifier.pressFeedback(interaction) else Modifier)
            .size(size.height)
            .then(
                // A disabled filled button keeps its shape and loses its colour, the same way a
                // disabled `AppButton` flattens rather than only fading its label.
                when {
                    family != null && enabled -> Modifier.keySurface(
                        color = family.bg,
                        edge = family.edge,
                        shape = shape,
                        pressed = pressed,
                    )
                    family != null ->
                        Modifier
                            .clip(shape)
                            .background(colors.surfaceSunken)
                    else -> Modifier.clip(shape)
                },
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(AppTheme.icons.md),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppIconButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        onClick = {},
    )
    AppIconButton(
        icon = Icons.Filled.Done,
        contentDescription = "Confirm",
        onClick = {},
        kind = IconButtonKind.Confirm,
    )
    AppIconButton(
        icon = Icons.Filled.Delete,
        contentDescription = "Delete",
        onClick = {},
        kind = IconButtonKind.Destructive,
        square = true,
    )
    AppIconButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back, small",
        onClick = {},
        size = ControlSize.Small,
    )
    AppIconButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back, large",
        onClick = {},
        size = ControlSize.Large,
    )
    AppIconButton(
        icon = Icons.Filled.Done,
        contentDescription = "Unavailable",
        onClick = {},
        kind = IconButtonKind.Confirm,
        enabled = false,
    )
}
