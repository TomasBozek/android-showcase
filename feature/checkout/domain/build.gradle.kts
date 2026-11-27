plugins {
    alias(libs.plugins.convention.kotlin.jvm)
    // FakeCheckoutRepository: every step of the wizard is tested against it.
    `java-test-fixtures`
}

dependencies {
    api(projects.service.core.domain)
    // An order is for the cart's lines, and placing one empties it.
    api(projects.feature.cart.domain)
}
