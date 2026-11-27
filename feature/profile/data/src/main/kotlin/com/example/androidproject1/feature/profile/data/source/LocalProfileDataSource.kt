package com.example.androidproject1.feature.profile.data.source

import com.example.androidproject1.feature.profile.domain.Profile
import kotlinx.coroutines.flow.Flow

/**
 * Internal to the data layer: it sits beside its implementation, and nothing above
 * `:feature:profile:data` names it. What the rest of the app depends on is the repository
 * interface in `domain`.
 */
interface LocalProfileDataSource {

    /** [Profile.EMPTY] when nothing has been saved yet — a first run is not a failure. */
    suspend fun read(): Profile

    /** The same profile as [read], and again after every write. */
    fun observe(): Flow<Profile>

    /** Name and e-mail only; the avatar has its own write because it is saved on its own. */
    suspend fun writeDetails(name: String, email: String)

    suspend fun writeAvatarUri(uri: String)

    /** Removes the three keys — what deleting the account does; [Profile.EMPTY] reads back. */
    suspend fun clear()
}
