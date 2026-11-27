plugins {
    alias(libs.plugins.convention.core.ui)
}

android {
    // This app's own design system, so its resources are `app_*` — the same reason
    // :service:core:ui ships `core_*`. Lint's ResourceName check is an error, so an unprefixed
    // string here stops the build rather than colliding with a consuming app's later.
    resourcePrefix = "app_"
}

dependencies {
    // Re-exported so a feature's `presentation` module needs only `api(projects.core.ui)` to get
    // both this app's theme and the reusable architecture in `:service:core:ui`.
    api(projects.service.core.ui)
    // Likewise: a destination navigates through `Navigator`, and a form guards back with
    // `DiscardBackHandler`, with no dependency line of its own.
    api(projects.service.navigation)
    // And a screen asks for a permission through `:service:permission` the same way.
    api(projects.service.permission)
    // AppScaffold reports the screen view, and a view model counts an event, through telemetry.
    api(projects.service.telemetry)
    // `LocalFormats`, which a screen asks for a number's role.
    api(projects.service.locale)

    // PreviewScreenshotSpec, which this module's own PreviewScreenshotTest subclasses. A feature's
    // `presentation` module gets the same line from `convention.feature.presentation`; :core:ui
    // applies the compose library plugin directly, so it asks for itself.
    testImplementation(testFixtures(projects.service.core.ui))
}
