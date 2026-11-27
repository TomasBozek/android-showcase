plugins {
    alias(libs.plugins.convention.service)
}

android {
    resourcePrefix = "notification_"
}

dependencies {
    // `UiText`: a notification's copy is resolved when it is posted, not when it is asked for.
    api(projects.service.core.ui)
    // The switches are kept in the app's preferences file.
    api(projects.service.core.data)
}
