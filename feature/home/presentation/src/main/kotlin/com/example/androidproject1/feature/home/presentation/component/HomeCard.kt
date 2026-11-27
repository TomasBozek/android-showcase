package com.example.androidproject1.feature.home.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppCounter
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.status.AppBadge
import com.example.androidproject1.core.ui.layout.appCoachTarget
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.keySurface

/**
 * One door on Home: the whole card is the key you press — a raised surface that travels on
 * `keySurface`, the way every other key in the system does — with the door's icon at the size an
 * icon has when it is the thing being looked at, a title, the one-line figure from that feature's
 * domain so a public door is never blank, and a count worn as a badge. The test id is the
 * caller's, `home_<door>Card`, because the vocabulary names the element and not the component.
 */
@Composable
fun HomeCard(
    title: String,
    icon: ImageVector,
    onOpen: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    figure: String? = null,
    badge: Int = 0,
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = AppTheme.density.minTouchTarget)
            .keySurface(
                color = colors.surfaceRaised,
                edge = colors.borderStrong,
                shape = AppTheme.shapes.xl,
                pressed = pressed,
            )
            .clickable(
                role = Role.Button,
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
            )
            .testTag(testTag)
            // A door is a tour target under the id a test finds it by.
            .appCoachTarget(testTag)
            .padding(AppTheme.spacing.inset.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AppIcon(
                    icon = icon,
                    contentDescription = null,
                    size = AppTheme.icons.lg,
                )
            }
            AppBadge(count = badge)
        }
        AppText(text = title, role = TextRole.Title)
        // A figure with a number in it rolls as the domain changes under it: "2 items"
        // becomes "3 items". One without — an email, a sentence — is text, and wraps.
        if (figure != null) {
            if (figure.any { it.isDigit() }) {
                AppCounter(text = figure, role = TextRole.Secondary)
            } else {
                AppText(text = figure, role = TextRole.Secondary)
            }
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    HomeCard(
        title = "Cart",
        icon = Icons.Filled.ShoppingCart,
        onOpen = {},
        testTag = "home_cartCard",
        figure = "2 items",
        badge = 2,
    )
    HomeCard(
        title = "Gallery",
        icon = Icons.Filled.ShoppingCart,
        onOpen = {},
        testTag = "home_galleryCard",
    )
}
