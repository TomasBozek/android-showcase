package com.example.androidproject1.feature.settings.presentation.settingsstorage

import androidx.lifecycle.viewModelScope
import com.example.androidproject1.feature.settings.domain.SampleDownload
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.launch

class SettingsStorageViewModel(
    logger: Logger,
    private val sampleDownload: SampleDownload,
) : BaseViewModel<SettingsStorageState, SettingsStorageEvent, SettingsStorageNavigation>(
    initialState = SettingsStorageState.PREVIEW,
    logger = logger.withTag("SettingsStorageViewModel"),
) {

    init {
        // The job outlives this screen, so the state is what it last said and not what was tapped:
        // coming back to a download that finished while the screen was closed shows it finished.
        viewModelScope.launch {
            sampleDownload.state.collect { download -> updateData { copy(download = download) } }
        }
    }

    override fun onUiEvent(event: SettingsStorageEvent) {
        when (event) {
            SettingsStorageEvent.DownloadClicked -> sampleDownload.start()
            SettingsStorageEvent.CancelClicked -> sampleDownload.cancel()
            SettingsStorageEvent.DeleteClicked -> viewModelScope.launch { sampleDownload.delete() }
            SettingsStorageEvent.NavigateUpClicked -> navigate(SettingsStorageNavigation.NavigateUp)
        }
    }
}
