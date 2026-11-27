plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // The prompt's title is a `UiText`, so a view model can name it without a `Context`.
    api(projects.service.core.ui)
}
