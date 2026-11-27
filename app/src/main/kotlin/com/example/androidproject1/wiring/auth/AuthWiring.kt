package com.example.androidproject1.wiring.auth

import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.devmenu.presentation.SessionReadout
import com.example.androidproject1.service.core.domain.result.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * What only `:app` can give the debug menu about the session: the signed-in address, read
 * from the auth feature the menu cannot name. `ApplicationModule` includes it where there is a
 * menu, and `delete_feature.py auth` deletes this package and that line with it.
 */
val authDevMenuWiring: Module = module {
    single<SessionReadout> { AuthSessionReadout(get()) }
}

internal class AuthSessionReadout(private val authService: AuthService) : SessionReadout {

    override fun signedIn(): Flow<String?> = authService.observeSession().map { outcome ->
        (outcome as? Outcome.Success)?.data?.email
    }
}
