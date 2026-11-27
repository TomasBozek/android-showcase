package com.example.androidproject1.feature.profile.domain.test

import com.example.androidproject1.feature.profile.domain.AvatarUpload
import com.example.androidproject1.feature.profile.domain.Profile
import com.example.androidproject1.feature.profile.domain.ProfileRepository
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * In-memory [ProfileRepository], beside the interface it fakes.
 *
 * It lived in `:feature:profile:presentation`'s tests while only that module tested against it;
 * Home greets by the profile's name now, which is the day the KDoc said to move it here.
 *
 * @property profile the stored profile, settable, so a test starts from a filled one without a save.
 * @property upload what every [uploadAvatar] plays — a `Channel`'s flow, to step one through a test.
 */
class FakeProfileRepository(
    initial: Profile = Profile.EMPTY,
    var failWith: DomainError? = null,
) : ProfileRepository {

    val profile = MutableStateFlow(initial)

    /** What `save` was last called with, so a test can assert the values and not just the call. */
    var saved: Profile? = null
        private set

    override suspend fun get(): Outcome<Profile> =
        failWith?.let { Outcome.Failure(it) } ?: Outcome.Success(profile.value)

    override fun observeProfile(): Flow<Outcome<Profile>> =
        profile.map { failWith?.let { error -> Outcome.Failure(error) } ?: Outcome.Success(it) }

    override suspend fun save(profile: Profile): Outcome<Unit> {
        failWith?.let { return Outcome.Failure(it) }
        saved = profile
        this.profile.value = profile
        return Outcome.Success(Unit)
    }

    override suspend fun setAvatar(sourceUri: String): Outcome<String> {
        failWith?.let { return Outcome.Failure(it) }
        // The real one copies the bytes and hands back a URI of its own. What a caller depends on
        // is exactly that: what it gets back is not the URI it passed in.
        val stored = "file:///data/avatar/stored.jpg"
        profile.value = profile.value.copy(avatarUri = stored)
        return Outcome.Success(stored)
    }

    var upload: Flow<AvatarUpload> = flowOf(AvatarUpload.Done)

    /** How many times [uploadAvatar] was collected — each one is an upload. */
    var uploads = 0
        private set

    override fun uploadAvatar(): Flow<AvatarUpload> = upload.onStart { uploads++ }
}
