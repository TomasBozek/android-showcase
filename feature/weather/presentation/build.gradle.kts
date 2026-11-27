plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.weather.api)
    api(projects.feature.weather.domain)
    // One fix and the name of the place it is in; `api`, because the view model names both.
    api(projects.service.location)
    // "Use my location" asks for the permission through the gate.
    implementation(projects.service.permission)

    testImplementation(testFixtures(projects.service.location))
}
