package com.example.androidproject1.feature.apphold.di

import com.example.androidproject1.feature.apphold.presentation.apphold.AppHoldViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

object AppHoldModule {

    val module: Module = module {
        viewModelOf(::AppHoldViewModel)
    }
}
