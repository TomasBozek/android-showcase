plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.orders.api)
    api(projects.feature.orders.domain)
    // The empty list leads to the catalog's tab, by its api route.
    implementation(projects.feature.catalog.api)

    testImplementation(testFixtures(projects.feature.orders.domain))
}
