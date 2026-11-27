package com.example.androidproject1.feature.profile.data.repository

import com.example.androidproject1.feature.profile.data.source.LocalProfileDataSource
import com.example.androidproject1.feature.profile.data.test.FakeAvatarDataSource
import com.example.androidproject1.feature.profile.domain.AvatarUpload
import com.example.androidproject1.feature.profile.domain.Profile
import com.example.androidproject1.service.core.domain.error.ServerError
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.files.UploadProgress
import com.example.androidproject1.service.files.test.FakeUploader
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * No Room, no DataStore, no [android.content.Context]: [FakeAvatarDataSource] and an in-memory
 * [LocalProfileDataSource] are enough to test the one thing this repository does that is not
 * straight delegation — that `setAvatar` writes in order, and what a failed copy leaves behind.
 */
class DefaultProfileRepositoryTest {

    private val local = InMemoryLocalProfileDataSource()
    private val avatar = FakeAvatarDataSource()
    private val uploader = FakeUploader()
    private val repository = DefaultProfileRepository(
        logger = FakeLogger(),
        localProfileDataSource = local,
        avatarDataSource = avatar,
        uploader = uploader,
    )

    @Test
    fun `a stored avatar is recorded against the profile`() = runTest {
        val outcome = repository.setAvatar("content://picker/1")

        val stored = (outcome as Outcome.Success).data
        assertEquals("content://picker/1", avatar.stored)
        assertEquals(stored, local.read().avatarUri)
    }

    @Test
    fun `a save is emitted to an open observer`() = runTest {
        val seen = mutableListOf<Profile>()
        val collector = launch { repository.observeProfile().collect { seen += (it as Outcome.Success).data } }
        runCurrent()

        repository.save(Profile(name = "Ada", email = "ada@example.com", avatarUri = null))
        runCurrent()
        collector.cancel()

        assertEquals(listOf(Profile.EMPTY, Profile(name = "Ada", email = "ada@example.com", avatarUri = null)), seen)
    }

    @Test
    fun `a copy that fails leaves nothing recorded`() = runTest {
        avatar.failWith = IllegalStateException("no longer readable")

        val outcome = repository.setAvatar("content://picker/1")

        assertTrue(outcome is Outcome.Failure)
        assertNull(local.read().avatarUri)
    }

    @Test
    fun `the stored avatar is uploaded, and its progress is the domain's`() = runTest {
        local.writeAvatarUri("file:///data/avatar/avatar-1.jpg")
        uploader.script = listOf(UploadProgress.Sending(0f), UploadProgress.Sending(0.5f), UploadProgress.Done)

        val progress = repository.uploadAvatar().toList()

        assertEquals(listOf(AvatarUpload.Sending(0f), AvatarUpload.Sending(0.5f), AvatarUpload.Done), progress)
        val (file, url, type) = uploader.requested.single()
        assertEquals(File("/data/avatar/avatar-1.jpg"), file)
        assertEquals("profile/avatar", url)
        assertEquals("image/jpeg", type)
    }

    @Test
    fun `a refused upload is a failure that keeps the network's error, and the local avatar stays`() = runTest {
        local.writeAvatarUri("file:///data/avatar/avatar-1.jpg")
        val refused = ServerError()
        uploader.script = listOf(UploadProgress.Sending(0f), UploadProgress.Failed(refused))

        val last = repository.uploadAvatar().toList().last()

        assertEquals(AvatarUpload.Failed(refused), last)
        assertEquals("file:///data/avatar/avatar-1.jpg", local.read().avatarUri)
    }

    @Test
    fun `with no avatar there is nothing to send`() = runTest {
        val progress = repository.uploadAvatar().toList()

        assertTrue(progress.single() is AvatarUpload.Failed)
        assertTrue(uploader.requested.isEmpty())
    }

    private class InMemoryLocalProfileDataSource : LocalProfileDataSource {

        private val profile = MutableStateFlow(Profile.EMPTY)

        override suspend fun read(): Profile = profile.value

        override fun observe(): Flow<Profile> = profile

        override suspend fun writeDetails(name: String, email: String) {
            profile.value = profile.value.copy(name = name, email = email)
        }

        override suspend fun writeAvatarUri(uri: String) {
            profile.value = profile.value.copy(avatarUri = uri)
        }

        override suspend fun clear() {
            profile.value = Profile.EMPTY
        }
    }
}
