plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // The ports it implements: `TokenStore`, `TokenRefresher` and `SessionEnder`.
    api(projects.service.network)
    // The encrypted store the session is kept in.
    api(projects.service.core.data)

    testImplementation(testFixtures(projects.service.core.domain))
    testImplementation(testFixtures(projects.service.log.domain))
}
