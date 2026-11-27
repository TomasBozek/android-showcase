plugins {
    alias(libs.plugins.convention.service)
}

android {
    // Keeps this module's string names from silently colliding with a consuming app's. The only
    // Android configuration left in a module build file, because it is the only one that is not
    // shared — see the convention plugins in build-logic/.
    resourcePrefix = "core_"
}

dependencies {
    api(projects.service.core.domain)
    // Every view model logs; the port is `:service:log:domain`'s.
    api(projects.service.log.domain)
    // `Screen()` opens a `UiCommand.Navigate` through its app navigator.
    implementation(projects.service.navigation)
    // The commands `Screen()` resolves leave the app through one place.
    implementation(projects.service.intents)

    // Re-exported: a module that takes these fixtures for MainDispatcherRule gets FakeLogger too,
    // so a screen test still needs one testFixtures line rather than two.
    testFixturesApi(testFixtures(projects.service.core.domain))
    testFixturesApi(testFixtures(projects.service.log.domain))

    testImplementation(testFixtures(projects.service.core.domain))
    testImplementation(testFixtures(projects.service.log.domain))
}
