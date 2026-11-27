package com.example.androidproject1.feature.settings.presentation.settingsstorage

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface SettingsStorageEvent : UiEvent {

    /** Start the download, or carry on from what an earlier try kept. */
    data object DownloadClicked : SettingsStorageEvent

    data object CancelClicked : SettingsStorageEvent

    data object DeleteClicked : SettingsStorageEvent

    data object NavigateUpClicked : SettingsStorageEvent
}
