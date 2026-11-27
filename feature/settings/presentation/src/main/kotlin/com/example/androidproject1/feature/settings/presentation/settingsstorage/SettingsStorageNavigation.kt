package com.example.androidproject1.feature.settings.presentation.settingsstorage

sealed interface SettingsStorageNavigation {

    data object NavigateUp : SettingsStorageNavigation
}
