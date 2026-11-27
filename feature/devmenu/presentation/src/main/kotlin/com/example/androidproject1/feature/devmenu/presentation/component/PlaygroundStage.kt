package com.example.androidproject1.feature.devmenu.presentation.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.list.AppCard
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.devmenu.presentation.playground.Stage
import com.example.androidproject1.feature.devmenu.presentation.playground.StageFontScale
import com.example.androidproject1.feature.devmenu.presentation.playground.StageTheme
import com.example.androidproject1.feature.devmenu.presentation.playground.StageWidth

/**
 * The bench the playground draws a component on, in the theme, font scale and width the
 * [Stage] says: a raised card, the component centred, and inside it a second `AppTheme`
 * so a dark stage on a light screen is the dark palette and nothing else changes — and a stage
 * under reduced motion is the theme with every duration at zero.
 *
 * The font scale is a `LocalDensity` with the factor multiplied in, which is what the system
 * does for the accessibility setting; the width is a fraction of the card, never a dp.
 */
@Composable
fun PlaygroundStage(
    stage: Stage,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (stage.theme) {
        StageTheme.System -> isSystemInDarkTheme()
        StageTheme.Light -> false
        StageTheme.Dark -> true
    }
    val density = LocalDensity.current
    AppTheme(darkTheme = darkTheme, reducedMotion = stage.reducedMotion) {
        AppCard(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.inset.lg),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, density.fontScale * stage.fontScale.factor),
                ) {
                    Box(modifier = Modifier.fillMaxWidth(stage.width.fraction), contentAlignment = Alignment.Center) {
                        content()
                    }
                }
            }
        }
    }
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    PlaygroundStage(stage = Stage()) {
        AppButton(label = "On the stage", onClick = {})
    }
    PlaygroundStage(
        stage = Stage(theme = StageTheme.Dark, fontScale = StageFontScale.Larger, width = StageWidth.Half),
    ) {
        AppButton(label = "Dark, larger, half", onClick = {})
    }
}
