plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // `LoggingAnalytics`, `LoggingErrorTracker` and `TrackingLogger` all take a Logger.
    api(projects.service.log.domain)
    // The install id is erased with the rest of the account's data.
    api(projects.service.core.domain)

    testImplementation(testFixtures(projects.service.log.domain))
    // TestDispatchers, for the crash log's disk work.
    testImplementation(testFixtures(projects.service.core.domain))
}
