package com.example.androidproject1.feature.catalog.presentation.categories

import com.example.androidproject1.feature.catalog.domain.CatalogRepository
import com.example.androidproject1.feature.catalog.domain.FavouritesRepository
import com.example.androidproject1.feature.catalog.domain.Product
import com.example.androidproject1.feature.catalog.presentation.R
import com.example.androidproject1.service.core.domain.error.DomainError
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.core.ui.viewmodel.ErrorDisplay
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.update

class CategoriesViewModel(
    logger: Logger,
    private val catalogRepository: CatalogRepository,
    private val favouritesRepository: FavouritesRepository,
) : BaseViewModel<CategoriesState, CategoriesEvent, CategoriesNavigation>(
    // Nothing to show until the catalog has loaded.
    initialState = null,
    logger = logger.withTag("CategoriesViewModel"),
) {

    companion object {

        /** Its own id, so the undo action is told apart from any other snackbar this screen shows. */
        const val SNACKBAR_UNDO_REMOVE = "categories_undo_remove_favourite"
    }

    /**
     * The latest favourites, held apart from the state because they can arrive before the
     * categories do, while there is no state to put them in.
     */
    private var favourites: List<Product> = emptyList()

    /**
     * The product the last remove took away — not something the screen draws, and the snackbar's
     * action comes back as plain data, so the view model remembers what its id referred to.
     */
    private var lastRemoved: String? = null

    init {
        loadCategories()
        observeFavourites()
    }

    override fun onUiEvent(event: CategoriesEvent) {
        when (event) {
            CategoriesEvent.SearchClicked -> navigate(CategoriesNavigation.Search)

            is CategoriesEvent.CategoryClicked -> navigate(
                CategoriesNavigation.Products(categoryId = event.category.id, categoryName = event.category.name),
            )

            CategoriesEvent.RefreshRequested -> refresh()

            is CategoriesEvent.FavouriteRemoved -> removeFavourite(event.productId)
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.SnackbarAction && event.id == SNACKBAR_UNDO_REMOVE) {
            lastRemoved?.let { setFavourite(it, favourite = true) }
            lastRemoved = null
            return
        }
        super.onSystemEvent(event)
    }

    /**
     * Cache first, then the network — so a cold start with no connection still shows what was
     * browsed last time, and `observe` keeps the screen current if the cache changes underneath.
     *
     * Inline rather than Alert: a dialog over the loading screen would leave nothing behind it.
     * The refresh failure that follows a stale hit is claimed by [keepStaleContent] instead, so a
     * usable list is not replaced by an error page.
     */
    private fun loadCategories() = observe(
        flow = { catalogRepository.observeCategories() },
        loading = overlay(),
        errorDisplay = ErrorDisplay.Inline,
        onError = ::keepStaleContent,
        onData = { categories ->
            uiState.update { state ->
                // Keep the flags: the flow emits again after every refresh, successful or not.
                val previous = state.data
                state.copy(
                    data = CategoriesState(
                        categories = categories,
                        favourites = favourites,
                        refreshing = previous?.refreshing ?: false,
                        stale = previous?.stale ?: false,
                    ),
                )
            }
        },
    )

    /**
     * Fetch again and let the observed cache carry the result. Silent on failure because the
     * banner is the report: a pull on a dead network sets `stale`, and a pull that gets through
     * clears it.
     */
    private fun refresh() {
        if (uiState.value.data?.refreshing == true) return
        execute(
            loading = { refreshing -> updateData { copy(refreshing = refreshing) } },
            errorDisplay = ErrorDisplay.Silent,
            onError = {
                updateData { copy(stale = true) }
                true
            },
            action = { catalogRepository.refreshCategories() },
            onData = { updateData { copy(stale = false) } },
        )
    }

    /**
     * Keeps a stale list on screen when the refresh behind it fails.
     *
     * `cached` emits the cache and then the failure, and `ErrorDisplay.Inline` renders a
     * `ContentState` *instead of* the content — so without this a screen that had something usable
     * to show would be replaced by "try again". Returning true claims the error; the banner
     * `stale` raises is what stops the staleness being silent, and it stays up until a refresh
     * gets through.
     */
    private fun keepStaleContent(error: DomainError): Boolean {
        val hasContent = uiState.value.data != null
        if (hasContent) updateData { copy(stale = true) }
        return hasContent
    }

    // Silent: a favourites list that cannot be read leaves the section out, and nothing about it is
    // worth an error over the categories.
    private fun observeFavourites() = observe(
        flow = { favouritesRepository.observeFavourites() },
        errorDisplay = ErrorDisplay.Silent,
        onData = { products ->
            favourites = products
            uiState.update { state -> state.copy(data = state.data?.copy(favourites = products)) }
        },
    )

    private fun removeFavourite(productId: String) {
        lastRemoved = productId
        setFavourite(productId, favourite = false)
        showSnackbar(
            message = R.string.categories_favourite_removed.toUiText(),
            actionLabel = R.string.categories_favourite_undo.toUiText(),
            id = SNACKBAR_UNDO_REMOVE,
        )
    }

    // No onData work: the observed flow is what updates the list, so writing it here too would be a
    // second source of truth for the same row.
    private fun setFavourite(productId: String, favourite: Boolean) = execute(
        action = { favouritesRepository.setFavourite(productId, favourite) },
        onData = { },
    )
}
