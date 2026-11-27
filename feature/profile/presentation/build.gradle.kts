plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    // Its route keys and deep links.
    api(projects.feature.profile.api)
    api(projects.feature.profile.domain)
    // The door's figure is who is signed in: the session is auth's, another feature's domain.
    api(projects.feature.auth.domain)
    // A picture from the library or the camera, as one call.
    api(projects.service.media)

    testImplementation(testFixtures(projects.feature.profile.domain))
    testImplementation(testFixtures(projects.service.media))
}
