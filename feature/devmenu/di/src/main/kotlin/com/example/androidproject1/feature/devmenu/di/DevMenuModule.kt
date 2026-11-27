package com.example.androidproject1.feature.devmenu.di

import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJumps
import com.example.androidproject1.feature.devmenu.presentation.DevMenuHooks
import com.example.androidproject1.feature.devmenu.presentation.devMenuDoor
import com.example.androidproject1.feature.devmenu.presentation.devmenu.DevMenuViewModel
import com.example.androidproject1.feature.devmenu.presentation.networklog.NetworkLogViewModel
import com.example.androidproject1.feature.devmenu.presentation.playground.DevMenuPlaygroundViewModel
import com.example.androidproject1.feature.devmenu.presentation.playgroundDoor
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * `BuildInfo` and `OfflineSwitch` are bound by `:app` — only the application module can read
 * `BuildConfig`, and only its `dev` source set has a fixture engine to switch off.
 */
object DevMenuModule {

    val module: Module = module {
        // Its door in the shell: Home, the drawer and Profile read every door with getAll().
        single<ShellDoor>(named("door:devMenu")) { devMenuDoor() }
        single<ShellDoor>(named("door:playground")) { playgroundDoor() }
        // The jump list: every feature's `ShellJump`, read once — bound only where this module is.
        single { ShellJumps(getAll()) }
        // The rows for features the menu cannot name: whichever seams `:app` bound.
        single { DevMenuHooks(session = getOrNull(), reminder = getOrNull(), whatsNew = getOrNull()) }
        viewModelOf(::DevMenuViewModel)
        viewModelOf(::DevMenuPlaygroundViewModel)
        viewModelOf(::NetworkLogViewModel)
    }
}
