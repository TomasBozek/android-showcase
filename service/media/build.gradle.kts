plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // `Outcome`: a picture that could not be had is a failure a caller can show.
    api(projects.service.core.domain)
    // Where a capture is written and where a picked file lands, and the URI the camera writes to.
    api(projects.service.files)

    testImplementation(testFixtures(projects.service.files))
    testImplementation(testFixtures(projects.service.core.domain))
}
