plugins {
    alias(libs.plugins.convention.feature.data)
}

dependencies {
    api(projects.service.core.data)
    api(projects.feature.settings.domain)
    // A large file, streamed to disk by a job that runs with the app closed, with a notification.
    api(projects.service.files)
    api(projects.service.work)
    api(projects.service.notification)
    testImplementation(testFixtures(projects.service.files))
    testImplementation(testFixtures(projects.service.work))
    testImplementation(testFixtures(projects.service.notification))
}
