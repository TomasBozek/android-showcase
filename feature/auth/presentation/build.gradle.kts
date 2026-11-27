plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.auth.api)
    api(projects.feature.auth.domain)
    // The app lock's prompt.
    api(projects.service.biometric)

    testImplementation(testFixtures(projects.feature.auth.domain))
    testImplementation(testFixtures(projects.service.biometric))
}
