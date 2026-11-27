package com.example.androidproject1.feature.profile.data.repository

import com.example.androidproject1.feature.profile.data.source.AvatarDataSource
import com.example.androidproject1.feature.profile.data.source.LocalProfileDataSource
import com.example.androidproject1.service.core.domain.LocalDataEraser

/**
 * The profile feature's share of "delete my account": the three keys in the shared
 * DataStore and the avatar directory under `filesDir`. Nothing else in the store is the
 * profile's, so nothing else goes.
 */
class DefaultProfileEraser(
    private val localProfileDataSource: LocalProfileDataSource,
    private val avatarDataSource: AvatarDataSource,
) : LocalDataEraser {

    override suspend fun erase() {
        localProfileDataSource.clear()
        avatarDataSource.clear()
    }
}
