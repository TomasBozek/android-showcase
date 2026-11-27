plugins {
    alias(libs.plugins.convention.feature.presentation)
}

dependencies {
    api(projects.core.ui)
    // Its route keys and deep links.
    api(projects.feature.inventory.api)
    api(projects.feature.inventory.domain)
    // Reading a barcode into the editor's field; `api`, because the view model names it.
    api(projects.service.scanner)

    // FakeInventoryRepository, for the ViewModel tests.
    testImplementation(testFixtures(projects.feature.inventory.domain))
    testImplementation(testFixtures(projects.service.scanner))
    // FakeFormDrafts, for the editor's drafts across launches.
    testImplementation(testFixtures(projects.service.core.domain))
}
