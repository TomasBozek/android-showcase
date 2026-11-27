package com.example.androidproject1.feature.settings.presentation.permissions

import android.Manifest
import com.example.androidproject1.feature.settings.presentation.R
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.permission.DeclaredPermission
import com.example.androidproject1.service.permission.PermissionChecker
import com.example.androidproject1.service.permission.PermissionStatus

class SettingsPermissionsViewModel(
    logger: Logger,
    private val permissions: PermissionChecker,
) : BaseViewModel<SettingsPermissionsState, SettingsPermissionsEvent, SettingsPermissionsNavigation>(
    // Renders straight away: an empty list is the honest state until the screen has read the
    // platform's answer, and it arrives on the first composition.
    initialState = SettingsPermissionsState(permissions = emptyList(), canPostNotifications = false),
    logger = logger.withTag("SettingsPermissionsViewModel"),
) {

    override fun onUiEvent(event: SettingsPermissionsEvent) {
        when (event) {
            is SettingsPermissionsEvent.PermissionsRead -> show(event.permissions)

            SettingsPermissionsEvent.OpenAppSettingsClicked ->
                sendCommand(UiCommand.OpenAppSettings)

            SettingsPermissionsEvent.NavigateUpClicked ->
                navigate(SettingsPermissionsNavigation.NavigateUp)

            SettingsPermissionsEvent.AllowNotificationsClicked -> allowNotifications()

            SettingsPermissionsEvent.RationaleContinueClicked -> {
                val refusedForGood = uiState.value.data?.rationale == NotificationsRationale.RefusedForGood
                updateData { copy(rationale = null, askNotifications = !refusedForGood) }
                if (refusedForGood) sendCommand(UiCommand.OpenAppSettings)
            }

            SettingsPermissionsEvent.RationaleNotNowClicked -> updateData { copy(rationale = null) }

            SettingsPermissionsEvent.NotificationsAsked -> updateData { copy(askNotifications = false) }
        }
    }

    // What the screen last read, so a tap is decided on the platform's current answer.
    private var declared: List<DeclaredPermission> = emptyList()

    /**
     * Android wants a rationale before a second ask, and the system dialog never shows after a
     * refusal for good — so the status decides: ask, explain then ask, or explain then Settings.
     */
    private fun allowNotifications() {
        val notifications = declared.firstOrNull { it.name == Manifest.permission.POST_NOTIFICATIONS }
            ?: DeclaredPermission(Manifest.permission.POST_NOTIFICATIONS, isGranted = false)
        when (permissions.status(notifications)) {
            PermissionStatus.NotRequested -> updateData { copy(askNotifications = true) }
            PermissionStatus.NeedsRationale -> updateData { copy(rationale = NotificationsRationale.Explain) }
            PermissionStatus.RefusedForGood -> updateData { copy(rationale = NotificationsRationale.RefusedForGood) }
            PermissionStatus.Granted, is PermissionStatus.PartiallyGranted -> Unit
        }
    }

    /**
     * The name a person reads: the five the app asks for or reads, by name; anything else
     * — a library's, a future one — as its last segment, which is what every row was before.
     */
    private fun String.label(): UiText = when (this) {
        Manifest.permission.POST_NOTIFICATIONS -> R.string.settings_permissions_label_notifications.toUiText()
        Manifest.permission.ACCESS_FINE_LOCATION -> R.string.settings_permissions_label_fine_location.toUiText()
        Manifest.permission.ACCESS_COARSE_LOCATION -> R.string.settings_permissions_label_coarse_location.toUiText()
        Manifest.permission.CAMERA -> R.string.settings_permissions_label_camera.toUiText()
        Manifest.permission.ACCESS_NETWORK_STATE -> R.string.settings_permissions_label_network_state.toUiText()
        else -> UiText.Literal(substringAfterLast('.'))
    }

    private fun show(declared: List<DeclaredPermission>) {
        this.declared = declared
        val rows = declared.map {
            PermissionRow(
                name = it.name,
                label = it.name.label(),
                isGranted = it.isGranted,
            )
        }

        updateData {
            copy(
                permissions = rows,
                canPostNotifications = rows.any { row ->
                    row.name == Manifest.permission.POST_NOTIFICATIONS && row.isGranted
                },
            )
        }
    }
}
