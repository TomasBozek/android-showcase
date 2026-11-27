package com.example.androidproject1.feature.profile.domain

import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow

/**
 * Implemented in the data layer. Declared here so the domain layer depends on nothing.
 *
 * Two reads: [get] backs the form — a flow that re-emitted while someone was typing would
 * overwrite what they typed — and [observeProfile] is for what shows the profile rather than
 * edits it, a greeting or an account header, which has to follow a save.
 */
interface ProfileRepository {

    suspend fun get(): Outcome<Profile>

    /** The stored profile, and every save after it; [Profile.EMPTY] before the first. */
    fun observeProfile(): Flow<Outcome<Profile>>

    suspend fun save(profile: Profile): Outcome<Unit>

    /**
     * Copies the picture at [sourceUri] into storage this app owns and records it as the avatar.
     *
     * Both routes end here — the Photo Picker's grant lasts only as long as the process, and the
     * camera writes to a cache file that is not ours to keep. Returns the stored `file://` URI,
     * which is new on every call so an image loader cannot serve the previous picture from cache.
     */
    suspend fun setAvatar(sourceUri: String): Outcome<String>

    /**
     * Sends the stored avatar to the server, ending at [AvatarUpload.Done] or
     * [AvatarUpload.Failed]; a failure leaves the local avatar where it is. Cold: each collection
     * is one upload, so a retry is collecting it again.
     */
    fun uploadAvatar(): Flow<AvatarUpload>
}
