package com.example.androidproject1.feature.profile.presentation.avatarcrop

import com.example.androidproject1.feature.profile.api.AvatarCropDestination
import com.example.androidproject1.feature.profile.domain.ProfileRepository
import com.example.androidproject1.feature.profile.presentation.R
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.media.CropArea
import com.example.androidproject1.service.media.ImageCropper
import java.io.File
import java.net.URI

/**
 * The square an avatar keeps: the picture the picker returned, under a frame the user pans
 * and pinches. Done cuts that square at [ImageCropper.AVATAR_PX] and stores it as the avatar —
 * Profile follows the stored profile, so it shows on the way back; Cancel stores nothing.
 */
class AvatarCropViewModel(
    logger: Logger,
    private val args: AvatarCropDestination,
    private val imageCropper: ImageCropper,
    private val profileRepository: ProfileRepository,
) : BaseViewModel<AvatarCropState, AvatarCropEvent, AvatarCropNavigation>(
    initialState = AvatarCropState(uri = args.uri),
    logger = logger.withTag("AvatarCropViewModel"),
) {

    override fun onUiEvent(event: AvatarCropEvent) = when (event) {
        is AvatarCropEvent.CropChanged -> updateData { copy(crop = event.crop) }
        AvatarCropEvent.DoneClicked -> finish()
        AvatarCropEvent.CancelClicked -> navigate(AvatarCropNavigation.Back)
    }

    private fun finish() {
        val crop = state.value.data?.crop ?: return
        execute(
            loading = overlay(R.string.profile_saving_photo.toUiText()),
            action = {
                val area = CropArea(left = crop.left, top = crop.top, width = crop.width, height = crop.height)
                when (val cropped = imageCropper.crop(File(URI(args.uri)), area)) {
                    is Outcome.Success -> profileRepository.setAvatar("file://${cropped.data.path}")
                    is Outcome.Failure -> cropped
                }
            },
            onData = { navigate(AvatarCropNavigation.Back) },
        )
    }
}
