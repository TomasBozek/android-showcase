package com.example.androidproject1.feature.devmenu.presentation.playground

import com.example.androidproject1.feature.devmenu.api.DevMenuPlaygroundDestination
import com.example.androidproject1.feature.devmenu.presentation.defaults
import com.example.androidproject1.feature.devmenu.presentation.playgroundEntry
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger

class DevMenuPlaygroundViewModel(
    logger: Logger,
    // The route key, handed in by the destination: which entry the bench opens on, or null for
    // its first.
    args: DevMenuPlaygroundDestination,
) : BaseViewModel<DevMenuPlaygroundState, DevMenuPlaygroundEvent, DevMenuPlaygroundNavigation>(
    initialState = DevMenuPlaygroundState.openedOn(args.entryId),
    logger = logger.withTag("DevMenuPlaygroundViewModel"),
) {

    override fun onUiEvent(event: DevMenuPlaygroundEvent) {
        when (event) {
            // The knobs reset with the entry: a value left over from another entry's knob of the
            // same key would be a bench that remembers the wrong thing.
            is DevMenuPlaygroundEvent.ComponentPicked -> playgroundEntry(event.entryId)?.let { entry ->
                updateData { copy(entryId = entry.id, values = entry.defaults()) }
            }

            is DevMenuPlaygroundEvent.KnobChanged ->
                updateData { copy(values = values + (event.key to event.value)) }

            is DevMenuPlaygroundEvent.StageThemePicked -> updateData { copy(stage = stage.copy(theme = event.theme)) }
            is DevMenuPlaygroundEvent.StageFontScalePicked ->
                updateData { copy(stage = stage.copy(fontScale = event.fontScale)) }
            is DevMenuPlaygroundEvent.StageWidthPicked -> updateData { copy(stage = stage.copy(width = event.width)) }
            is DevMenuPlaygroundEvent.StageReducedMotionToggled ->
                updateData { copy(stage = stage.copy(reducedMotion = event.reducedMotion)) }

            DevMenuPlaygroundEvent.NavigateUpClicked -> navigate(DevMenuPlaygroundNavigation.NavigateUp)
        }
    }
}
