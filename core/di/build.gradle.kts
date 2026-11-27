plugins {
    alias(libs.plugins.convention.feature.di)
}

dependencies {
    // The HttpClient is assembled here; the engine arrives from :app.
    implementation(projects.service.network)
    // The permission checker is bound here, once for the process.
    implementation(projects.service.permission)
    // `api`, and deliberately: this is the app's single aggregation point, so the destinations each
    // feature's di module re-exports reach :app through here. `buildHealth` reads that as a
    // violation because nothing in Koin.kt's signature mentions them.
    api(projects.feature.auth.di)
    api(projects.feature.home.di)
    api(projects.feature.settings.di)
    api(projects.feature.catalog.di)
    api(projects.feature.gallery.di)
    api(projects.feature.cart.di)
    api(projects.feature.profile.di)
    api(projects.feature.devmenu.di)
    api(projects.feature.onboarding.di)
    api(projects.feature.trips.di)
    api(projects.feature.inventory.di)
    api(projects.feature.movies.di)
    api(projects.feature.weather.di)
    api(projects.feature.apphold.di)
    api(projects.feature.checkout.di)
    api(projects.feature.orders.di)

    implementation(projects.service.core.data)
    implementation(projects.service.core.domain)
    implementation(projects.service.log.data)
    implementation(projects.service.telemetry)
    // The network log's buffer, fed by the HTTP client's logging.
    implementation(projects.service.devtools)
    implementation(projects.service.session)
    implementation(projects.service.work)
    implementation(projects.service.notification)
    implementation(projects.service.files)
    implementation(projects.service.location)
    implementation(projects.service.media)
    implementation(projects.service.intents)
    implementation(projects.service.device)
    implementation(projects.service.biometric)
    implementation(projects.service.play)
    implementation(projects.service.scanner)
}
