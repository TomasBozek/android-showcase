plugins {
    alias(libs.plugins.convention.android.application)
}

dependencies {
    // Pulls in every core layer and every feature's `di` module, and through them the whole graph.
    // `implementation`, not `api`: nothing consumes the application module.
    implementation(projects.core.di)

    // Declared rather than reached transitively through :core:di: MainActivity uses AppTheme,
    // and MainViewModel uses AuthService.
    implementation(projects.core.ui)
    implementation(projects.feature.auth.domain)
    // AppNavHost turns a picked product into a cart line and drives the tab badge, so it names
    // both domains. Another feature's *domain* is allowed; its presentation is not.
    implementation(projects.feature.cart.domain)
    implementation(projects.feature.catalog.domain)
    // Home's inventory count is faked in AppNavHostAnalyticsTest, so the test names the interface.
    implementation(projects.feature.inventory.domain)
    // MainActivity draws the whole app in the stored theme, so it reads the preference.
    implementation(projects.feature.settings.domain)
    // The stored `seen` flag decides whether the app opens on the tour.
    implementation(projects.feature.onboarding.domain)
    // `wiring.checkout` binds CheckoutPrefill, which `dev` fills with an example address and card.
    implementation(projects.feature.checkout.domain)
    // `wiring.trips` gives the debug menu its reminder over the trips.
    implementation(projects.feature.trips.domain)

    // The HttpClient's engine is chosen per flavor: fixtures on dev, OkHttp elsewhere —
    // both engines come from `convention.android.application`, which owns the flavors.
    implementation(projects.service.network)
    // The debug menu's switches and build info are :app's to bind (DebugMenu, ApplicationModule):
    // declared here, not reached through :feature:devmenu, so the app builds without that feature.
    implementation(projects.service.devtools)
    // The platform's connectivity monitor, bound by `prod` and `staging` (dev binds the fixture's).
    implementation(projects.service.connectivity)
    // MainViewModel collects the session's expiry.
    implementation(projects.service.session)
    // `ApplicationModule` binds Settings' review port over it and says which flavors use the fakes.
    implementation(projects.service.play)
    // Google Pay's sheet, which the debug menu can pretend is ready.
    implementation(projects.service.pay)
    // MainViewModel reads the version code.
    implementation(projects.service.device)
    // The channels are created in `App`, the icon is `NotificationConfig`.
    implementation(projects.service.notification)
    // `App` is WorkManager's configuration provider.
    implementation(projects.service.work)
    // The dismissed server messages are :app's own, in the shared preferences file.
    implementation(projects.service.core.data)
    // The crash records go to the share sheet or a mail app, as a file the store serves.
    implementation(projects.service.intents)
    implementation(projects.service.files)

    // MainDispatcherRule + FakeLogger, and FakeAuthService, for MainViewModelTest. Every other
    // module gets the first line from `convention.feature.presentation`; :app is not one.
    testImplementation(testFixtures(projects.service.core.ui))
    // FileLogFlavorTest names the sink the debug menu's graph binds.
    testImplementation(projects.service.log.data)
    testImplementation(testFixtures(projects.service.telemetry))
    testImplementation(testFixtures(projects.service.device))
    testImplementation(testFixtures(projects.service.notification))
    testImplementation(testFixtures(projects.service.play))
    testImplementation(testFixtures(projects.service.pay))
    testImplementation(testFixtures(projects.feature.auth.domain))
    testImplementation(testFixtures(projects.feature.settings.domain))
    testImplementation(testFixtures(projects.feature.onboarding.domain))
    testImplementation(testFixtures(projects.feature.catalog.domain))
    testImplementation(testFixtures(projects.feature.cart.domain))
    testImplementation(testFixtures(projects.feature.inventory.domain))
    testImplementation(testFixtures(projects.feature.trips.domain))
    testImplementation(testFixtures(projects.feature.profile.domain))
    testImplementation(testFixtures(projects.feature.orders.domain))
    // AppNavHostAnalyticsTest fakes the shows' favourites and the weather for Home's doors.
    testImplementation(projects.feature.movies.domain)
    testImplementation(projects.feature.weather.domain)
}
