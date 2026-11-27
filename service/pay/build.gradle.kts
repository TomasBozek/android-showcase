plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // `Outcome` and `DomainError`: a payment that did not happen is a failure a caller can show.
    api(projects.service.core.domain)

    testImplementation(testFixtures(projects.service.core.domain))
}
