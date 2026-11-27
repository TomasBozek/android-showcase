plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.apphold.api)
    // Play's immediate update, in place of the store page.
    api(projects.service.play)

    testImplementation(testFixtures(projects.service.play))
}
