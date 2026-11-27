package com.example.androidproject1.feature.devmenu.presentation.playground

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.feature.devmenu.presentation.KnobValue
import com.example.androidproject1.feature.devmenu.presentation.R
import com.example.androidproject1.feature.devmenu.presentation.defaults
import com.example.androidproject1.feature.devmenu.presentation.playgroundCatalog
import com.example.androidproject1.feature.devmenu.presentation.playgroundEntry

/**
 * Which entry is on the bench and what its knobs hold. Nothing here names a component: the
 * screen looks the entry up by id and draws its controls from its knobs.
 */
@Immutable
data class DevMenuPlaygroundState(
    val entryId: String,
    val values: Map<String, KnobValue>,
    val stage: Stage = Stage(),
) {

    companion object {

        /** The first entry with its knobs at their defaults — also the ViewModel's initial state. */
        val PREVIEW = playgroundCatalog.first().let { DevMenuPlaygroundState(it.id, it.defaults()) }

        /**
         * The bench opened on [entryId], or on its first entry when that is null or is an id the
         * bench does not have — a gallery foundation page, say, which has no knobs.
         */
        fun openedOn(entryId: String?): DevMenuPlaygroundState =
            entryId?.let(::playgroundEntry)?.let { DevMenuPlaygroundState(it.id, it.defaults()) }
                ?: PREVIEW
    }
}

/**
 * What the bench is drawn in: the playground's own three knobs, above every component's,
 * and kept across entries — a person who set dark wants to see the next component dark too.
 */
@Immutable
data class Stage(
    val theme: StageTheme = StageTheme.System,
    val fontScale: StageFontScale = StageFontScale.Default,
    val width: StageWidth = StageWidth.Full,
    /** The device's "Remove animations" setting, as the stage's own switch. */
    val reducedMotion: Boolean = false,
)

enum class StageTheme(@StringRes val label: Int) {
    System(R.string.dev_menu_playground_theme_system),
    Light(R.string.dev_menu_playground_theme_light),
    Dark(R.string.dev_menu_playground_theme_dark),
}

/** Multipliers over the device's own font scale; `Largest` is Android's accessibility ceiling. */
enum class StageFontScale(val factor: Float) { Default(1f), Large(1.3f), Larger(1.5f), Largest(2f) }

/**
 * A fraction of the bench rather than a width in dp: a feature never writes a dp, and a phone
 * cannot show a tablet's 840 dp anyway. `Third` is where a row's label starts to wrap.
 */
enum class StageWidth(val fraction: Float, @StringRes val label: Int) {
    Full(1f, R.string.dev_menu_playground_width_full),
    Half(0.5f, R.string.dev_menu_playground_width_half),
    Third(1f / 3f, R.string.dev_menu_playground_width_third),
}

/** The first entry, a second entry, and the first with every knob turned — three shapes of bench. */
class DevMenuPlaygroundStatePreviews : PreviewParameterProvider<DevMenuPlaygroundState> {

    override val values = sequenceOf(
        DevMenuPlaygroundState.PREVIEW,
        playgroundEntry("switch")!!.let { DevMenuPlaygroundState(it.id, it.defaults()) },
        DevMenuPlaygroundState.PREVIEW.copy(
            values = mapOf(
                "label" to KnobValue.Text("Delete everything"),
                "kind" to KnobValue.Choice(1),
                "size" to KnobValue.Choice(2),
                "enabled" to KnobValue.Bool(false),
                "loading" to KnobValue.Bool(true),
            ),
            stage = Stage(theme = StageTheme.Dark, fontScale = StageFontScale.Larger, width = StageWidth.Half),
        ),
    )
}
