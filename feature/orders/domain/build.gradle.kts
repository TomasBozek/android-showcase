plugins {
    alias(libs.plugins.convention.kotlin.jvm)
    // FakeOrdersRepository: checkout records through the repository, so more than this feature fakes it.
    `java-test-fixtures`
}

dependencies {
    api(projects.service.core.domain)
}
