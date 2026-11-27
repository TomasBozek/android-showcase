package com.example.androidproject1.core.ui.component.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/** What a banner is saying. The default is [Warning], which is what "you're offline" is. */
enum class BannerTone { Info, Success, Warning, Error }

/**
 * One line, full width, in a feedback role, drawn above the screen rather than over it: a
 * fact about the whole app that stays true until it stops — the device is offline.
 *
 * Not a toast: a toast reports something that happened and goes away; a banner reports
 * something that is so, for as long as it is so, and has no action of its own. What to do about
 * it is the screen's business. `MainActivity` composes one above `AppNavHost` always, and shows
 * it with [visible] while `MainViewModel.online` is false: the banner expands in over
 * `sheetEnterMillis` and shrinks out over `sheetExitMillis`, so the top bar below it slides
 * rather than jumps. A banner composed only while its fact holds would pop.
 */
@Composable
fun AppBanner(
    text: String,
    modifier: Modifier = Modifier,
    tone: BannerTone = BannerTone.Warning,
    visible: Boolean = true,
) {
    val feedback = AppTheme.colors.feedback
    val family = when (tone) {
        BannerTone.Info -> feedback.info
        BannerTone.Success -> feedback.success
        BannerTone.Warning -> feedback.warning
        BannerTone.Error -> feedback.error
    }
    val motion = AppTheme.motion
    // The caller's modifier stays on the text, inside the clip, so an inset the caller pads for
    // shrinks with the banner rather than staying behind as a bare strip.
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(motion.sheetEnterMillis, easing = motion.toggleEasing)) +
            fadeIn(tween(motion.sheetEnterMillis, easing = motion.toggleEasing)),
        exit = shrinkVertically(tween(motion.sheetExitMillis, easing = motion.toggleEasing)) +
            fadeOut(tween(motion.sheetExitMillis, easing = motion.toggleEasing)),
    ) {
        AppText(
            text = text,
            role = TextRole.Label,
            color = family.onContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
                .fillMaxWidth()
                .background(family.container)
                .padding(
                    horizontal = AppTheme.spacing.inset.lg,
                    vertical = AppTheme.spacing.inset.sm,
                ),
        )
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
        AppBanner(text = "You're offline")
        AppBanner(text = "Synced a minute ago", tone = BannerTone.Info)
        AppBanner(text = "All receipts printed", tone = BannerTone.Success)
        AppBanner(text = "The printer is out of paper", tone = BannerTone.Error)
    }
}
