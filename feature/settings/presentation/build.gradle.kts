plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    // The version, OS and model About shows and a feedback mail carries.
    api(projects.service.device)
    // The step-up before an account is deleted.
    api(projects.service.biometric)
    // The Reminders switch and the permission's status, which a view model reads without a `Context`.
    api(projects.service.notification)
    api(projects.service.permission)
    // Its route keys and deep links.
    api(projects.feature.settings.api)
    api(projects.feature.settings.domain)
    // Another feature's `domain` is fair game; its `presentation` is not — see doctor.py.
    api(projects.feature.auth.domain)
    api(projects.feature.profile.domain)
    api(projects.feature.onboarding.domain)

    testImplementation(testFixtures(projects.feature.auth.domain))
    testImplementation(testFixtures(projects.feature.profile.domain))
    testImplementation(testFixtures(projects.feature.onboarding.domain))
    testImplementation(testFixtures(projects.feature.settings.domain))
    testImplementation(testFixtures(projects.service.biometric))
    testImplementation(testFixtures(projects.service.notification))
    testImplementation(testFixtures(projects.service.device))
    testImplementation(testFixtures(projects.service.locale))
    testImplementation(testFixtures(projects.service.permission))
}
