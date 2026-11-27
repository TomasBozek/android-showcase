package com.example.androidproject1.feature.apphold.presentation.apphold

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.play.AppUpdates
import com.example.androidproject1.service.play.UpdateKind
import com.example.androidproject1.service.play.UpdateStart
import kotlinx.coroutines.launch

class AppHoldViewModel(
    logger: Logger,
    // The route key, handed in by the shell: why the app is held, and where its store page is.
    private val args: AppHoldDestination,
    private val appUpdates: AppUpdates,
) : BaseViewModel<AppHoldState, AppHoldEvent, AppHoldNavigation>(
    initialState = AppHoldState(reason = args.reason),
    logger = logger.withTag("AppHoldViewModel"),
) {

    override fun onUiEvent(event: AppHoldEvent) = when (event) {
        AppHoldEvent.UpdateClicked -> update()
    }

    /**
     * Play's immediate update, in place, when Play has one for this device; the store page
     * when it has not — a build not installed from Play, or an update it does not offer as
     * immediate. A user who turns the flow down is left on this screen: they can ask again, and
     * are not sent to a store they did not choose.
     */
    private fun update() {
        viewModelScope.launch {
            val offer = appUpdates.check()
            val started = if (offer?.immediateAllowed ==
                true
            ) {
                appUpdates.start(UpdateKind.Immediate)
            } else {
                UpdateStart.NotStarted
            }
            if (started == UpdateStart.NotStarted) sendCommand(UiCommand.OpenBrowser(args.storeUrl))
        }
    }
}
