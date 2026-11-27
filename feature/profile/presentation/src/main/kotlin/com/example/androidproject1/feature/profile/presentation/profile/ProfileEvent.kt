package com.example.androidproject1.feature.profile.presentation.profile

import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface ProfileEvent : UiEvent {

    data class NameChanged(val name: String) : ProfileEvent

    data class EmailChanged(val email: String) : ProfileEvent

    /** Pressed whether or not the form is valid — see [ProfileViewModel]. */
    data object SaveClicked : ProfileEvent

    /** Opens the camera half of the picture section, which is what the permission gate guards. */
    data object TakePhotoClicked : ProfileEvent

    /**
     * "Choose photo": the system Photo Picker, which asks for no permission. What comes back is a
     * file `:service:media` made — square, capped, no metadata — and the data layer copies it in.
     */
    data object ChoosePhotoClicked : ProfileEvent

    /** "Capture", shown once the camera permission is held: the camera app, through `:service:media`. */
    data object CaptureClicked : ProfileEvent

    /** Raised by the rationale once the system will not show the permission dialog again. */
    data object OpenAppSettingsClicked : ProfileEvent

    /** Sends the stored avatar again after an upload failed. */
    data object RetryUploadClicked : ProfileEvent

    data object NavigateUpClicked : ProfileEvent

    /** A contributed door's row, by the door's id. */
    data class DoorClicked(val id: String) : ProfileEvent

    /** The back gesture, on a form with unsaved edits. */
    data object BackRequested : ProfileEvent
}
