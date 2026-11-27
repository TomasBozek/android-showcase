plugins {
    alias(libs.plugins.convention.service)
}

dependencies {
    api(projects.service.core.domain)
    // Every repository and client logs; the port is `:service:log:domain`'s.
    api(projects.service.log.domain)
}
