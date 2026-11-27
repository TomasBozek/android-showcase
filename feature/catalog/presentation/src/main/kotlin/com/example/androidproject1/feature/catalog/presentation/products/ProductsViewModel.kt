package com.example.androidproject1.feature.catalog.presentation.products

import com.example.androidproject1.feature.catalog.api.ProductsDestination
import com.example.androidproject1.feature.catalog.domain.CatalogRepository
import com.example.androidproject1.feature.catalog.presentation.R
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.state.ContentState
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

class ProductsViewModel(
    logger: Logger,
    // The route key, handed in by the destination. Available in `init`, and it comes back with
    // the back stack entry after process death.
    private val args: ProductsDestination,
    private val catalogRepository: CatalogRepository,
) : BaseViewModel<ProductsState, ProductsEvent, ProductsNavigation>(
    // Nothing to show until the category named by the route has loaded its products.
    initialState = null,
    logger = logger.withTag("ProductsViewModel"),
) {

    /** The title. A category link names the id alone, so its name comes from the cache. */
    private var categoryName: String = args.categoryName.orEmpty()

    init {
        if (args.categoryName == null) nameCategory()
        load()
    }

    override fun onUiEvent(event: ProductsEvent) {
        when (event) {
            is ProductsEvent.ProductClicked -> navigate(ProductsNavigation.ProductDetail(event.product.id))
            ProductsEvent.NavigateUpClicked -> navigate(ProductsNavigation.NavigateUp)
            ProductsEvent.RefreshRequested -> refresh()
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        super.onSystemEvent(event)
        // Retry on the empty message: the default cleared it, so the pull indicator has a list
        // (an empty one) to draw over while the category is fetched again.
        if (event is SystemEvent.ContentAction && event.id == CONTENT_EMPTY) refresh()
    }

    // Cache then network, so a category browsed before opens instantly and offline. Inline
    // rather than Alert: `cached` emits a refresh failure after the stale list, so the error
    // lands beside a usable screen instead of replacing it.
    private fun load() = observe(
        flow = { catalogRepository.observeProducts(args.categoryId) },
        loading = overlay(),
        errorDisplay = ErrorDisplay.Inline,
        onError = ::keepStaleContent,
        onData = { products ->
            // The data is set first, empty list or not: with data on screen the chrome's message
            // draws inside the scaffold, under the title and the up arrow, rather than over the
            // whole window. Cleared when the refresh that follows fills the list in.
            uiState.update { state ->
                state.copy(
                    data = ProductsState(
                        categoryName = categoryName,
                        products = products,
                        refreshing = state.data?.refreshing ?: false,
                    ),
                )
            }
            if (products.isEmpty()) {
                showContent(
                    ContentState.Empty(
                        id = CONTENT_EMPTY,
                        message = R.string.products_empty.toUiText(),
                        actionLabel = R.string.products_retry.toUiText(),
                    ),
                )
            } else {
                clearContent()
            }
        },
    )

    /** The category's own name, for a route that came without one; an unknown id keeps a blank title. */
    private fun nameCategory() = execute(
        errorDisplay = ErrorDisplay.Silent,
        action = { catalogRepository.observeCategories().first() },
        onData = { categories ->
            categories.firstOrNull { it.id == args.categoryId }?.let { category ->
                categoryName = category.name
                updateData { copy(categoryName = category.name) }
            }
        },
    )

    /** Fetch the category again and let the observed cache carry the result; a failure says so once. */
    private fun refresh() {
        if (uiState.value.data?.refreshing == true) return
        execute(
            loading = { refreshing -> updateData { copy(refreshing = refreshing) } },
            errorDisplay = ErrorDisplay.Silent,
            onError = {
                showSnackbar(R.string.catalog_stale.toUiText())
                true
            },
            action = { catalogRepository.refreshProducts(args.categoryId) },
            onData = {},
        )
    }

    /** See `CategoriesViewModel.keepStaleContent`: a usable list beats an error page. */
    private fun keepStaleContent(error: DomainError): Boolean {
        val hasContent = uiState.value.data != null
        if (hasContent) showSnackbar(R.string.catalog_stale.toUiText())
        return hasContent
    }

    private companion object {

        /** The empty category's message, so its Retry can be told from an error's. */
        const val CONTENT_EMPTY = "empty"
    }
}
