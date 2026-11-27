package com.example.androidproject1.feature.home.di

import com.example.androidproject1.core.ui.shell.ShellCreations
import com.example.androidproject1.feature.home.presentation.home.HomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

object HomeModule {

    val module: Module = module {
        // What the speed dial offers: every feature's `ShellCreation`, read once with getAll().
        single { ShellCreations(getAll()) }
        viewModelOf(::HomeViewModel)
    }
}
