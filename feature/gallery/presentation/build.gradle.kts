plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.gallery.api)
    // Another feature's routes, pushed through the Navigator.
    implementation(projects.feature.devmenu.api)
}
