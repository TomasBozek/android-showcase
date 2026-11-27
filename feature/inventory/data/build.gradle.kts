plugins {
    alias(libs.plugins.convention.feature.data)
    alias(libs.plugins.convention.android.room)
}

dependencies {
    api(projects.service.core.data)
    api(projects.feature.inventory.domain)
    // The outbox: its writes are sent over the network, by work that runs while the app is closed.
    api(projects.service.network)
    api(projects.service.work)

    testImplementation(testFixtures(projects.service.work))
}
