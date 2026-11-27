plugins {
    alias(libs.plugins.convention.feature.data)
    alias(libs.plugins.convention.android.room)
}

dependencies {
    api(projects.service.core.data)
    api(projects.feature.trips.domain)
    // A reminder is work that runs while the app is closed, and a notification when it does.
    api(projects.service.work)
    api(projects.service.notification)
    // The scheme of the deep link a reminder's tap opens is the application id.
    api(projects.service.device)

    testImplementation(testFixtures(projects.feature.trips.domain))
    testImplementation(testFixtures(projects.service.work))
    testImplementation(testFixtures(projects.service.notification))
    testImplementation(testFixtures(projects.service.device))
}
