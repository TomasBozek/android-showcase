package com.example.androidproject1.feature.profile.presentation.avatarcrop

import androidx.compose.runtime.Immutable
import com.example.androidproject1.core.ui.component.content.ImageCrop

/**
 * The picture just picked, and which square of it the frame holds. [crop] is `null` until
 * the frame has drawn the picture and said so, and nothing can be cropped before that.
 */
@Immutable
data class AvatarCropState(
    val uri: String,
    val crop: ImageCrop? = null,
) {

    val canFinish: Boolean get() = crop != null

    companion object {

        val PREVIEW = AvatarCropState(
            uri = "file:///cache/picked-1.jpg",
            crop = ImageCrop(left = 0.25f, top = 0f, width = 0.5f, height = 1f),
        )
    }
}
