package com.example.androidproject1.feature.profile.domain

import com.example.androidproject1.service.core.domain.error.DomainError

/**
 * How the avatar's upload is going — `:service:files`' `UploadProgress` in the domain's own
 * words, because this module is Kotlin/JVM and cannot name an Android library's type.
 */
sealed interface AvatarUpload {

    /** [fraction] of the picture sent, `0f` to `1f`. */
    data class Sending(val fraction: Float) : AvatarUpload

    data object Done : AvatarUpload

    /** The local avatar stays as it is; sending it again is a new [ProfileRepository.uploadAvatar]. */
    data class Failed(val error: DomainError) : AvatarUpload
}
