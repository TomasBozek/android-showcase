plugins {
    alias(libs.plugins.convention.kotlin.jvm)
    // `Flow<PagingData<Movie>>`, which is plain Kotlin.
    alias(libs.plugins.convention.paging)
}

dependencies {
    api(projects.service.core.domain)
}
