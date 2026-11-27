plugins {
    alias(libs.plugins.convention.kotlin.jvm)
    // FakeProfileRepository: Home greets by the profile's name, so more than this feature tests against it.
    `java-test-fixtures`
}

dependencies {
    api(projects.service.core.domain)
}
