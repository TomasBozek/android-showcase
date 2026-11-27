plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // `Outcome`, `DispatcherProvider` and `TemperatureUnit`, which `Formats` writes.
    api(projects.service.core.domain)
}
