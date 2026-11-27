plugins {
    alias(libs.plugins.convention.feature.presentation)
    // `collectAsLazyPagingItems`.
    alias(libs.plugins.convention.paging)
}

dependencies {
    api(projects.core.ui)
    api(projects.feature.movies.api)
    api(projects.feature.movies.domain)
}
