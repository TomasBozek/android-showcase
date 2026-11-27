plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    // `FileStore.uriFor`: a file leaves the app as a content:// URI with a read grant.
    api(projects.service.files)

    testImplementation(testFixtures(projects.service.files))
}
