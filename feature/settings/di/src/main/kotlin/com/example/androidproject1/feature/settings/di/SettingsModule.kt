package com.example.androidproject1.feature.settings.di

import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJump
import com.example.androidproject1.feature.settings.data.download.DefaultSampleDownload
import com.example.androidproject1.feature.settings.data.download.SampleDownloadBoard
import com.example.androidproject1.feature.settings.data.download.SampleDownloadConfig
import com.example.androidproject1.feature.settings.data.download.SampleDownloadHandler
import com.example.androidproject1.feature.settings.data.repository.DefaultReviewRepository
import com.example.androidproject1.feature.settings.data.repository.DefaultThemeRepository
import com.example.androidproject1.feature.settings.data.source.DefaultLocalReviewDataSource
import com.example.androidproject1.feature.settings.data.source.DefaultLocalThemeDataSource
import com.example.androidproject1.feature.settings.data.source.LocalReviewDataSource
import com.example.androidproject1.feature.settings.data.source.LocalThemeDataSource
import com.example.androidproject1.feature.settings.domain.ReviewRepository
import com.example.androidproject1.feature.settings.domain.SampleDownload
import com.example.androidproject1.feature.settings.domain.ThemeRepository
import com.example.androidproject1.feature.settings.presentation.about.AboutViewModel
import com.example.androidproject1.feature.settings.presentation.language.SettingsLanguageViewModel
import com.example.androidproject1.feature.settings.presentation.permissions.SettingsPermissionsViewModel
import com.example.androidproject1.feature.settings.presentation.settings.SettingsViewModel
import com.example.androidproject1.feature.settings.presentation.settingsDoor
import com.example.androidproject1.feature.settings.presentation.settingsJumps
import com.example.androidproject1.feature.settings.presentation.settingsstorage.SettingsStorageViewModel
import com.example.androidproject1.service.work.WorkHandler
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

object SettingsModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:settings")) { settingsDoor(get()) }
        // The screens the debug menu jumps to; read with getAll() where its modules are bound.
        settingsJumps.forEach { jump -> single<ShellJump>(named("jump:${jump.id}")) { jump } }
        viewModelOf(::SettingsViewModel)
        viewModelOf(::SettingsPermissionsViewModel)
        viewModelOf(::SettingsLanguageViewModel)
        viewModelOf(::AboutViewModel)
        viewModelOf(::SettingsStorageViewModel)

        singleOf(::DefaultThemeRepository) bind ThemeRepository::class
        singleOf(::DefaultLocalThemeDataSource) bind LocalThemeDataSource::class
        singleOf(::DefaultReviewRepository) bind ReviewRepository::class
        singleOf(::DefaultLocalReviewDataSource) bind LocalReviewDataSource::class

        // The 100 MB sample download: the job, where it is, and the port the screen holds.
        single { SampleDownloadConfig() }
        singleOf(::SampleDownloadBoard)
        singleOf(::SampleDownloadHandler) bind WorkHandler::class
        singleOf(::DefaultSampleDownload) bind SampleDownload::class
    }
}
