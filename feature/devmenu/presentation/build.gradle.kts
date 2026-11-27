plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.devmenu.api)
    // Build info, the switches' ports and the network log the menu draws.
    api(projects.service.devtools)
    // The fake Play update the debug menu drives on `dev`.
    api(projects.service.play)
    // The session row, the reminder and the What's new reset are seams `:app` binds per feature.

    // FakeCrashLog, for the crash reports row.
    testImplementation(testFixtures(projects.service.telemetry))
}
