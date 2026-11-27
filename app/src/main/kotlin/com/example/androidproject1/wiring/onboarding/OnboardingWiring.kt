package com.example.androidproject1.wiring.onboarding

import com.example.androidproject1.feature.devmenu.presentation.WhatsNewReset
import com.example.androidproject1.feature.onboarding.domain.OnboardingRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The debug menu's What's new reset, over the onboarding feature's stored version code.
 * `delete_feature.py onboarding` deletes this package and its include with it.
 */
val onboardingDevMenuWiring: Module = module {
    single<WhatsNewReset> {
        val onboarding = get<OnboardingRepository>()
        object : WhatsNewReset {
            // Version 1 rather than 0: 0 is a first run, which shows nothing.
            override suspend fun reset() {
                onboarding.setLastSeenVersionCode(1)
            }
        }
    }
}
