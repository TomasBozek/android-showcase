plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.home.api)

    // The greeting is the profile's name or the session's email — two frame features' domains.
    // Every door is contributed, so no sample is named here.
    api(projects.feature.profile.domain)
    api(projects.feature.auth.domain)

    // The fakes, each living with the interface it fakes.
    testImplementation(testFixtures(projects.feature.profile.domain))
    testImplementation(testFixtures(projects.feature.auth.domain))
}
