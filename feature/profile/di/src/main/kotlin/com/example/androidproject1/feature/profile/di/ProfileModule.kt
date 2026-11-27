package com.example.androidproject1.feature.profile.di

import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.profile.data.repository.DefaultProfileEraser
import com.example.androidproject1.feature.profile.data.repository.DefaultProfileRepository
import com.example.androidproject1.feature.profile.data.source.AvatarDataSource
import com.example.androidproject1.feature.profile.data.source.DefaultAvatarDataSource
import com.example.androidproject1.feature.profile.data.source.DefaultLocalProfileDataSource
import com.example.androidproject1.feature.profile.data.source.LocalProfileDataSource
import com.example.androidproject1.feature.profile.domain.ProfileRepository
import com.example.androidproject1.feature.profile.presentation.avatarcrop.AvatarCropViewModel
import com.example.androidproject1.feature.profile.presentation.profile.ProfileViewModel
import com.example.androidproject1.feature.profile.presentation.profileDoor
import com.example.androidproject1.feature.profile.presentation.profileJumps
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.media.AndroidImageCropper
import com.example.androidproject1.service.media.ImageCropper
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

object ProfileModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:profile")) { profileDoor(get()) }
        // The screens the debug menu jumps to; read with getAll() where its modules are bound.
        profileJumps.forEach { jump -> single<ShellJump>(named("jump:${jump.id}")) { jump } }
        viewModelOf(::ProfileViewModel)
        viewModelOf(::AvatarCropViewModel)

        // The avatar's crop: `:service:media`'s, bound where its one caller is.
        single<ImageCropper> { AndroidImageCropper(files = get(), dispatchers = get()) }
        singleOf(::DefaultProfileRepository) bind ProfileRepository::class
        singleOf(::DefaultLocalProfileDataSource) bind LocalProfileDataSource::class
        singleOf(::DefaultAvatarDataSource) bind AvatarDataSource::class
        // What "delete my account" wipes here; the auth feature collects every one with getAll().
        singleOf(::DefaultProfileEraser) bind LocalDataEraser::class
    }
}
