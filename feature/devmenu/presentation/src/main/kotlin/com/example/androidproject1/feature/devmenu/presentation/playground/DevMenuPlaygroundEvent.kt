package com.example.androidproject1.feature.devmenu.presentation.playground

import com.example.androidproject1.feature.devmenu.presentation.KnobValue
import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface DevMenuPlaygroundEvent : UiEvent {

    /** Another entry on the bench; its knobs start at their defaults. */
    data class ComponentPicked(val entryId: String) : DevMenuPlaygroundEvent

    /** One knob turned. */
    data class KnobChanged(val key: String, val value: KnobValue) : DevMenuPlaygroundEvent

    /** One of the stage's own knobs turned; the component's knobs are left alone. */
    data class StageThemePicked(val theme: StageTheme) : DevMenuPlaygroundEvent

    data class StageFontScalePicked(val fontScale: StageFontScale) : DevMenuPlaygroundEvent

    data class StageWidthPicked(val width: StageWidth) : DevMenuPlaygroundEvent

    data class StageReducedMotionToggled(val reducedMotion: Boolean) : DevMenuPlaygroundEvent

    data object NavigateUpClicked : DevMenuPlaygroundEvent
}
