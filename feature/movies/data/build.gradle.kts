plugins {
    alias(libs.plugins.convention.feature.data)
    alias(libs.plugins.convention.android.room)
    // The pager, its mediator and Room's `PagingSource`.
    alias(libs.plugins.convention.paging)
}

dependencies {
    api(projects.service.core.data)
    api(projects.feature.movies.domain)
    api(projects.service.network)
}
