plugins {
    alias(libs.plugins.convention.android.application)
}

dependencies {
    // Pulls in every core layer and every feature's `di` module, and through them the whole graph.
    // `implementation`, not `api`: nothing consumes the application module.
    implementation(projects.core.di)

    // Declared rather than reached transitively through :core:di: MainActivity uses AppTheme.
    implementation(projects.core.ui)

    // DeepLinks parses the app's own links into keys.
    implementation(projects.service.deeplink)
    // The navigator, the result store and the app navigator MainActivity provides.
    implementation(projects.service.navigation)

    // The HttpClient's engine is chosen per flavor: fixtures on dev, OkHttp elsewhere —
    // both engines come from `convention.android.application`, which owns the flavors.
    implementation(projects.service.network)
    // The debug menu's switches and build info are :app's to bind (DebugMenu, ApplicationModule).
    implementation(projects.service.devtools)
    // The platform's connectivity monitor, bound by `prod` and `staging` (dev binds the fixture's).
    implementation(projects.service.connectivity)
    // `ApplicationModule` says which flavors use Play's fakes.
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

    // MainDispatcherRule + FakeLogger, for MainViewModelTest. Every other module gets them from
    // `convention.feature.presentation`; :app is not one.
    testImplementation(testFixtures(projects.service.core.ui))
    testImplementation(projects.service.log.data)
    testImplementation(testFixtures(projects.service.telemetry))
    testImplementation(testFixtures(projects.service.device))
    testImplementation(testFixtures(projects.service.notification))
    testImplementation(testFixtures(projects.service.play))
    testImplementation(testFixtures(projects.service.pay))
}
