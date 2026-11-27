plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.checkout.api)
    api(projects.feature.checkout.domain)
    // A placed order gives way to the orders feature's screen, pushed by its api route.
    implementation(projects.feature.orders.api)
    // The step-up before an order is placed.
    api(projects.service.biometric)
    // Google Pay on the payment step; its sum is the cart's.
    api(projects.service.pay)
    api(projects.feature.cart.domain)

    testImplementation(testFixtures(projects.feature.checkout.domain))
    testImplementation(testFixtures(projects.feature.cart.domain))
    testImplementation(testFixtures(projects.service.biometric))
    testImplementation(testFixtures(projects.service.pay))
}
