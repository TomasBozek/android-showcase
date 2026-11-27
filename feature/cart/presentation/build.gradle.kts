plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.cart.api)
    // Another feature's routes, pushed through the Navigator.
    implementation(projects.feature.catalog.api)
    implementation(projects.feature.checkout.api)
    api(projects.feature.cart.domain)

    testImplementation(testFixtures(projects.feature.cart.domain))
}
