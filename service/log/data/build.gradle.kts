plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    api(projects.service.log.domain)
}
