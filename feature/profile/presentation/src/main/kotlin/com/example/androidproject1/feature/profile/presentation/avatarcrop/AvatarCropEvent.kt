package com.example.androidproject1.feature.profile.presentation.avatarcrop

import com.example.androidproject1.core.ui.component.content.ImageCrop
import com.example.androidproject1.service.core.ui.event.UiEvent

sealed interface AvatarCropEvent : UiEvent {

    /** The frame moved: a pan or a pinch, or the first draw. */
    data class CropChanged(val crop: ImageCrop) : AvatarCropEvent

    data object DoneClicked : AvatarCropEvent

    data object CancelClicked : AvatarCropEvent
}
