plugins {
    alias(libs.plugins.convention.feature.api)
}

dependencies {
    api(projects.service.deeplink)
    // Profile is reached from the Settings tab, so its door opens Settings first.
    api(projects.feature.settings.api)
}
