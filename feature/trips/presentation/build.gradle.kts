plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    // The reminder switch asks for POST_NOTIFICATIONS through the permission request.
    implementation(projects.service.permission)
    api(projects.feature.trips.api)
    api(projects.feature.trips.domain)
    // Another feature's domain, which is allowed: a saved trip is a milestone for the review ask.
    // `api`, because the wizard's constructor names it and `:feature:trips:di` builds the wizard.
    api(projects.feature.settings.domain)

    // The fakes live with the interfaces they fake.
    testImplementation(testFixtures(projects.feature.trips.domain))
    testImplementation(testFixtures(projects.feature.settings.domain))
    // FakeFormDrafts, for the wizard's draft across launches.
    testImplementation(testFixtures(projects.service.core.domain))
}
