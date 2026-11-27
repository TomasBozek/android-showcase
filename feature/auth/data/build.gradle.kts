plugins {
    alias(libs.plugins.convention.feature.data)
}

dependencies {
    api(projects.service.core.data)
    api(projects.service.network)
    // Who is signed in, and the tokens, are the session module's — this layer keeps neither.
    api(projects.service.session)
    api(projects.feature.auth.domain)
    testImplementation(testFixtures(projects.service.session))
}
