plugins {
    alias(libs.plugins.convention.feature.data)
    alias(libs.plugins.convention.android.room)
}

dependencies {
    api(projects.service.core.data)
    api(projects.service.network)
    api(projects.feature.orders.domain)

    testImplementation(testFixtures(projects.feature.orders.domain))
}
