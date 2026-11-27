package com.example.androidproject1.feature.settings.presentation.about

import com.example.androidproject1.feature.settings.domain.AppInfo
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.device.DeviceInfo
import com.example.androidproject1.service.log.domain.Logger

/** Nothing to load: what this build is was fixed when it was built, and `:app` hands it in. */
class AboutViewModel(
    logger: Logger,
    private val appInfo: AppInfo,
    deviceInfo: DeviceInfo,
) : BaseViewModel<AboutState, AboutEvent, AboutNavigation>(
    initialState = AboutState(appInfo = appInfo, device = deviceInfo),
    logger = logger.withTag("AboutViewModel"),
) {

    override fun onUiEvent(event: AboutEvent) = when (event) {
        // A Custom Tab, so reading the policy does not leave the app.
        AboutEvent.PrivacyClicked -> sendCommand(UiCommand.OpenBrowser(appInfo.privacyUrl))

        AboutEvent.TermsClicked -> sendCommand(UiCommand.OpenBrowser(appInfo.termsUrl))

        AboutEvent.LicencesClicked -> navigate(AboutNavigation.Licences)

        AboutEvent.NavigateUpClicked -> navigate(AboutNavigation.NavigateUp)
    }
}
