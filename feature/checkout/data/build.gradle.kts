plugins {
    alias(libs.plugins.convention.feature.data)
}

dependencies {
    api(projects.service.core.data)
    api(projects.service.network)
    api(projects.feature.checkout.domain)
    // A placed order is recorded where the orders feature lists it.
    api(projects.feature.orders.domain)

    testImplementation(testFixtures(projects.feature.cart.domain))
    testImplementation(testFixtures(projects.feature.orders.domain))
}
