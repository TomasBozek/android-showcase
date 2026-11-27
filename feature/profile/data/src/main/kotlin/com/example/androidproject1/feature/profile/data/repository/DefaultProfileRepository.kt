package com.example.androidproject1.feature.profile.data.repository

import com.example.androidproject1.feature.profile.data.source.AvatarDataSource
import com.example.androidproject1.feature.profile.data.source.LocalProfileDataSource
import com.example.androidproject1.feature.profile.domain.AvatarUpload
import com.example.androidproject1.feature.profile.domain.Profile
import com.example.androidproject1.feature.profile.domain.ProfileRepository
import com.example.androidproject1.service.core.data.BaseRepository
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.error.UnexpectedError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.files.UploadProgress
import com.example.androidproject1.service.files.Uploader
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.net.URI

class DefaultProfileRepository(
    logger: Logger,
    private val localProfileDataSource: LocalProfileDataSource,
    private val avatarDataSource: AvatarDataSource,
    private val uploader: Uploader,
) : ProfileRepository, BaseRepository(logger = logger.withTag("DefaultProfileRepository")) {

    override suspend fun get(): Outcome<Profile> = execute { localProfileDataSource.read() }

    override fun observeProfile(): Flow<Outcome<Profile>> = observe(source = localProfileDataSource.observe())

    override suspend fun save(profile: Profile): Outcome<Unit> = execute {
        localProfileDataSource.writeDetails(name = profile.name, email = profile.email)
    }

    override suspend fun setAvatar(sourceUri: String): Outcome<String> = execute {
        // Stored before it is recorded: a preference pointing at a file that was never written is
        // worse than no preference at all.
        val stored = avatarDataSource.store(sourceUri)
        localProfileDataSource.writeAvatarUri(stored)
        stored
    }

    override fun uploadAvatar(): Flow<AvatarUpload> = flow {
        val stored = localProfileDataSource.read().avatarUri
        if (stored == null) {
            emit(AvatarUpload.Failed(UnexpectedError("No avatar to upload")))
            return@flow
        }
        // The crop writes a square JPEG, so that is what the server is told it gets.
        emitAll(uploader.upload(File(URI(stored)), AVATAR_URL, JPEG).map { it.toDomain() })
    }

    private fun UploadProgress.toDomain(): AvatarUpload = when (this) {
        is UploadProgress.Sending -> AvatarUpload.Sending(fraction)
        UploadProgress.Done -> AvatarUpload.Done
        is UploadProgress.Failed -> AvatarUpload.Failed(error as? DomainError ?: UnexpectedError(cause = error))
    }

    private companion object {

        const val AVATAR_URL = "profile/avatar"
        const val JPEG = "image/jpeg"
    }
}
