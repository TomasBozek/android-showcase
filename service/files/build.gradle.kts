plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // The Ktor client the downloader streams with, and `HttpErrorMapper` for its failures.
    api(projects.service.network)
}
