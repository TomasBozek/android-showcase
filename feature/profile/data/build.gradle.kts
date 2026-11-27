plugins {
    alias(libs.plugins.convention.feature.data)
    // Ships FakeAvatarDataSource: no Context, no file I/O, and a failure is a settable field.
    alias(libs.plugins.convention.android.library.testfixtures)
}

dependencies {
    api(projects.service.core.data)
    api(projects.feature.profile.domain)
    // The avatar's upload.
    implementation(projects.service.files)

    testImplementation(testFixtures(projects.service.files))
}
