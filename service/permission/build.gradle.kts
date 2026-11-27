plugins {
    alias(libs.plugins.convention.service)
}

android {
    resourcePrefix = "permission_"
}

dependencies {
    // `UiText` for the rationale's copy, and `openAppSettings()` beside the other platform commands.
    api(projects.service.core.ui)
}
