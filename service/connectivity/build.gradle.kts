plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // The port it implements.
    api(projects.service.network)

    testImplementation(testFixtures(projects.service.core.domain))
}
