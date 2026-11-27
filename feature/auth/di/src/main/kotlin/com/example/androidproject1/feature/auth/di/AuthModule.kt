package com.example.androidproject1.feature.auth.di

import com.example.androidproject1.feature.auth.data.lock.DefaultAppLock
import com.example.androidproject1.feature.auth.data.repository.DefaultAuthRepository
import com.example.androidproject1.feature.auth.data.source.DefaultRemoteAuthDataSource
import com.example.androidproject1.feature.auth.data.source.RemoteAuthDataSource
import com.example.androidproject1.feature.auth.domain.AppLock
import com.example.androidproject1.feature.auth.domain.AuthRepository
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.domain.DefaultAuthService
import com.example.androidproject1.feature.auth.domain.TermsPolicy
import com.example.androidproject1.feature.auth.presentation.applock.AppLockViewModel
import com.example.androidproject1.feature.auth.presentation.forgotpassword.ForgotPasswordViewModel
import com.example.androidproject1.feature.auth.presentation.login.LoginViewModel
import com.example.androidproject1.feature.auth.presentation.resetpassword.ResetPasswordViewModel
import com.example.androidproject1.feature.auth.presentation.signup.SignUpViewModel
import com.example.androidproject1.feature.auth.presentation.verifyemail.VerifyEmailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

object AuthModule {

    val module: Module = module {
        viewModelOf(::LoginViewModel)
        viewModelOf(::SignUpViewModel)
        viewModelOf(::ForgotPasswordViewModel)
        viewModelOf(::ResetPasswordViewModel)
        viewModelOf(::VerifyEmailViewModel)
        viewModelOf(::AppLockViewModel)

        // Spelled out: `getAll()` gathers every feature's LocalDataEraser, which `verify`
        // cannot see through — KoinGraphTest names List::class for it.
        single<AuthService> { DefaultAuthService(authRepository = get(), erasers = getAll()) }
        // Spelled out rather than singleOf: the constructor has defaulted `newSessionId` and
        // `newTokens` lambdas for tests, which reflection would try to resolve from the graph.
        single<AuthRepository> {
            DefaultAuthRepository(
                logger = get(),
                sessionStore = get(),
                remoteAuthDataSource = get(),
                currentTermsVersion = get<TermsPolicy>().version,
            )
        }
        singleOf(::DefaultRemoteAuthDataSource) bind RemoteAuthDataSource::class
        // Spelled out for its scope: the rule runs for the life of the process, and a
        // CoroutineScope is not something the graph should hand out.
        single<AppLock>(createdAtStart = true) {
            DefaultAppLock(
                dataStoreProvider = get(),
                foreground = get(),
                clock = get(),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            )
        }
    }
}
